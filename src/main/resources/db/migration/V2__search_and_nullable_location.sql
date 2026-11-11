-- Recherche accent-insensible via unaccent + index trigram, et localisation nullable.

DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS unaccent;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'unaccent ignoré: %', SQLERRM;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'unaccent') THEN
        CREATE OR REPLACE FUNCTION immutable_unaccent(input text)
        RETURNS text
        LANGUAGE sql
        IMMUTABLE
        PARALLEL SAFE
        AS $body$ SELECT public.unaccent('public.unaccent', input) $body$;
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'immutable_unaccent ignoré: %', SQLERRM;
END $$;

DO $$
BEGIN
    IF to_regclass('public.pharmacies') IS NOT NULL THEN
        EXECUTE 'ALTER TABLE pharmacies ALTER COLUMN location DROP NOT NULL';
        IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'immutable_unaccent')
           AND EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
            EXECUTE 'CREATE INDEX IF NOT EXISTS idx_pharmacies_name_search_trgm ON pharmacies USING gin (immutable_unaccent(lower(name)) gin_trgm_ops)';
        END IF;
    END IF;
END $$;
