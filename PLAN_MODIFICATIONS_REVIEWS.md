# Plan des modifications pour les commentaires et notations

## Vue d'ensemble
Ajout d'un système de commentaires et de notation (étoiles sur 5) pour les pharmacies, accessible via l'app mobile.

---

## 1. NOUVELLE ENTITÉ : PharmacyReview

### Fichier : `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/model/PharmacyReview.java`

**Structure :**
- `id` (Long) - Identifiant unique
- `pharmacy` (Pharmacy) - Relation ManyToOne vers Pharmacy
- `client` (Client) - Relation ManyToOne vers Client (utilisateur de l'app)
- `rating` (Integer) - Note de 1 à 5 étoiles
- `comment` (String) - Commentaire texte (optionnel)
- `createdAt` (LocalDateTime) - Date de création
- `updatedAt` (LocalDateTime) - Date de modification

**Contraintes :**
- Un client ne peut commenter qu'une seule fois par pharmacie (contrainte unique sur pharmacy_id + client_id)
- Rating entre 1 et 5
- Commentaire optionnel mais limité à 1000 caractères

---

## 2. NOUVEAU REPOSITORY : PharmacyReviewRepository

### Fichier : `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/repository/PharmacyReviewRepository.java`

**Méthodes à ajouter :**
- `List<PharmacyReview> findByPharmacyId(Long pharmacyId)` - Tous les commentaires d'une pharmacie
- `Optional<PharmacyReview> findByPharmacyIdAndClientId(Long pharmacyId, Long clientId)` - Vérifier si un client a déjà commenté
- `Double findAverageRatingByPharmacyId(Long pharmacyId)` - Note moyenne d'une pharmacie
- `Long countByPharmacyId(Long pharmacyId)` - Nombre total de commentaires
- `List<PharmacyReview> findByPharmacyIdOrderByCreatedAtDesc(Long pharmacyId, Pageable pageable)` - Commentaires récents avec pagination

---

## 3. NOUVEAUX DTOs

### 3.1 PharmacyReviewCreateDto
**Fichier :** `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/dto/PharmacyReviewCreateDto.java`

**Champs :**
- `pharmacyId` (Long) - ID de la pharmacie (obligatoire)
- `clientId` (Long) - ID du client (obligatoire)
- `rating` (Integer) - Note de 1 à 5 (obligatoire, validation @Min(1) @Max(5))
- `comment` (String) - Commentaire (optionnel, max 1000 caractères)

### 3.2 PharmacyReviewResponseDto
**Fichier :** `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/dto/PharmacyReviewResponseDto.java`

**Champs :**
- `id` (Long)
- `pharmacyId` (Long)
- `pharmacyName` (String)
- `clientId` (Long)
- `clientName` (String) - Prénom + Nom du client (ou "Anonyme" si pas de nom)
- `rating` (Integer)
- `comment` (String)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)

### 3.3 PharmacyRatingSummaryDto (optionnel, pour les statistiques)
**Fichier :** `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/dto/PharmacyRatingSummaryDto.java`

**Champs :**
- `averageRating` (Double) - Note moyenne
- `totalReviews` (Long) - Nombre total de commentaires
- `ratingDistribution` (Map<Integer, Long>) - Distribution des notes (1 étoile: X, 2 étoiles: Y, etc.)

---

## 4. MODIFICATIONS DES DTOs EXISTANTS

### 4.1 PharmacyResponseDto
**Ajouter les champs suivants :**
- `averageRating` (Double) - Note moyenne (null si aucun commentaire)
- `totalReviews` (Long) - Nombre total de commentaires (0 si aucun)
- `recentReviews` (List<PharmacyReviewResponseDto>) - Liste des 3 derniers commentaires (optionnel)

### 4.2 PharmacyWithDistanceDto
**Hérite de PharmacyResponseDto, donc les champs seront automatiquement disponibles**

---

## 5. NOUVEAU SERVICE : PharmacyReviewService

### Fichier : `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/service/PharmacyReviewService.java`

**Méthodes :**
- `PharmacyReviewResponseDto create(PharmacyReviewCreateDto dto)` - Créer un commentaire/note
- `PharmacyReviewResponseDto update(Long reviewId, PharmacyReviewCreateDto dto)` - Modifier son commentaire
- `void delete(Long reviewId)` - Supprimer son commentaire
- `List<PharmacyReviewResponseDto> getByPharmacyId(Long pharmacyId, int page, int size)` - Liste paginée des commentaires
- `PharmacyRatingSummaryDto getRatingSummary(Long pharmacyId)` - Statistiques de notation

**Règles métier :**
- Un client ne peut créer qu'un seul commentaire par pharmacie
- Un client ne peut modifier/supprimer que ses propres commentaires
- Si un client modifie son commentaire, on met à jour l'existant au lieu d'en créer un nouveau

### Fichier : `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/service/PharmacyReviewServiceImpl.java`

**Implémentation du service avec :**
- Validation des données
- Vérification de l'existence de la pharmacie et du client
- Calcul de la note moyenne
- Gestion des erreurs

