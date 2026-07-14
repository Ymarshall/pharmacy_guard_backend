# 03 — Spécifications techniques

## Architecture

Monolithe **Spring Boot 3.5.9** (Java 17) organisé en **modules métier** sous `com.buddies.pharmaciegarde.modules` :

```
controller → service (+ Impl) → repository (Spring Data JPA) → model / dto
```

Modules : `pharmacy`, `duty`, `user`, `common`, `dashboard`, `report`.

Référence décision structurante : [ADR-001](../docs/adr/ADR-001-modele-duty-period.md).

## Stack

| Couche | Technologie |
|--------|-------------|
| Runtime | Java 17, Gradle 8.14.3 |
| Framework | Spring Boot 3.5.9 (Web, Data JPA, Validation, Security) |
| DB | PostgreSQL (`pharmacy_guarde_db`) |
| ORM | Hibernate (`ddl-auto=update`) |
| Docs API | SpringDoc OpenAPI 2.6.0 |
| PDF | Apache PDFBox 3.0.3 |
| OCR | Google Cloud Vision 3.7.0 |
| Excel | Apache POI 5.2.5 |
| Géo | Google Maps Geocoding API + Haversine maison |

## Modèle de données (synthèse)

| Entité | Rôle |
|--------|------|
| `Pharmacy` | Référentiel établissements + `location` (lat,lng string) |
| `Assurance` | Assureurs ; M:N avec pharmacies |
| `DutyPeriod` | Fenêtre de garde + `isCurrent` |
| `Duty` | Lien Pharmacie ↔ Période + statut |
| `DutyFile` | Fichier source + statut de processing |
| `User` | Compte back-office (email + BCrypt) |
| `Client` | Utilisateur mobile (phone, coords) |
| `Zone` | Référentiel géographique CI |
| `EmergencyNumber` | Annuaire d’urgence |
| `PharmacyReview` | Note / commentaire client |

## Sécurité (état implémenté)

- `SessionCreationPolicy.STATELESS`, CSRF off, CORS vers localhost:4200/3000.
- BCrypt pour mots de passe BO.
- Routes `/api/**` actuellement en `permitAll()` (pas de JWT/OAuth enforce).
- Login client : téléphone seul ; login BO : email/password sans token retourné.

## Traitements critiques

- Ingestion PDF/OCR/Excel **synchrone** dans la requête HTTP.
- Seed / géocodage via `CommandLineRunner` au démarrage.
- Pas de Kafka/Rabbit/Redis ; dossiers `batch/` et `integration/` préparés mais vides.

## Configuration

Fichier principal : `src/main/resources/application.properties`  
Clés sensibles : DB, `google.maps.api.key`, credentials Google Vision.

## Observabilité / qualité

- Swagger UI : `/swagger-ui.html`
- OpenAPI JSON : `/api-docs`
- Tests : couverture minimale (smoke Spring Boot).
- Migrations : scripts SQL manuels (`migration_*.sql`) — pas Flyway/Liquibase.
