# 04 — Spécifications API

Base URL (dev) : `http://localhost:8080`  
Documentation interactive : `/swagger-ui.html`  
Contrat OpenAPI généré : `/api-docs`

Convention : JSON UTF-8. Authentification applicative **non enforceée** côté Security (voir risque dans ADR / tech specs).

## Auth

| Méthode | Path | Body | Réponse |
|---------|------|------|---------|
| POST | `/api/auth/login` | `{ email, password }` | `User` (sans JWT) |
| POST | `/api/auth/client/login` | `{ phone }` | `Client` (+ évent. contextes métier) |

## Pharmacies (BO)

| Méthode | Path | Description |
|---------|------|-------------|
| GET | `/api/pharmacies` | Liste (+ filtres `city`, `commune`, `statut`) |
| GET | `/api/pharmacies/{id}` | Détail |
| POST | `/api/pharmacies` | Création |
| PUT | `/api/pharmacies/{id}` | Mise à jour |
| DELETE | `/api/pharmacies/{id}` | Suppression |
| POST | `/api/pharmacies/update-geocoding` | Géocodage batch |
| POST | `/api/pharmacies/{id}/update-geocoding` | Géocodage unitaire |

## Client mobile — pharmacies

| Méthode | Path | Description |
|---------|------|-------------|
| GET | `/api/client/pharmacies/nearest` | Plus proches |
| GET | `/api/client/pharmacies/nearest-duty` | De garde (période courante) |
| GET | `/api/client/pharmacies/search` | Recherche |
| POST | `/api/client/pharmacies/duty/report` | Signalement garde |

## Avis

| Méthode | Path | Description |
|---------|------|-------------|
| GET/POST | `/api/client/pharmacies/{id}/reviews` | Liste / création |
| GET | `/api/client/pharmacies/{id}/rating-summary` | Synthèse notes |
| PUT/DELETE | `/api/client/pharmacies/{id}/reviews/{reviewId}` | MAJ / suppression |

## Périodes & gardes

| Méthode | Path | Description |
|---------|------|-------------|
| CRUD | `/api/duty-periods` | Gestion périodes |
| GET | `/api/duty-periods/current` | Période courante |
| PUT | `/api/duty-periods/{id}/activate` | Activation `isCurrent` |
| CRUD | `/api/duties` | Gardes |
| GET | `/api/duties` (filtres) | Par pharmacie / période / dates |
| PUT | `/api/duties/{id}/enable` / `disable` | Bascules statut |

## Fichiers de garde

| Méthode | Path | Description |
|---------|------|-------------|
| POST | `/api/duty-files/upload` | PDF/JPEG |
| POST | `/api/duty-files/upload-excel` | Excel |
| POST | `/api/duty-files/{id}/process` | Traitement |
| POST | `/api/duty-files/{id}/extract-text` | Extraction texte |
| GET/DELETE | `/api/duty-files` … | Liste / suppression |

## Référentiels & admin

| Domaine | Base path |
|---------|-----------|
| Users | `/api/users` |
| Clients | `/api/clients` |
| Assurances | `/api/assurances` |
| Zones | `/api/zones` |
| Urgences | `/api/emergency-numbers` |
| Dashboard | `/api/dashboard/stats` |
| Reports | `/api/reports/chart/{pie\|line\|bar\|area}` |
| Info | `/api` |

## Schémas JSON illustratifs

### DutyPeriod

```json
{
  "id": 1,
  "startDate": "2026-07-14",
  "endDate": "2026-07-21",
  "isCurrent": true
}
```

### Duty

```json
{
  "id": 10,
  "pharmacyId": 3,
  "dutyPeriodId": 1,
  "status": "ACTIVE",
  "dutyFileId": 5
}
```

### Client login

```json
{ "phone": "+2250700000000" }
```

> Pour le contrat exhaustif des schémas et codes d’erreur, se référer à l’export SpringDoc `/api-docs` (source de vérité runtime).
