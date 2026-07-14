-- Script SQL URGENT pour corriger l'erreur de contrainte NOT NULL
-- À exécuter IMMÉDIATEMENT dans pgAdmin 4 sur la base pharmacy_guarde_db

-- Étape 1: Rendre les colonnes start_date et end_date nullable
-- C'est LA SOLUTION IMMÉDIATE pour résoudre l'erreur
ALTER TABLE duties ALTER COLUMN start_date DROP NOT NULL;
ALTER TABLE duties ALTER COLUMN end_date DROP NOT NULL;

-- Étape 2: Ajouter la colonne duty_period_id si elle n'existe pas
ALTER TABLE duties ADD COLUMN IF NOT EXISTS duty_period_id BIGINT;

-- Étape 3: Créer la table duty_periods si elle n'existe pas
CREATE TABLE IF NOT EXISTS duty_periods (
    id BIGSERIAL PRIMARY KEY,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT
);

-- Étape 4: Ajouter la clé étrangère pour duty_period_id
-- Supprimer d'abord la contrainte si elle existe déjà
ALTER TABLE duties DROP CONSTRAINT IF EXISTS fk_duty_period;

-- Ajouter la nouvelle contrainte
ALTER TABLE duties 
ADD CONSTRAINT fk_duty_period 
FOREIGN KEY (duty_period_id) 
REFERENCES duty_periods(id);

-- Vérification: Vérifier que les contraintes ont été supprimées
SELECT 
    column_name, 
    is_nullable, 
    data_type 
FROM information_schema.columns 
WHERE table_name = 'duties' 
    AND column_name IN ('start_date', 'end_date', 'duty_period_id');

