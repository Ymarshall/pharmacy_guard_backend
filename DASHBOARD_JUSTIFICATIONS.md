# Dashboard Backoffice - Justifications et API

## API Principale : `GET /api/dashboard/stats`

Cette API retourne toutes les statistiques pertinentes pour le dashboard du backoffice en un seul appel.

---

## 📊 1. Vue d'ensemble (Overview)

### Justification
Fournit une vision globale du système pour une prise de décision rapide.

### Données incluses :
- **Total Pharmacies** : Nombre total de pharmacies enregistrées
  - *Justification* : Vue globale du parc de pharmacies gérées
- **Total Périodes** : Nombre de périodes de garde créées
  - *Justification* : Suivi de l'historique des gardes
- **Total Duties** : Nombre total de gardes assignées
  - *Justification* : Volume d'activité totale
- **Total Clients** : Nombre d'utilisateurs de l'application mobile
  - *Justification* : Indicateur d'adoption de l'application
- **Total Users** : Nombre d'utilisateurs backoffice
  - *Justification* : Gestion des accès administrateurs
- **Total Zones** : Nombre de zones géographiques
  - *Justification* : Couverture géographique du système

---

## 🔄 2. Période de garde active (Current Period)

### Justification
Informations essentielles sur la période de garde en cours pour un suivi opérationnel.

### Données incluses :
- **Période ID, Dates de début/fin** : Identifiant et période de la garde active
  - *Justification* : Référence rapide de la période courante
- **Nombre de pharmacies en garde** : Nombre de pharmacies assignées à cette période
  - *Justification* : Vérifier que toutes les pharmacies sont couvertes
- **Zones couvertes** : Nombre de zones géographiques distinctes avec pharmacies en garde
  - *Justification* : Vérifier la couverture géographique
- **Fichiers uploadés** : Nombre de fichiers liés à cette période
  - *Justification* : Suivi des sources de données
- **Fichiers traités** : Nombre de fichiers traités avec succès
  - *Justification* : Indicateur de progression du traitement
- **Jours restants** : Nombre de jours avant la fin de la période
  - *Justification* : Planification de la prochaine période

---

## 💊 3. Statistiques Pharmacies (Pharmacy Stats)

### Justification
Analyser la répartition et l'état des pharmacies pour identifier les zones à améliorer.

### Données incluses :
- **Pharmacies actives vs inactives** : Répartition par statut
  - *Justification* : Identifier les pharmacies à réactiver ou archiver
- **Distribution par zone** : Nombre de pharmacies par ville/zone
  - *Justification* : Identifier les zones sur/sous-couvertes
- **Top 5 zones** : Zones avec le plus de pharmacies
  - *Justification* : Visualisation rapide des zones principales
- **Pharmacies créées ce mois** : Nouveautés récentes
  - *Justification* : Suivi de l'expansion du réseau (nécessite un champ createdAt)

---

## 📁 4. Statistiques Fichiers (File Processing Stats)

### Justification
Suivre l'efficacité du traitement des fichiers PDF/images pour détecter les problèmes.

### Données incluses :
- **Total fichiers** : Nombre total de fichiers uploadés
  - *Justification* : Volume global de fichiers traités
- **Par statut** : PENDING, PROCESSING, COMPLETED, FAILED
  - *Justification* : État du traitement en temps réel
- **Taux de succès** : Pourcentage de fichiers traités avec succès
  - *Justification* : Indicateur de qualité du processus
- **Total pharmacies extraites** : Nombre total de pharmacies extraites de tous les fichiers
  - *Justification* : Volume de données extraites

---

## 📈 5. Activité récente (Recent Activity)

### Justification
Suivre les dernières actions pour un audit rapide et une traçabilité.

### Données incluses :
- **5 dernières périodes créées** : Avec dates et nombre de gardes
  - *Justification* : Historique récent des périodes
- **5 derniers fichiers uploadés** : Avec statut et date
  - *Justification* : Suivi des uploads récents
- **5 dernières pharmacies créées** : Avec zone
  - *Justification* : Suivi de l'ajout de nouvelles pharmacies

---

## 🚨 6. Alertes (Alerts)

### Justification
Détecter automatiquement les problèmes nécessitant une attention immédiate.

### Types d'alertes :
1. **FILE_ERROR** (HIGH) : Fichiers en erreur de traitement
   - *Justification* : Action requise pour corriger les erreurs
2. **PROCESSING_STUCK** (MEDIUM) : Fichiers bloqués en traitement (>30min)
   - *Justification* : Détecter les traitements anormalement longs
3. **NO_ACTIVE_PERIOD** (HIGH) : Aucune période active
   - *Justification* : Système inutilisable sans période active
4. **PERIOD_EXPIRING** (MEDIUM) : Période expire dans <3 jours
   - *Justification* : Planification proactive de la prochaine période
5. **PENDING_FILES** (LOW) : Fichiers en attente de traitement
   - *Justification* : Rappel pour traiter les fichiers en attente

---

## 📋 Recommandations d'affichage Dashboard

### Section 1 : Header (Vue d'ensemble)
```
┌─────────────────────────────────────────────────────────┐
│  Total Pharmacies: 150  │  Périodes: 25  │  Duties: 450 │
│  Clients: 1250  │  Users: 5  │  Zones: 120              │
└─────────────────────────────────────────────────────────┘
```

### Section 2 : Période Active (Card principale)
```
┌─────────────────────────────────────────────────────────┐
│  🟢 PERIODE ACTIVE                                       │
│  09/01/2026 - 15/01/2026 (5 jours restants)            │
│  • 38 pharmacies en garde                               │
│  • 12 zones couvertes                                   │
│  • 3 fichiers uploadés, 2 traités                      │
└─────────────────────────────────────────────────────────┘
```

### Section 3 : Statistiques (Graphiques)
- Graphique en barres : Pharmacies par zone (Top 5)
- Graphique circulaire : Répartition Actif/Inactif
- Graphique linéaire : Évolution des fichiers par statut

### Section 4 : Activité récente (Liste)
- Tableau : 5 dernières périodes
- Tableau : 5 derniers fichiers
- Tableau : 5 dernières pharmacies

### Section 5 : Alertes (Notifications)
- Badge rouge : Alertes HIGH
- Badge orange : Alertes MEDIUM
- Badge bleu : Alertes LOW

