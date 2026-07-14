-- Script SQL pour créer la table pharmacy_reviews
-- Compatible PostgreSQL

CREATE TABLE IF NOT EXISTS pharmacy_reviews (
    id BIGSERIAL PRIMARY KEY,
    pharmacy_id BIGINT NOT NULL REFERENCES pharmacies(id) ON DELETE CASCADE,
    client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT unique_pharmacy_client_review UNIQUE (pharmacy_id, client_id)
);

-- Index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_pharmacy_reviews_pharmacy_id ON pharmacy_reviews(pharmacy_id);
CREATE INDEX IF NOT EXISTS idx_pharmacy_reviews_client_id ON pharmacy_reviews(client_id);
CREATE INDEX IF NOT EXISTS idx_pharmacy_reviews_created_at ON pharmacy_reviews(created_at DESC);

-- Commentaires sur la table
COMMENT ON TABLE pharmacy_reviews IS 'Table des commentaires et notes des clients sur les pharmacies';
COMMENT ON COLUMN pharmacy_reviews.rating IS 'Note de 1 à 5 étoiles';
COMMENT ON COLUMN pharmacy_reviews.comment IS 'Commentaire texte optionnel (max 1000 caractères)';
COMMENT ON CONSTRAINT unique_pharmacy_client_review ON pharmacy_reviews IS 'Un client ne peut commenter qu''une seule fois par pharmacie';
