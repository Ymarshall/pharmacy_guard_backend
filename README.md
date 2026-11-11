# Pharmacie de Garde - API REST

API REST complète pour la gestion d'une application mobile de pharmacies de garde en Côte d'Ivoire, avec un back-office pour l'administration.

## 📋 Table des matières

- [Description](#description)
- [Technologies et Versions](#technologies-et-versions)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Configuration](#configuration)
- [Structure du Projet](#structure-du-projet)
- [Fonctionnalités](#fonctionnalités)
- [APIs Disponibles](#apis-disponibles)
- [Documentation Swagger](#documentation-swagger)
- [Services Externes](#services-externes)
- [Base de Données](#base-de-données)
- [Démarrage](#démarrage)
- [Tests](#tests)

## 📖 Description

Cette application est une API REST développée avec Spring Boot pour gérer :
- **Application Mobile** : Recherche de pharmacies de garde, numéros d'urgence, géolocalisation
- **Back-Office** : Gestion des pharmacies, périodes de garde, traitement de fichiers PDF, statistiques et rapports

### Fonctionnalités principales

- ✅ Gestion complète des pharmacies (CRUD)
- ✅ Gestion des périodes de garde
- ✅ Traitement automatique de fichiers PDF/JPEG pour extraire les listes de pharmacies de garde
- ✅ OCR avec Google Vision API pour les images
- ✅ Géocodage automatique des pharmacies avec Google Maps API
- ✅ Recherche de pharmacies par nom, ville, commune, district
- ✅ Calcul des pharmacies les plus proches basé sur la géolocalisation
- ✅ Gestion des clients (app mobile) avec authentification par téléphone
- ✅ Gestion des utilisateurs (back-office) avec authentification email/mot de passe
- ✅ Gestion des numéros d'urgence
- ✅ Gestion des zones (villes, communes, districts)
- ✅ Dashboard avec statistiques en temps réel
- ✅ Rapports avec graphiques (pie, line, bar, area)
- ✅ Gestion des assurances prises en charge par les pharmacies

## 🛠 Technologies et Versions

### Framework et Outils

- **Java** : 17
- **Spring Boot** : 3.5.9
- **Spring Data JPA** : 3.5.9
- **Spring Security** : 3.5.9
- **Spring Web** : 3.5.9
- **Gradle** : 8.x

### Base de Données

- **PostgreSQL** : 12+

### Bibliothèques

- **SpringDoc OpenAPI (Swagger)** : 2.6.0
- **Apache PDFBox** : 3.0.3
- **Google Cloud Vision API** : 3.7.0
- **Jackson** : (inclus dans Spring Boot)
- **BCrypt** : (inclus dans Spring Security)

## 📦 Prérequis

Avant de commencer, assurez-vous d'avoir installé :

1. **Java 17** ou supérieur
   ```bash
   java -version
   ```

2. **PostgreSQL** 12 ou supérieur
   ```bash
   psql --version
   ```

3. **Gradle** 8.x (optionnel, le wrapper est inclus)
   ```bash
   ./gradlew --version
   ```

4. **Clé API Google Maps** (pour le géocodage)
   - Obtenez votre clé sur : https://console.cloud.google.com/google/maps-apis/credentials
   - Activez l'API "Geocoding API"

5. **Compte de service Google Cloud** (optionnel, pour OCR)
   - Fichier JSON de credentials
   - Activez l'API "Cloud Vision API"

## 🚀 Installation

### 1. Cloner le projet

```bash
git clone <repository-url>
cd pharmaciegarde
```

### 2. Créer la base de données PostgreSQL

```sql
CREATE DATABASE pharmacy_guarde_db;
```

### 3. Configurer la base de données

Modifiez les variables d'environnement (voir `.env.example`). Ne placez plus de secret dans `application.properties`.

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pharmacy_guarde_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=votre_mot_de_passe
export JWT_SECRET=une-chaine-aleatoire-d-au-moins-32-caracteres
```

### 4. Configurer les services externes (optionnel)

#### Google Maps API

Ajoutez votre clé API dans `application.properties` ou via une variable d'environnement :

```bash
export GOOGLE_MAPS_API_KEY=votre_cle_api
export GOOGLE_VISION_ENABLED=true
export GOOGLE_APPLICATION_CREDENTIALS=/chemin/vers/votre-fichier.json
```

Le fichier JSON de compte de service ne doit jamais être commité. Le nom `pharmacie-guarde-26c4adebd2b7.json` qui figurait dans la configuration a été retiré.

#### Google Vision API (pour OCR)

Placez votre fichier JSON de credentials dans `src/main/resources/` ou configurez la variable d'environnement :

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/chemin/vers/votre-fichier.json
```

### 5. Compiler et lancer l'application

```bash
./gradlew build
./gradlew bootRun
```

L'application sera accessible sur : `http://localhost:8080`

## ⚙️ Configuration

### Variables d'environnement recommandées

Pour la production, utilisez des variables d'environnement :

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pharmacy_guarde_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=votre_mot_de_passe
export GOOGLE_MAPS_API_KEY=votre_cle_api
export GOOGLE_APPLICATION_CREDENTIALS=/chemin/vers/credentials.json
```

### Configuration des fichiers uploadés

Les fichiers PDF/JPEG sont stockés dans :
- `uploads/duty-files/` : Fichiers uploadés
- `uploads/extractions/` : Fichiers texte extraits

## 📁 Structure du Projet

```
src/main/java/com/buddies/pharmaciegarde/
├── common/                    # Utilitaires communs
│   ├── exception/            # Gestion des exceptions
│   └── utils/                # Utilitaires (DistanceCalculator, etc.)
├── config/                    # Configuration Spring
│   ├── app/                  # Configuration application (CORS, etc.)
│   └── security/             # Configuration Spring Security
└── modules/
    ├── common/               # Modules communs
    │   ├── controller/       # EmergencyNumberController, ZoneController
    │   ├── dto/              # DTOs
    │   ├── model/            # EmergencyNumber, Zone
    │   ├── repository/       # Repositories
    │   └── service/          # Services
    ├── dashboard/            # Dashboard et statistiques
    ├── duty/                 # Gestion des gardes
    │   ├── controller/       # DutyController, DutyFileController, DutyPeriodController
    │   ├── dto/              # DTOs
    │   ├── model/            # Duty, DutyFile, DutyPeriod
    │   ├── repository/       # Repositories
    │   └── service/          # Services (traitement PDF, OCR, etc.)
    ├── pharmacy/            # Gestion des pharmacies
    │   ├── controller/       # PharmacyController, ClientPharmacyController, etc.
    │   ├── dto/              # DTOs
    │   ├── model/            # Pharmacy, Assurance
    │   ├── repository/       # Repositories
    │   └── service/          # Services (géocodage, etc.)
    ├── report/               # Rapports et graphiques
    └── user/                 # Gestion des utilisateurs et clients
        ├── controller/       # UserController, ClientController, AuthController
        ├── dto/              # DTOs
        ├── model/            # User, Client
        ├── repository/       # Repositories
        └── service/          # Services
```

## 🎯 Fonctionnalités

### 1. Gestion des Pharmacies

- ✅ CRUD complet des pharmacies
- ✅ Recherche par nom, ville, commune, district
- ✅ Calcul des pharmacies les plus proches (géolocalisation)
- ✅ Gestion des pharmacies de garde pour une période active
- ✅ Association avec des assurances
- ✅ Géocodage automatique avec Google Maps API
- ✅ Initialisation automatique de 20 pharmacies au démarrage

### 2. Gestion des Gardes

- ✅ Création de périodes de garde (startDate, endDate, isCurrent)
- ✅ Upload de fichiers PDF/JPEG contenant les listes de pharmacies de garde
- ✅ Traitement automatique des fichiers :
  - Extraction de texte depuis PDF
  - OCR avec Google Vision pour les images JPEG
  - Parsing intelligent pour extraire : zone, nom, pharmacien, téléphone, localisation
  - Création automatique des pharmacies si elles n'existent pas
  - Création automatique des gardes
- ✅ Gestion des fichiers de garde (upload, traitement, suppression)
- ✅ Historique des gardes par pharmacie

### 3. Gestion des Clients (App Mobile)

- ✅ Inscription avec téléphone et géolocalisation
- ✅ Authentification par téléphone uniquement
- ✅ Mise à jour de la localisation
- ✅ Activation/désactivation des comptes
- ✅ Recherche des pharmacies de garde les plus proches

### 4. Gestion des Utilisateurs (Back-Office)

- ✅ CRUD complet des utilisateurs
- ✅ Authentification email/mot de passe
- ✅ Mise à jour du mot de passe
- ✅ Hashage des mots de passe avec BCrypt

### 5. Numéros d'Urgence

- ✅ CRUD complet des numéros d'urgence
- ✅ Catégories : medical, fire, police, bloodBank, other
- ✅ Support des icônes (Flutter Material Icons)
- ✅ Couleurs personnalisables
- ✅ Initialisation automatique au démarrage

### 6. Zones (Villes, Communes, Districts)

- ✅ CRUD complet des zones
- ✅ Types : city, commune, district
- ✅ Initialisation automatique avec les villes et communes de Côte d'Ivoire
- ✅ Normalisation automatique (majuscules, sans accents)

### 7. Dashboard

- ✅ Statistiques globales (pharmacies, clients, utilisateurs, gardes)
- ✅ Informations sur la période de garde active
- ✅ Distribution des pharmacies par statut
- ✅ Statistiques de traitement de fichiers
- ✅ Activité récente
- ✅ Alertes (période expirée, fichiers non traités, etc.)

### 8. Rapports

- ✅ Graphique en camembert : Répartition des pharmacies par statut
- ✅ Graphique linéaire : Tendance de création des pharmacies
- ✅ Graphique en barres : Top 10 des zones avec le plus de pharmacies
- ✅ Graphique en aires : Tendance cumulative des gardes

## 📡 APIs Disponibles

### Authentification

#### Back-Office
- `POST /api/auth/login` - Connexion utilisateur (email/password)

#### App Mobile
- `POST /api/auth/client/login` - Connexion client (téléphone uniquement)
- `POST /api/auth/client/register` - Inscription client (téléphone + géolocalisation)

### Pharmacies

#### Back-Office
- `GET /api/pharmacies` - Lister toutes les pharmacies
- `GET /api/pharmacies/{id}` - Récupérer une pharmacie
- `POST /api/pharmacies` - Créer une pharmacie
- `PUT /api/pharmacies/{id}` - Mettre à jour une pharmacie
- `DELETE /api/pharmacies/{id}` - Supprimer une pharmacie
- `GET /api/pharmacies/city/{city}` - Pharmacies par ville
- `GET /api/pharmacies/commune/{commune}` - Pharmacies par commune
- `GET /api/pharmacies/statut/{statut}` - Pharmacies par statut
- `POST /api/pharmacies/update-geocoding` - Mettre à jour les coordonnées GPS de toutes les pharmacies
- `POST /api/pharmacies/{id}/update-geocoding` - Mettre à jour les coordonnées GPS d'une pharmacie

#### App Mobile
- `GET /api/client/pharmacies/nearest` - Pharmacies les plus proches (toutes)
- `GET /api/client/pharmacies/nearest-duty` - Pharmacies de garde les plus proches (période active)
- `GET /api/client/pharmacies/search` - Recherche de pharmacies

### Gardes

- `GET /api/duties` - Lister toutes les gardes
- `GET /api/duties/{id}` - Récupérer une garde
- `POST /api/duties` - Créer une garde
- `PUT /api/duties/{id}` - Mettre à jour une garde
- `DELETE /api/duties/{id}` - Supprimer une garde
- `GET /api/duties/pharmacy/{pharmacyId}` - Gardes d'une pharmacie
- `GET /api/duties/pharmacy/{pharmacyId}/history` - Historique des gardes d'une pharmacie
- `GET /api/duties/period/{dutyPeriodId}` - Gardes d'une période
- `DELETE /api/duties/period/{dutyPeriodId}/clear` - Supprimer toutes les gardes d'une période

### Périodes de Garde

- `GET /api/duty-periods` - Lister toutes les périodes
- `GET /api/duty-periods/{id}` - Récupérer une période
- `POST /api/duty-periods` - Créer une période
- `PUT /api/duty-periods/{id}` - Mettre à jour une période
- `DELETE /api/duty-periods/{id}` - Supprimer une période
- `GET /api/duty-periods/current` - Période active
- `POST /api/duty-periods/{id}/activate` - Activer une période

### Fichiers de Garde

- `POST /api/duty-files/upload` - Uploader un ou plusieurs fichiers PDF/JPEG
  - Paramètres : `file` (un seul fichier) ou `files` (plusieurs fichiers)
  - Paramètres : `dutyPeriodId` (obligatoire), `description` (optionnel)
- `GET /api/duty-files` - Lister tous les fichiers
- `GET /api/duty-files/{id}` - Récupérer un fichier
- `DELETE /api/duty-files/{id}` - Supprimer un fichier
- `GET /api/duty-files/period/{dutyPeriodId}` - Fichiers d'une période
- `POST /api/duty-files/{id}/process` - Traiter un fichier (extraction intelligente)
- `POST /api/duty-files/{id}/extract-text` - Extraire le texte brut (sans AI)

### Clients

- `GET /api/clients` - Lister tous les clients
- `GET /api/clients/{id}` - Récupérer un client
- `POST /api/clients` - Créer un client
- `PUT /api/clients/{id}` - Mettre à jour un client
- `DELETE /api/clients/{id}` - Supprimer un client
- `PUT /api/clients/{id}/location` - Mettre à jour la localisation
- `PUT /api/clients/{id}/status` - Activer/désactiver un client

### Utilisateurs (Back-Office)

- `GET /api/users` - Lister tous les utilisateurs
- `GET /api/users/{id}` - Récupérer un utilisateur
- `POST /api/users` - Créer un utilisateur
- `PUT /api/users/{id}` - Mettre à jour un utilisateur
- `DELETE /api/users/{id}` - Supprimer un utilisateur
- `PUT /api/users/{id}/password` - Mettre à jour le mot de passe

### Assurances

- `GET /api/assurances` - Lister toutes les assurances
- `GET /api/assurances/{id}` - Récupérer une assurance
- `POST /api/assurances` - Créer une assurance
- `PUT /api/assurances/{id}` - Mettre à jour une assurance
- `DELETE /api/assurances/{id}` - Supprimer une assurance

### Numéros d'Urgence

- `GET /api/emergency-numbers` - Lister tous les numéros d'urgence
- `GET /api/emergency-numbers/{id}` - Récupérer un numéro d'urgence
- `POST /api/emergency-numbers` - Créer un numéro d'urgence
- `PUT /api/emergency-numbers/{id}` - Mettre à jour un numéro d'urgence
- `DELETE /api/emergency-numbers/{id}` - Supprimer un numéro d'urgence

### Zones

- `GET /api/zones` - Lister toutes les zones
- `GET /api/zones/{id}` - Récupérer une zone
- `POST /api/zones` - Créer une zone
- `PUT /api/zones/{id}` - Mettre à jour une zone
- `DELETE /api/zones/{id}` - Supprimer une zone
- `GET /api/zones/search` - Rechercher des zones
- `GET /api/zones/active` - Zones actives

### Dashboard

- `GET /api/dashboard/stats` - Statistiques du dashboard

### Rapports

- `GET /api/reports/pharmacy-status-pie` - Graphique camembert : Statut des pharmacies
- `GET /api/reports/pharmacy-creation-trend-line` - Graphique linéaire : Tendance de création
- `GET /api/reports/top-zones-bar` - Graphique barres : Top zones
- `GET /api/reports/cumulative-duty-trend-area` - Graphique aires : Tendance cumulative des gardes

## 📚 Documentation Swagger

Une fois l'application démarrée, accédez à la documentation Swagger :

- **Swagger UI** : http://localhost:8080/swagger-ui.html
- **API Docs (JSON)** : http://localhost:8080/api-docs

La documentation Swagger inclut :
- Tous les endpoints disponibles
- Les modèles de données (DTOs)
- Les exemples de requêtes/réponses
- La possibilité de tester les APIs directement depuis l'interface

## 🔌 Services Externes

### Google Maps API

**Utilisation** : Géocodage des adresses de pharmacies pour obtenir les coordonnées GPS

**Configuration** :
1. Obtenez une clé API sur https://console.cloud.google.com/google/maps-apis/credentials
2. Activez l'API "Geocoding API"
3. Configurez la clé via variable d'environnement ou dans `application.properties`

**Endpoints utilisant Google Maps** :
- `POST /api/pharmacies/update-geocoding` - Mise à jour des coordonnées GPS

**Coût** : ~$5 pour 1000 requêtes (après quota gratuit)

### Google Cloud Vision API

**Utilisation** : OCR pour extraire le texte des images JPEG de listes de pharmacies

**Configuration** :
1. Créez un compte de service sur Google Cloud Console
2. Téléchargez le fichier JSON de credentials
3. Placez-le dans `src/main/resources/` ou configurez `GOOGLE_APPLICATION_CREDENTIALS`

**Endpoints utilisant Google Vision** :
- `POST /api/duty-files/{id}/process` - Traitement avec OCR (si fichier JPEG)

**Coût** : Payant selon le nombre d'images traitées

## 🗄️ Base de Données

### Schéma principal

- **pharmacies** : Informations des pharmacies
- **duties** : Gardes des pharmacies (liées à une période)
- **duty_periods** : Périodes de garde (startDate, endDate, isCurrent)
- **duty_files** : Fichiers PDF/JPEG uploadés
- **clients** : Clients de l'app mobile
- **users** : Utilisateurs du back-office
- **assurances** : Assurances prises en charge
- **pharmacy_assurances** : Table de liaison (many-to-many)
- **emergency_numbers** : Numéros d'urgence
- **zones** : Villes, communes, districts

### Initialisation automatique

Au démarrage de l'application, les données suivantes sont automatiquement initialisées :

1. **Zones** : Liste complète des villes et communes de Côte d'Ivoire
2. **Pharmacies** : 20 pharmacies pré-configurées
3. **Numéros d'urgence** : Liste des numéros d'urgence avec icônes Flutter Material

## 🏃 Démarrage

### Mode développement

```bash
./gradlew bootRun
```

### Mode production

```bash
./gradlew build
java -jar build/libs/pharmaciegarde-0.0.1-SNAPSHOT.jar
```

### Vérifier que l'application fonctionne

1. Accédez à Swagger UI : http://localhost:8080/swagger-ui.html
2. Testez un endpoint simple : `GET /api/pharmacies`

## 🧪 Tests

```bash
./gradlew test
```

## 📝 Notes importantes

### Convention de nommage des pharmacies

- Toutes les pharmacies doivent commencer par **"PHCIE"** (et non "PHARMACIE")
- Le service d'initialisation normalise automatiquement les noms

### Détection des doublons de pharmacies

Les pharmacies sont détectées comme doublons si elles ont :
- Le même nom (normalisé)
- La même commune
- La même ville
- Le même district

### Gestion des périodes de garde

- Une seule période peut être active (`isCurrent = true`) à la fois
- L'activation d'une période désactive automatiquement toutes les autres

### Traitement des fichiers

- **PDF** : Extraction de texte directe avec Apache PDFBox
- **JPEG** : OCR avec Google Vision API (si configuré)
- Les pharmacies extraites sont automatiquement créées si elles n'existent pas
- Les gardes sont créées automatiquement pour la période liée au fichier

## 🔒 Sécurité

- Mots de passe back-office hachés avec BCrypt (coût 12).
- API stateless : jeton JWT `Authorization: Bearer` (pas de session, CSRF désactivé pour cette raison).
- Lectures publiques conservées pour l'application mobile (pharmacies, proximité, urgences, zones, période courante).
- Écritures back-office, utilisateurs, fichiers et signalements réservés aux rôles `ADMIN`, `OPERATOR` ou `CLIENT` selon l'endpoint.
- CORS limité aux origines de `CORS_ALLOWED_ORIGINS`.
- Swagger coupé avec le profil `prod` (`SWAGGER_ENABLED=false`).
- Uploads contrôlés (signature réelle, 10 Mo, nom aléatoire, hors racine web).
- Limite de tentatives sur les connexions.

### Import d'affiches (prévisualisation)

1. `POST /api/duty-files/upload`
2. `POST /api/duty-files/{id}/preview` — lignes, score, alternatives, sans créer de garde
3. L'administrateur corrige
4. `POST /api/duty-files/{id}/confirm` — création de masse, doublons ignorés

`POST /api/duty-files/{id}/process` ne crée plus automatiquement les pharmacies inconnues. Passer `autoCreateUnknown=true` pour les lignes sans aucun candidat proche. Les lignes ambiguës ne sont jamais créées automatiquement. `POST /api/duty-files/{id}/process-async` lance le même traitement hors du thread HTTP.

### Pagination

Sans `page` ni `size`, les listes restent des tableaux JSON. Avec l'un des deux paramètres, la réponse est `{ content, page, size, totalElements, totalPages }`.

## 📞 Support

Pour toute question ou problème, veuillez créer une issue dans le dépôt du projet.

## 📄 Licence

[À définir]

---

**Version** : 0.0.1-SNAPSHOT  
**Dernière mise à jour** : Janvier 2026

