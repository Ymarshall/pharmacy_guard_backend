-- Script de migration pour mettre à jour la table duties
-- À exécuter manuellement dans votre base de données PostgreSQL

-- Étape 1: Ajouter la colonne duty_period_id si elle n'existe pas
ALTER TABLE duties
ADD COLUMN IF NOT EXISTS duty_period_id BIGINT;

-- Étape 2: Créer la table duty_periods si elle n'existe pas
CREATE TABLE IF NOT EXISTS duty_periods (
    id BIGSERIAL PRIMARY KEY,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT
);

-- Étape 3: Ajouter la clé étrangère pour duty_period_id (si la colonne existe)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'duties' AND column_name = 'duty_period_id') THEN
        -- Supprimer la contrainte si elle existe déjà
        ALTER TABLE duties DROP CONSTRAINT IF EXISTS fk_duty_period;
        
        -- Ajouter la clé étrangère
        ALTER TABLE duties
        ADD CONSTRAINT fk_duty_period
        FOREIGN KEY (duty_period_id)
        REFERENCES duty_periods(id);
    END IF;
END $$;

-- Étape 4: Rendre les colonnes start_date et end_date nullable (si elles existent encore)
-- Cela permettra de migrer progressivement sans casser les contraintes
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'duties' AND column_name = 'start_date') THEN
        -- Supprimer la contrainte NOT NULL
        ALTER TABLE duties ALTER COLUMN start_date DROP NOT NULL;
        -- Optionnel: Définir une valeur par défaut NULL pour les nouvelles insertions
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'duties' AND column_name = 'end_date') THEN
        -- Supprimer la contrainte NOT NULL
        ALTER TABLE duties ALTER COLUMN end_date DROP NOT NULL;
        -- Optionnel: Définir une valeur par défaut NULL pour les nouvelles insertions
    END IF;
END $$;

-- Étape 4b: S'assurer que duty_period_id peut être NULL temporairement pendant la migration
-- (Ensuite, vous pouvez rendre cette colonne NOT NULL une fois que toutes les données sont migrées)

-- Étape 5: Supprimer les colonnes start_date et end_date (UNIQUEMENT si vous êtes sûr qu'elles ne sont plus utilisées)
-- DÉCOMMENTEZ LES LIGNES CI-DESSOUS APRÈS AVOIR VÉRIFIÉ QUE TOUTES LES DONNÉES ONT ÉTÉ MIGRÉES
-- ALTER TABLE duties DROP COLUMN IF EXISTS start_date;
-- ALTER TABLE duties DROP COLUMN IF EXISTS end_date;

