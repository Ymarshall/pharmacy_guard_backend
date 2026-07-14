-- Script SQL pour ajouter la colonne status à la table duties
-- Compatible MySQL/MariaDB

-- Étape 1 : Ajouter la colonne status avec une valeur par défaut
ALTER TABLE duties 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Étape 2 : Mettre à jour les enregistrements existants pour s'assurer qu'ils ont tous 'ACTIVE'
-- (Cette étape est optionnelle si la colonne a déjà une valeur par défaut, mais c'est une bonne pratique)
UPDATE duties 
SET status = 'ACTIVE' 
WHERE status IS NULL OR status = '';

-- Étape 3 : Ajouter une contrainte CHECK pour s'assurer que seules les valeurs valides sont acceptées
-- (Optionnel mais recommandé pour la cohérence des données)
ALTER TABLE duties 
ADD CONSTRAINT chk_duty_status 
CHECK (status IN ('ACTIVE', 'TO_VERIFY', 'DISABLED'));

-- Vérification : Afficher la structure de la table pour confirmer
-- DESCRIBE duties;
