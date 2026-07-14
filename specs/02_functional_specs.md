# 02 — Spécifications fonctionnelles

## Personas

- **Opérateur** : importe les listes, vérifie les gardes, active une période.
- **Admin** : gère users, zones, pharmacies, référentiels.
- **Client mobile** : s’enregistre par téléphone, consulte gardes proches, note / signale.

## User stories

### Back-office

| ID | En tant que | Je veux | Afin de |
|----|-------------|---------|---------|
| US-BO-01 | Opérateur | Créer une période de garde avec dates | Planifier le prochain cycle |
| US-BO-02 | Opérateur | Uploader PDF/JPEG/Excel de listes | Éviter la saisie manuelle |
| US-BO-03 | Opérateur | Lancer le traitement d’un fichier | Extraire pharmacies / gardes |
| US-BO-04 | Opérateur | Vérifier et activer/désactiver des gardes | Diffuser une liste fiable |
| US-BO-05 | Opérateur | Activer une période (`isCurrent`) | Publier vers le mobile |
| US-BO-06 | Admin | CRUD pharmacies / assurances / zones | Maintenir le référentiel |
| US-BO-07 | Admin | Consulter le dashboard et les rapports | Piloter l’opérationnel |
| US-BO-08 | Admin | Modérer les avis clients | Garantir la qualité du contenu |

### Mobile / client API

| ID | En tant que | Je veux | Afin de |
|----|-------------|---------|---------|
| US-MO-01 | Citoyen | Me connecter avec mon numéro | Accéder rapidement au service |
| US-MO-02 | Citoyen | Voir les pharmacies de garde proches | Me déplacer vers la plus pertinente |
| US-MO-03 | Citoyen | Rechercher / lister des pharmacies | Trouver un établissement connu |
| US-MO-04 | Citoyen | Noter et commenter une pharmacie | Partager mon expérience |
| US-MO-05 | Citoyen | Signaler qu’une pharmacie n’est pas vraiment de garde | Corriger les erreurs de liste |
| US-MO-06 | Citoyen | Consulter les numéros d’urgence | Obtenir de l’aide hors pharmacies |

## Flux métier principaux

### Flux A — Publication d’une période de garde

```
1. Créer DutyPeriod (start/end)
2. Upload DutyFile(s) (PDF / image / Excel)
3. Process → extraction → matching pharmacies → création Duty
4. Revue humaine (ACTIVE / TO_VERIFY / DISABLED)
5. Activer DutyPeriod (isCurrent = true; précédente désactivée)
6. Clients mobiles consomment nearest-duty
```

### Flux B — Signalement client

```
1. Client authentifié (ou identifié par clientId)
2. POST duty/report sur une pharmacie de la période courante
3. Duty passe en TO_VERIFY (ou équivalent métier)
4. Opérateur traite dans le BO
```

### Flux C — Proximité

```
1. Client fournit lat/lng (login ou requête)
2. API filtre sur Duty de la période courante
3. Calcul distance Haversine → tri nearest
```

## Règles métier

- Une seule période `isCurrent` à la fois (invariant opérationnel).
- Un avis : contrainte d’unicité `(pharmacy_id, client_id)`.
- Pharmacie : statut `ACTIF` / `INACTIF`.
- Duty : `ACTIVE` | `TO_VERIFY` | `DISABLED`.
- DutyFile : `PENDING` → `PROCESSING` → `COMPLETED` | `FAILED`.
