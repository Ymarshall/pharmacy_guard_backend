# ADR-001: Modèle de garde centré sur DutyPeriod et Duty

**État** : Accepté

## Contexte

Le métier « pharmacie de garde » en Côte d’Ivoire repose sur des listes officielles publiées pour une **fenêtre temporelle** (souvent hebdomadaire ou pour un créneau réglementaire), pas sur des horaires 24/7 individuels par pharmacie.

Il fallait donc répondre à plusieurs contraintes simultanées :

- Administrer une période active unique consommée par l’app mobile (`nearest-duty`, signalements).
- Importer des listes sources (PDF, images OCR, Excel) et les rattacher à un même cycle de vie.
- Permettre la vérification humaine (statuts `ACTIVE` / `TO_VERIFY` / `DISABLED`) avant et pendant la diffusion.
- Alimenter le dashboard et les rapports BO sans dupliquer la logique de « qui est de garde maintenant ».

Une approche alternative (calendriers libres par pharmacie, ou multi-périodes actives concurrentes) complexifierait immédiatement l’API mobile, l’ingestion documentaire et l’expérience back-office.

## Décision

Nous retenons un **modèle à deux agrégats** :

1. **`DutyPeriod`** — fenêtre métier (`startDate` / `endDate`) avec un flag **`isCurrent`** désignant la période exposée aux clients mobiles.
2. **`Duty`** — affectation d’une `Pharmacy` à une `DutyPeriod`, avec statut opérationnel et lien éventuel vers un `DutyFile` source.

Le flux nominal est : créer une période → importer / traiter les fichiers → produire des `Duty` → vérifier → **activer** la période (`isCurrent`) → l’API client lit exclusivement la période courante.

Cette décision est matérialisée dans le monolithe Spring Boot (`modules/duty`), les endpoints `/api/duty-periods` et `/api/duties`, et les endpoints mobiles `/api/client/pharmacies/nearest-duty` et `/api/client/pharmacies/duty/report`.

## Conséquences

### Avantages

- **Cohérence métier** : une seule source de vérité pour « les gardes actuelles ».
- **Alignement ingestion ↔ diffusion** : PDF/Excel/OCR écrivent des `Duty` sur une période cible, pas des règles éparses.
- **Opérabilité BO** : activation, désactivation unitaire, signalements `TO_VERIFY` restent gérables dans un périmètre borné.
- **Évolutivité documentée** : historique par périodes successives sans mélanger les cycles.

### Points d’attention / risques

- **Invariant d’unicité** : une seule période `isCurrent` doit être garantie côté service (concurrence, double activation).
- **Couplage mobile** : toute évolution multi-zones / multi-périodes actives nécessite un contrat API versionné.
- **Traitement synchrone des fichiers** : le pipeline documentaire s’exécute encore dans le thread HTTP ; sous charge, l’activation d’une période peut être retardée (candidat ADR ultérieur : async / file d’attente).
- **Migrations schéma** : le cycle de vie DutyPeriod/Duty est critique ; `ddl-auto=update` ne remplace pas une stratégie de migration versionnée (Flyway/Liquibase) en production.
