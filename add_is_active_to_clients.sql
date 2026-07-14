-- Script SQL pour ajouter la colonne is_active à la table clients
-- À exécuter dans pgAdmin 4 sur la base pharmacy_guarde_db

-- Ajouter la colonne is_active si elle n'existe pas
ALTER TABLE clients
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;

-- Mettre à jour tous les clients existants pour qu'ils soient actifs par défaut
UPDATE clients
SET is_active = true
WHERE is_active IS NULL;

-- Vérification : Vérifier que la colonne a été ajoutée
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default
FROM information_schema.columns 
WHERE table_name = 'clients' 
    AND column_name = 'is_active';

