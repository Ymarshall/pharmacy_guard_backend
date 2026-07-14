# 01 — Exigences métier (Business Requirements)

## Vision

Fournir le **socle serveur** de la plateforme Pharmacy Guard : permettre au grand public ivoirien de localiser une pharmacie de garde fiable, tout en offrant aux opérateurs un back-office pour publier, vérifier et maintenir les listes de gardes.

## Problématique

Les listes de pharmacies de garde sont diffusées sous formes hétérogènes (PDF, scans, tableurs). Sans système central :

- les citoyens peinent à trouver une pharmacie ouverte hors horaires ;
- les opérateurs re-saisissent manuellement des données errorables ;
- il n’existe pas de boucle de feedback (signalements, avis) pour fiabiliser les listes.

## Objectifs

| ID | Objectif | Critère de succès |
|----|----------|-------------------|
| O1 | Centraliser le référentiel pharmacies, zones, assurances, urgences | CRUD API stable + persistance PostgreSQL |
| O2 | Publier une période de garde courante consommable par mobile et BO | `DutyPeriod.isCurrent` + endpoints client |
| O3 | Accélérer l’ingestion des listes officielles | Upload PDF/Excel/images + extraction (PDFBox / Vision / POI) |
| O4 | Permettre la vérification et la correction opérationnelle | Statuts Duty, signalements client, disable |
| O5 | Donner de la visibilité opérationnelle | Dashboard stats + rapports graphiques |
| O6 | Servir l’app mobile (clients, proximité, avis) | API `/api/client/**` et `/api/auth/client/**` |

## Périmètre

**Dans le périmètre**

- API REST Spring Boot pour back-office Angular et app Flutter.
- Domaines : pharmacies, gardes, périodes, fichiers, clients, users, zones, urgences, avis, dashboard, reports.
- Géocodage Google Maps et calcul de distance (Haversine).

**Hors périmètre (état actuel)**

- Authentification JWT / OAuth opérationnelle sur toutes les routes.
- Traitement asynchrone / file d’attente pour OCR.
- Messagerie temps réel, push FCM.
- Multi-tenant multi-pays.

## Parties prenantes

| Acteur | Besoin |
|--------|--------|
| Opérateur BO | Publier et corriger les gardes |
| Citoyen (app) | Trouver vite une pharmacie de garde proche |
| Administrateur | Gérer utilisateurs, zones, référentiels |
| Équipe technique | API documentée (OpenAPI), évolutive |

## Contraintes

- Stack imposée : Java 17, Spring Boot 3.x, PostgreSQL.
- Latency acceptable pour recherche proximité et lecture période courante.
- Données géographiques centrées Côte d’Ivoire (Africa/Abidjan).
