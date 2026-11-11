-- Migration additive et idempotente.
-- Reprend les scripts SQL historiques de la racine (clients.is_active, duties.status,
-- pharmacy_reviews, duty_periods) sans recréer une base déjà gérée par Hibernate.
-- baseline-on-migrate = 0 : une base existante reçoit cette version, elle n'est pas reconstruite.
-- Les CREATE/ALTER sont ignorés tant que la table n'existe pas (Flyway passe avant Hibernate
-- sur une base neuve). SchemaHardeningRunner repose ensuite les index une fois les tables créées.

DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS pg_trgm;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Extension pg_trgm non installée: %', SQLERRM;
END $$;

DO $$
BEGIN
    IF to_regclass('public.clients') IS NOT NULL THEN
        EXECUTE 'ALTER TABLE clients ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE';
        EXECUTE 'UPDATE clients SET is_active = TRUE WHERE is_active IS NULL';
    END IF;

    IF to_regclass('public.users') IS NOT NULL THEN
        EXECUTE 'ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT ''ADMIN''';
        EXECUTE 'UPDATE users SET role = ''ADMIN'' WHERE role IS NULL';
    END IF;

    IF to_regclass('public.duties') IS NOT NULL THEN
        EXECUTE 'ALTER TABLE duties ADD COLUMN IF NOT EXISTS duty_period_id BIGINT';
        EXECUTE 'ALTER TABLE duties ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT ''ACTIVE''';
        EXECUTE 'UPDATE duties SET status = ''ACTIVE'' WHERE status IS NULL OR status = ''''';
    END IF;

    IF to_regclass('public.duty_files') IS NOT NULL THEN
        EXECUTE 'ALTER TABLE duty_files ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64)';
    END IF;

    IF to_regclass('public.duty_periods') IS NULL THEN
        EXECUTE 'CREATE TABLE duty_periods (
            id BIGSERIAL PRIMARY KEY,
            start_date DATE NOT NULL,
            end_date DATE NOT NULL,
            is_current BOOLEAN NOT NULL DEFAULT FALSE,
            description TEXT
        )';
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.pharmacies') IS NOT NULL
       AND to_regclass('public.clients') IS NOT NULL
       AND to_regclass('public.pharmacy_reviews') IS NULL THEN
        EXECUTE 'CREATE TABLE pharmacy_reviews (
            id BIGSERIAL PRIMARY KEY,
            pharmacy_id BIGINT NOT NULL REFERENCES pharmacies(id) ON DELETE CASCADE,
            client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
            rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
            comment TEXT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP,
            CONSTRAINT unique_pharmacy_client_review UNIQUE (pharmacy_id, client_id)
        )';
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.pharmacies') IS NOT NULL THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_pharmacies_name ON pharmacies (name)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_pharmacies_city ON pharmacies (city)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_pharmacies_commune ON pharmacies (commune)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_pharmacies_statut ON pharmacies (statut)';
        IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
            EXECUTE 'CREATE INDEX IF NOT EXISTS idx_pharmacies_name_trgm ON pharmacies USING gin (name gin_trgm_ops)';
        END IF;
    END IF;

    IF to_regclass('public.duties') IS NOT NULL THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_duties_pharmacy ON duties (pharmacy_id)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_duties_period ON duties (duty_period_id)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_duties_status ON duties (status)';
        EXECUTE 'DELETE FROM duties a USING duties b
                 WHERE a.ctid > b.ctid
                   AND a.pharmacy_id = b.pharmacy_id
                   AND a.duty_period_id = b.duty_period_id
                   AND a.pharmacy_id IS NOT NULL
                   AND a.duty_period_id IS NOT NULL';
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uq_duties_pharmacy_period ON duties (pharmacy_id, duty_period_id)';
    END IF;

    IF to_regclass('public.duty_periods') IS NOT NULL THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_duty_periods_dates ON duty_periods (start_date, end_date)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_duty_periods_current ON duty_periods (is_current)';
    END IF;

    IF to_regclass('public.duty_files') IS NOT NULL THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_duty_files_period ON duty_files (duty_period_id)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_duty_files_hash ON duty_files (content_hash)';
    END IF;

    IF to_regclass('public.pharmacy_reviews') IS NOT NULL THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_pharmacy_reviews_pharmacy_id ON pharmacy_reviews (pharmacy_id)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_pharmacy_reviews_client_id ON pharmacy_reviews (client_id)';
    END IF;
END $$;
