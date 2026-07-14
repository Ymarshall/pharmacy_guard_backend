# Traitement des Documents de Garde

## Vue d'ensemble

Ce système permet d'extraire les informations des fichiers de garde (PDF texte, PDF scanné, ou images) et de les structurer en JSON.

## Structure des données extraites

Chaque pharmacie extraite contient :
- **zone** : La ville/zone (ex: "ABENGOUROU")
- **pharmacyName** : Le nom de la pharmacie (ex: "PHARMACIE DU MARCHE")
- **pharmacistName** : Le nom du pharmacien (ex: "MME SENIN DON MELLO A. CHRISTELLE")
- **phoneNumbers** : Les numéros de téléphone (ex: "27 35 91 30 09/01 02 03 87 17")
- **location** : La localisation/description (ex: "PLACE DU GRAND MARCHE/ A COTE DE TELECEL")
- **commune** : La commune si disponible
- **confidence** : Niveau de confiance de l'extraction (0.0 à 1.0)

## Format JSON de sortie

```json
{
  "dutyFileId": 1,
  "status": "COMPLETED",
  "pharmaciesFound": 15,
  "dutiesCreated": 12,
  "pharmacies": [
    {
      "zone": "ABENGOUROU",
      "pharmacyName": "PHARMACIE DU MARCHE",
      "pharmacistName": "MME SENIN DON MELLO A. CHRISTELLE",
      "phoneNumbers": "27 35 91 30 09/01 02 03 87 17",
      "location": "PLACE DU GRAND MARCHE/ A COTE DE TELECEL",
      "commune": null,
      "pharmacyId": 5,
      "existsInDatabase": true,
      "confidence": 0.9
    }
  ]
}
```

## Solutions disponibles

### 1. PDF Texte (Actuel)
- **Méthode** : Extraction directe avec Apache PDFBox
- **Avantages** : Rapide, gratuit, pas de dépendance externe
- **Limitations** : Ne fonctionne que pour les PDF avec texte extractible

### 2. OCR avec Tesseract (Recommandé pour images)
- **Méthode** : OCR open-source
- **Avantages** : Gratuit, open-source, fonctionne hors ligne
- **Limitations** : Moins précis que Google Vision, nécessite installation
- **Dépendance** : `net.sourceforge.tess4j:tess4j:5.x.x`

### 3. Google Vision API (Recommandé pour production)
- **Méthode** : API cloud de Google
- **Avantages** : Très précis, supporte plusieurs langues, gestion de la qualité
- **Limitations** : Nécessite une clé API, coûts selon usage
- **Configuration** : Voir `GoogleVisionServiceImpl.java`

## Recommandation

Pour votre cas d'usage avec des fichiers contenant :
- Zones/Villes
- Noms de pharmacies
- Noms de pharmaciens
- Numéros de téléphone
- Localisations

**Je recommande Google Vision API** car :
1. **Précision élevée** : Meilleure reconnaissance de texte, surtout pour les documents scannés
2. **Support multilingue** : Gère bien le français et les caractères spéciaux
3. **Structuration** : Peut détecter la structure du document (colonnes, tableaux)
4. **Scalabilité** : Gère bien les volumes importants

## Configuration Google Vision API

1. Créer un projet sur [Google Cloud Platform](https://console.cloud.google.com/)
2. Activer l'API Vision
3. Créer une clé API
4. Ajouter dans `application.properties` :
   ```properties
   google.vision.enabled=true
   google.vision.api.key=VOTRE_CLE_API
   ```
5. Ajouter la dépendance dans `build.gradle` :
   ```gradle
   implementation 'com.google.cloud:google-cloud-vision:3.0.0'
   ```

## Alternative : Tesseract OCR

Si vous préférez une solution gratuite et locale :

1. Installer Tesseract sur le serveur
2. Ajouter la dépendance :
   ```gradle
   implementation 'net.sourceforge.tess4j:tess4j:5.8.0'
   ```
3. Configurer le chemin vers Tesseract dans `application.properties`

## Prochaines étapes

1. Tester avec un fichier réel
2. Ajuster les patterns regex selon le format exact de vos fichiers
3. Implémenter Google Vision ou Tesseract selon votre choix
4. Améliorer le matching avec les pharmacies existantes (fuzzy matching)

