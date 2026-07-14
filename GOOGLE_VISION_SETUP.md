# Configuration Google Vision API

## Étape 1 : Placer le fichier JSON de credentials

Le système utilise une **priorité** pour trouver les credentials :

### PRIORITÉ 1 : Variable d'environnement (recommandé pour la production) ⭐
1. Placez le fichier où vous voulez (ex: `/etc/google-credentials.json` ou `~/credentials/google-vision.json`)
2. Définissez la variable d'environnement :
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=/chemin/vers/votre-fichier.json
   ```
3. **Avantage** : Plus sécurisé, pas dans le code, facile à changer selon l'environnement

### PRIORITÉ 2 : Fichier dans `src/main/resources/` (fallback pour développement)
1. Placez votre fichier JSON dans : `src/main/resources/google-credentials.json`
2. Configurez dans `application.properties` : `google.vision.credentials.path=google-credentials.json`
3. **Avantage** : Facile pour le développement local
4. **ATTENTION** : Ne commitez JAMAIS ce fichier dans Git ! (déjà dans `.gitignore`)

## Étape 2 : Configurer application.properties

```properties
# Activer Google Vision
google.vision.enabled=true

# Chemin vers le fichier JSON dans src/main/resources/ (fallback si pas de variable d'environnement)
google.vision.credentials.path=google-credentials.json
```

**Note** : Si vous définissez `GOOGLE_APPLICATION_CREDENTIALS`, cette configuration sera ignorée (priorité à la variable d'environnement).

## Étape 3 : Vérifier que le fichier est ignoré par Git

Le fichier `.gitignore` contient déjà :
```
google-credentials.json
*-credentials.json
credentials/
```

**IMPORTANT** : Ne commitez JAMAIS votre fichier de credentials dans Git !

## Étape 4 : Tester

1. Redémarrer l'application
2. Uploader une image ou un PDF scanné
3. Le système utilisera automatiquement Google Vision pour l'OCR

## Sécurité

- ✅ Le fichier JSON est dans `.gitignore`
- ✅ Utilisez des variables d'environnement en production
- ✅ Limitez les permissions du compte de service dans Google Cloud
- ✅ Ne partagez jamais votre fichier de credentials

## Dépannage

### Erreur : "Fichier de credentials non trouvé"
- Vérifiez le chemin dans `application.properties`
- Vérifiez que le fichier existe
- Vérifiez les permissions du fichier

### Erreur : "Permission denied"
- Vérifiez que le compte de service a les permissions Vision API
- Vérifiez que l'API Vision est activée dans Google Cloud Console

### Erreur : "Invalid credentials"
- Vérifiez que le fichier JSON est valide
- Vérifiez que le compte de service n'a pas été supprimé