---

## 6. MODIFICATIONS DU SERVICE EXISTANT : PharmacyServiceImpl

### Modifications dans `mapToResponseDto` et `mapToPharmacyWithDistanceDto`

**Ajouter le calcul et l'inclusion de :**
- Note moyenne (averageRating)
- Nombre total de commentaires (totalReviews)
- Optionnellement : les 3 derniers commentaires (recentReviews)

**Nouvelle méthode privée :**
```java
private void enrichPharmacyWithRatings(PharmacyResponseDto dto, Pharmacy pharmacy) {
    // Calculer la note moyenne
    // Compter le nombre total de commentaires
    // Optionnellement : récupérer les 3 derniers commentaires
}
```

**Appeler cette méthode dans :**
- `mapToResponseDto`
- `mapToPharmacyWithDistanceDto`

---

## 7. NOUVEAU CONTRÔLEUR : PharmacyReviewController

### Fichier : `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/controller/PharmacyReviewController.java`

**Endpoints :**
- `POST /api/client/pharmacies/{pharmacyId}/reviews` - Créer un commentaire/note
- `PUT /api/client/pharmacies/reviews/{reviewId}` - Modifier son commentaire
- `DELETE /api/client/pharmacies/reviews/{reviewId}` - Supprimer son commentaire
- `GET /api/client/pharmacies/{pharmacyId}/reviews` - Liste des commentaires (paginée)
- `GET /api/client/pharmacies/{pharmacyId}/rating-summary` - Statistiques de notation

**Note :** Tous les endpoints sont dans `/api/client/` car ils sont destinés à l'app mobile.

---

## 8. SCRIPT SQL POUR LA TABLE

### Fichier : `create_pharmacy_reviews_table.sql`

```sql
CREATE TABLE pharmacy_reviews (
    id BIGSERIAL PRIMARY KEY,
    pharmacy_id BIGINT NOT NULL REFERENCES pharmacies(id) ON DELETE CASCADE,
    client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT unique_pharmacy_client_review UNIQUE (pharmacy_id, client_id)
);

CREATE INDEX idx_pharmacy_reviews_pharmacy_id ON pharmacy_reviews(pharmacy_id);
CREATE INDEX idx_pharmacy_reviews_client_id ON pharmacy_reviews(client_id);
CREATE INDEX idx_pharmacy_reviews_created_at ON pharmacy_reviews(created_at DESC);
```

---

## 9. RÉSUMÉ DES FICHIERS À CRÉER/MODIFIER

### Fichiers à CRÉER :
1. `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/model/PharmacyReview.java`
2. `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/repository/PharmacyReviewRepository.java`
3. `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/dto/PharmacyReviewCreateDto.java`
4. `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/dto/PharmacyReviewResponseDto.java`
5. `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/dto/PharmacyRatingSummaryDto.java`
6. `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/service/PharmacyReviewService.java`
7. `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/service/PharmacyReviewServiceImpl.java`
8. `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/controller/PharmacyReviewController.java`
9. `create_pharmacy_reviews_table.sql`

### Fichiers à MODIFIER :
1. `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/dto/PharmacyResponseDto.java` - Ajouter averageRating, totalReviews, recentReviews
2. `src/main/java/com/buddies/pharmaciegarde/modules/pharmacy/service/PharmacyServiceImpl.java` - Enrichir les DTOs avec les notes

---

## 10. FLUX D'UTILISATION

1. **Créer un commentaire :**
   - Client appelle `POST /api/client/pharmacies/{pharmacyId}/reviews`
   - Le système vérifie qu'il n'a pas déjà commenté cette pharmacie
   - Si oui, on met à jour l'existant
   - Si non, on crée un nouveau commentaire

2. **Voir les commentaires :**
   - Les APIs existantes (`getNearestDutyPharmacies`, `searchPharmacies`, etc.) incluront automatiquement la note moyenne et le nombre de commentaires
   - Pour voir tous les commentaires : `GET /api/client/pharmacies/{pharmacyId}/reviews`

3. **Modifier/Supprimer :**
   - Le client peut modifier ou supprimer uniquement ses propres commentaires

---

## 11. CONSIDÉRATIONS TECHNIQUES

- **Performance :** Utiliser des requêtes optimisées avec JOIN pour éviter le N+1 problem
- **Pagination :** Les listes de commentaires seront paginées (par défaut 10 par page)
- **Validation :** Validation stricte des notes (1-5) et longueur des commentaires
- **Sécurité :** Un client ne peut modifier/supprimer que ses propres commentaires

---

## Questions à valider avant implémentation :

1. Un client peut-il modifier son commentaire existant ? (Oui, selon le plan)
2. Faut-il afficher tous les commentaires dans les listes de pharmacies ou seulement la note moyenne ? (Seulement la note moyenne + nombre total, avec endpoint séparé pour les détails)
3. Faut-il limiter le nombre de caractères dans les commentaires ? (Oui, 1000 caractères)
4. Faut-il permettre la suppression de commentaires ? (Oui, uniquement ses propres commentaires)
