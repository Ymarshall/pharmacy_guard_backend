# 06 — Glossaire métier & technique

| Terme | Définition |
|-------|------------|
| **Pharmacie de garde** | Pharmacie désignée pour assurer un service hors horaires habituels sur une période réglementaire. |
| **DutyPeriod** | Fenêtre temporelle (`startDate`–`endDate`) regroupant les gardes publiées ; une seule peut être `isCurrent`. |
| **Période courante** | `DutyPeriod` avec `isCurrent = true`, consommée par l’app mobile. |
| **Duty** | Affectation d’une pharmacie à une période de garde, avec statut opérationnel. |
| **DutyFile** | Fichier source (PDF, image, Excel) servant à extraire les gardes d’une période. |
| **TO_VERIFY** | Statut Duty indiquant qu’une vérification humaine est requise (ex. après signalement). |
| **Signalement** | Action client indiquant qu’une pharmacie déclarée de garde ne l’est pas réellement. |
| **Client** | Utilisateur de l’application mobile, identifié principalement par numéro de téléphone. |
| **User** | Compte administrateur / opérateur du back-office. |
| **Zone** | Entité géographique de référence (ville, commune, département, région) en Côte d’Ivoire. |
| **Assurance** | Organisme assureur accepté par une pharmacie (relation M:N). |
| **Avis / Review** | Note (1–5) et commentaire laissés par un Client sur une Pharmacie. |
| **Géocodage** | Conversion d’une adresse en coordonnées GPS via Google Maps. |
| **Haversine** | Formule de calcul de distance angulaire entre deux points GPS. |
| **OCR** | Reconnaissance optique de caractères (Google Cloud Vision) sur images / PDF scannés. |
| **BO / Back-office** | Interface d’administration Angular (`pharmacy_guard`). |
| **Monolithe modulaire** | Application unique découpée en packages métier sans microservices. |
| **OpenAPI / Swagger** | Documentation du contrat REST exposée par SpringDoc. |
