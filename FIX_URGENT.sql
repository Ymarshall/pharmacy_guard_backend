-- FIX URGENT - À EXÉCUTER DANS PGADMIN 4
-- Copiez-collez ces lignes dans l'éditeur SQL de pgAdmin et exécutez-les

-- 1. Rendre les colonnes start_date et end_date nullable (SUPPRIME L'ERREUR IMMÉDIATEMENT)
ALTER TABLE duties ALTER COLUMN start_date DROP NOT NULL;
ALTER TABLE duties ALTER COLUMN end_date DROP NOT NULL;

-- 2. Ajouter la colonne duty_period_id si elle n'existe pas
ALTER TABLE duties ADD COLUMN IF NOT EXISTS duty_period_id BIGINT;

-- 3. Créer la table duty_periods si elle n'existe pas
CREATE TABLE IF NOT EXISTS duty_periods (
    id BIGSERIAL PRIMARY KEY,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT
);

-- 4. Ajouter la clé étrangère (si elle n'existe pas déjà)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_duty_period' 
        AND conrelid = 'duties'::regclass
    ) THEN
        ALTER TABLE duties 
        ADD CONSTRAINT fk_duty_period 
        FOREIGN KEY (duty_period_id) 
        REFERENCES duty_periods(id);
    END IF;
END $$;

-- 5. Vérifier que tout est OK (optionnel - pour voir le résultat)
SELECT 
    column_name, 
    is_nullable, 
    data_type 
FROM information_schema.columns 
WHERE table_name = 'duties' 
    AND column_name IN ('start_date', 'end_date', 'duty_period_id')
ORDER BY column_name;

