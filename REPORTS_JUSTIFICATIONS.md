# Menu Rapport - APIs et Justifications

## Vue d'ensemble

Le menu **Rapport** contient **4 graphiques** différents pour analyser les statistiques du système de gestion de pharmacies de garde.

---

## 📊 Graphique 1 : Camembert (Pie Chart)

### API : `GET /api/reports/chart/pharmacy-status-pie`

### Description
Graphique en camembert affichant la répartition des pharmacies entre **Actif** et **Inactif**.

### Justification
- **Visuel immédiat** : Permet de voir en un coup d'œil la proportion de pharmacies opérationnelles
- **Décision rapide** : Identifie rapidement si un grand nombre de pharmacies sont inactives (action requise)
- **Métrique clé** : Le ratio Actif/Inactif est un indicateur de santé du réseau
- **Action** : Si plus de 20% sont inactives, une campagne de réactivation peut être nécessaire

### Données retournées
```json
{
  "title": "Répartition des pharmacies par statut",
  "chartType": "pie",
  "labels": ["Actif", "Inactif"],
  "values": [145, 5],
  "metadata": {
    "activePercentage": 97,
    "inactivePercentage": 3,
    "total": 150,
    "colors": ["#10B981", "#EF4444"]
  }
}
```

### Utilisation dans l'UI
- Afficher un graphique en camembert avec deux segments
- Couleur verte (#10B981) pour "Actif", rouge (#EF4444) pour "Inactif"
- Afficher les pourcentages dans les segments ou en légende

---

## 📈 Graphique 2 : Ligne (Line Chart)

### API : `GET /api/reports/chart/pharmacy-creation-trend-line`

### Paramètres (optionnels)
- `startDate` (format: YYYY-MM-DD) : Date de début. **Par défaut** : 12 mois avant aujourd'hui
- `endDate` (format: YYYY-MM-DD) : Date de fin. **Par défaut** : aujourd'hui

### Description
Graphique en ligne montrant l'**évolution du nombre de périodes de garde créées par mois**.

### Justification
- **Analyse temporelle** : Permet de détecter des tendances (croissance, stagnation, décroissance)
- **Planification** : Aide à prévoir la charge de travail future basée sur les tendances passées
- **Détection d'anomalies** : Une chute brutale peut indiquer un problème opérationnel
- **Évaluation de performance** : Comparer les mois pour évaluer l'efficacité des processus

### Données retournées
```json
{
  "title": "Évolution du nombre de gardes par période",
  "chartType": "line",
  "labels": ["2025-01", "2025-02", "2025-03", ...],
  "values": [2, 4, 3, 5, 4, ...],
  "metadata": {
    "totalPeriods": 25,
    "color": "#3B82F6"
  }
}
```

### Utilisation dans l'UI
- Afficher un graphique linéaire avec les mois en axe X et le nombre de périodes en axe Y
- Permettre à l'utilisateur de filtrer par plage de dates
- Afficher une ligne de tendance si possible

---

## 📊 Graphique 3 : Histogramme (Bar Chart)

### API : `GET /api/reports/chart/top-zones-bar`

### Description
Histogramme affichant les **Top 10 des zones géographiques avec le plus grand nombre de pharmacies**.

### Justification
- **Répartition géographique** : Visualise la couverture du réseau par zone
- **Identification des zones prioritaires** : Les zones avec le plus de pharmacies nécessitent une attention particulière
- **Planification stratégique** : Aide à décider où concentrer les efforts d'expansion
- **Équilibrage** : Détecte les déséquilibres entre zones (trop concentrées vs trop dispersées)
- **Action** : Identifier les zones sous-représentées pour une expansion ciblée

### Données retournées
```json
{
  "title": "Top 10 des zones avec le plus de pharmacies",
  "chartType": "bar",
  "labels": ["ABIDJAN", "BOUAKE", "YAMOUSSOUKRO", "ABENGOUROU", ...],
  "values": [45, 28, 15, 12, ...],
  "metadata": {
    "totalZones": 120,
    "color": "#8B5CF6"
  }
}
```

### Utilisation dans l'UI
- Afficher un graphique en barres horizontales ou verticales
- Les barres doivent être triées par ordre décroissant
- Permettre de voir le nombre exact au survol (tooltip)
- Afficher "Top 10" mais indiquer le total de zones dans les métadonnées

---

## 📈 Graphique 4 : Zone (Area Chart)

### API : `GET /api/reports/chart/cumulative-trend-area`

### Paramètres (optionnels)
- `startDate` (format: YYYY-MM-DD) : Date de début. **Par défaut** : 12 mois avant aujourd'hui
- `endDate` (format: YYYY-MM-DD) : Date de fin. **Par défaut** : aujourd'hui

### Description
Graphique en zone (area chart) montrant l'**évolution cumulée** du nombre de **périodes de garde créées** et du nombre total de **gardes assignées** au fil du temps.

### Justification
- **Visualisation de croissance** : Montre la croissance globale du système sur une période
- **Relation périodes/gardes** : Permet de voir la corrélation entre la création de périodes et l'assignation de gardes
- **Efficacité opérationnelle** : Si les gardes croissent plus vite que les périodes, le système devient plus efficace
- **Projection** : Aide à projeter la croissance future basée sur les tendances cumulées
- **Double métrique** : Deux séries de données superposées pour une analyse comparative

### Données retournées
```json
{
  "title": "Évolution cumulée des périodes de garde et gardes assignées",
  "chartType": "area",
  "labels": ["2025-01", "2025-02", "2025-03", ...],
  "series": [
    {
      "name": "Périodes de garde",
      "data": [2, 6, 9, 14, 18, ...],
      "color": "#3B82F6"
    },
    {
      "name": "Gardes assignées",
      "data": [38, 76, 114, 190, 228, ...],
      "color": "#10B981"
    }
  ],
  "metadata": {
    "totalPeriods": 25,
    "totalDuties": 450
  }
}
```

### Utilisation dans l'UI
- Afficher un graphique en zone empilé ou superposé (superposé recommandé pour comparaison)
- Deux zones : bleue (#3B82F6) pour les périodes, verte (#10B981) pour les gardes
- Légende interactive pour montrer/masquer une série
- Permettre le filtrage par plage de dates
- Afficher les valeurs cumulées au survol (tooltip)

---

## 🎨 Recommandations d'affichage dans l'UI

### Layout suggéré pour le menu Rapport

```
┌─────────────────────────────────────────────────────────────┐
│  📊 RAPPORTS ET STATISTIQUES                                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────┐  ┌──────────────────────────────┐ │
│  │  1. Camembert        │  │  2. Ligne                    │ │
│  │  (Répartition Statut)│  │  (Évolution Périodes)       │ │
│  │                      │  │                              │ │
│  │    [Pie Chart]       │  │    [Line Chart]              │ │
│  │                      │  │    [Filtre dates]            │ │
│  └──────────────────────┘  └──────────────────────────────┘ │
│                                                              │
│  ┌──────────────────────┐  ┌──────────────────────────────┐ │
│  │  3. Histogramme      │  │  4. Zone                    │ │
│  │  (Top 10 Zones)      │  │  (Cumulatif)                │ │
│  │                      │  │                              │ │
│  │    [Bar Chart]       │  │    [Area Chart]              │ │
│  │                      │  │    [Filtre dates]            │ │
│  └──────────────────────┘  └──────────────────────────────┘ │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Filtres communs
- **Période** : Sélecteur de plage de dates (pour les graphiques temporels)
- **Export** : Bouton pour exporter les données en CSV/PDF
- **Rafraîchissement** : Bouton pour actualiser les données

### Couleurs recommandées
- **Actif** : Vert (#10B981)
- **Inactif** : Rouge (#EF4444)
- **Périodes** : Bleu (#3B82F6)
- **Gardes** : Vert (#10B981)
- **Zones** : Violet (#8B5CF6)

---

## 🔄 Mises à jour futures possibles

1. **Graphique en donut** : Alternative au camembert avec un centre pour afficher le total
2. **Graphique en barres empilées** : Répartition des pharmacies par statut ET par zone
3. **Graphique de corrélation** : Relation entre nombre de clients et nombre de pharmacies par zone
4. **Graphique de taux de traitement** : Évolution du taux de succès des fichiers traités par mois

---

## 📝 Notes importantes

- **Performance** : Les graphiques sont optimisés pour des volumes de données raisonnables (< 10 000 enregistrements)
- **Cachage** : Considérer le cache des données de rapports pour améliorer les performances
- **Données historiques** : Les graphiques utilisent les dates des périodes de garde comme référence temporelle
- **Extensibilité** : La structure `ChartDataDto` permet d'ajouter facilement de nouveaux types de graphiques

