-- Script SQL pour ajouter la colonne category à la table emergency_numbers
-- À exécuter dans pgAdmin ou via psql

ALTER TABLE emergency_numbers 
ADD COLUMN IF NOT EXISTS category VARCHAR(50) NOT NULL DEFAULT 'other';

-- Mettre à jour les valeurs existantes si nécessaire
-- UPDATE emergency_numbers SET category = 'other' WHERE category IS NULL;

-- Optionnel : Supprimer la valeur par défaut après avoir mis à jour les données existantes
-- ALTER TABLE emergency_numbers ALTER COLUMN category DROP DEFAULT;

