package com.buddies.pharmaciegarde.modules.duty.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.protobuf.ByteString;

/**
 * Implémentation du service Google Vision API
 * 
 * Pour utiliser ce service, vous devez :
 * 1. Créer un projet Google Cloud Platform
 * 2. Activer l'API Vision
 * 3. Créer un compte de service et télécharger le fichier JSON
 * 4. Placer le fichier JSON dans src/main/resources/ ou à la racine du projet
 * 5. Configurer le chemin dans application.properties :
 *    google.vision.credentials.path=google-credentials.json
 *    google.vision.enabled=true
 * 6. OU utiliser la variable d'environnement GOOGLE_APPLICATION_CREDENTIALS
 */
@Service
public class GoogleVisionServiceImpl implements GoogleVisionService {

	@Value("${google.vision.credentials.path:}")
	private String credentialsPath;

	@Value("${google.vision.enabled:false}")
	private boolean enabled;

	private final DocumentProcessingService documentProcessingService;
	private final com.buddies.pharmaciegarde.modules.common.repository.ZoneRepository zoneRepository;

	public GoogleVisionServiceImpl(
			DocumentProcessingService documentProcessingService,
			com.buddies.pharmaciegarde.modules.common.repository.ZoneRepository zoneRepository) {
		this.documentProcessingService = documentProcessingService;
		this.zoneRepository = zoneRepository;
		initializeCredentials();
	}

	private static boolean credentialsInitialized = false;
	private static Path tempCredentialsFile = null;
	private static GoogleCredentials googleCredentials = null;

	private void initializeCredentials() {
		if (!enabled || credentialsInitialized) {
			return;
		}

		// PRIORITÉ 1 : Vérifier la variable d'environnement en premier (recommandé pour production)
		String envCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
		if (envCredentials != null && !envCredentials.isEmpty()) {
			Path envPath = Paths.get(envCredentials);
			if (Files.exists(envPath)) {
				try (InputStream credsStream = Files.newInputStream(envPath)) {
					googleCredentials = GoogleCredentials.fromStream(credsStream);
					googleCredentials = googleCredentials.createScoped(
							java.util.Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
					System.out.println("✓ Google Vision: Credentials chargés depuis variable d'environnement: " + envCredentials);
					credentialsInitialized = true;
					return;
				} catch (Exception e) {
					System.err.println("⚠ Erreur lors du chargement des credentials depuis GOOGLE_APPLICATION_CREDENTIALS: " + e.getMessage());
				}
			} else {
				System.err.println("⚠ ATTENTION: Le fichier spécifié dans GOOGLE_APPLICATION_CREDENTIALS n'existe pas: " + envCredentials);
			}
		}

		// PRIORITÉ 2 : Utiliser le fichier dans src/main/resources (fallback pour développement)
		if (credentialsPath != null && !credentialsPath.isEmpty()) {
			try {
				Path path = null;
				
				// Essayer d'abord comme chemin absolu
				Path absolutePath = Paths.get(credentialsPath);
				if (Files.exists(absolutePath)) {
					path = absolutePath;
					System.out.println("✓ Google Vision: Utilisation du fichier (chemin absolu): " + path);
				} else {
					// Essayer dans les resources (src/main/resources/)
					try {
						Resource resource = new ClassPathResource(credentialsPath);
						if (resource.exists()) {
							// Pour les fichiers dans le JAR, on doit les copier temporairement
							InputStream inputStream = resource.getInputStream();
							tempCredentialsFile = Files.createTempFile("google-credentials-", ".json");
							Files.copy(inputStream, tempCredentialsFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
							// Marquer le fichier pour suppression à la fin du programme
							tempCredentialsFile.toFile().deleteOnExit();
							path = tempCredentialsFile;
							System.out.println("✓ Google Vision: Fichier copié depuis resources vers: " + path.toAbsolutePath());
						}
					} catch (Exception e) {
						// Si on ne peut pas charger depuis resources, essayer à la racine du projet
						Path rootPath = Paths.get(System.getProperty("user.dir"), credentialsPath);
						if (Files.exists(rootPath)) {
							path = rootPath;
							System.out.println("✓ Google Vision: Utilisation du fichier (racine projet): " + path);
						}
					}
				}

				if (path != null && Files.exists(path)) {
					String absolutePathStr = path.toAbsolutePath().toString();
					
					// Charger les credentials directement depuis le fichier
					try (InputStream credsStream = Files.newInputStream(path)) {
						googleCredentials = GoogleCredentials.fromStream(credsStream);
						// Créer un scope pour Vision API
						googleCredentials = googleCredentials.createScoped(
								java.util.Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
					}
					
					System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", absolutePathStr);
					credentialsInitialized = true;
					System.out.println("✓ Google Vision: Credentials chargés depuis: " + absolutePathStr);
				} else {
					System.err.println("⚠ ATTENTION: Fichier de credentials Google Vision non trouvé: " + credentialsPath);
					System.err.println("   Vérifiez que le fichier existe dans src/main/resources/ ou définissez GOOGLE_APPLICATION_CREDENTIALS");
				}
			} catch (Exception e) {
				System.err.println("✗ Erreur lors de la configuration des credentials Google Vision: " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			// Aucun chemin configuré, on espère que GOOGLE_APPLICATION_CREDENTIALS est défini
			if (envCredentials == null || envCredentials.isEmpty()) {
				System.err.println("⚠ ATTENTION: Google Vision est activé mais aucun credentials n'est configuré.");
				System.err.println("   Options:");
				System.err.println("   1. Définir la variable d'environnement GOOGLE_APPLICATION_CREDENTIALS");
				System.err.println("   2. Configurer google.vision.credentials.path dans application.properties");
			}
		}
	}

	private void ensureCredentialsInitialized() {
		if (!credentialsInitialized) {
			initializeCredentials();
		}
		
		// Vérifier que les credentials sont disponibles
		if (googleCredentials == null) {
			String credsPath = System.getProperty("GOOGLE_APPLICATION_CREDENTIALS");
			if (credsPath == null || credsPath.isEmpty()) {
				credsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
			}
			
			if (credsPath == null || credsPath.isEmpty() || !Files.exists(Paths.get(credsPath))) {
				throw new IllegalStateException(
					"Les credentials Google Vision ne sont pas disponibles. " +
					"Vérifiez que le fichier existe dans src/main/resources/" + credentialsPath + 
					" ou définissez la variable d'environnement GOOGLE_APPLICATION_CREDENTIALS"
				);
			}
		}
	}

	@Override
	public String extractTextFromImage(byte[] imageBytes) throws Exception {
		if (!enabled) {
			throw new IllegalStateException("Google Vision API n'est pas activé. " +
					"Veuillez configurer google.vision.enabled=true dans application.properties");
		}

		// S'assurer que les credentials sont initialisés avant de créer le client
		ensureCredentialsInitialized();

		// Créer le client avec les credentials explicites
		ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
				.setCredentialsProvider(() -> googleCredentials)
				.build();

		try (ImageAnnotatorClient vision = ImageAnnotatorClient.create(settings)) {
			ByteString imgBytes = ByteString.copyFrom(imageBytes);
			Image img = Image.newBuilder().setContent(imgBytes).build();
			
			Feature feat = Feature.newBuilder()
					.setType(Feature.Type.DOCUMENT_TEXT_DETECTION) // Meilleur pour les documents
					.build();
			
			AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
					.addFeatures(feat)
					.setImage(img)
					.build();
			
			BatchAnnotateImagesResponse response = vision.batchAnnotateImages(
					java.util.Arrays.asList(request));
			
			List<AnnotateImageResponse> responses = response.getResponsesList();
			if (responses.isEmpty()) {
				throw new IOException("Aucune réponse de Google Vision API");
			}
			
			AnnotateImageResponse annotateResponse = responses.get(0);
			if (annotateResponse.hasError()) {
				throw new IOException("Erreur Google Vision: " + annotateResponse.getError().getMessage());
			}
			
			// Extraire le texte complet du document
			TextAnnotation textAnnotation = annotateResponse.getFullTextAnnotation();
			if (textAnnotation != null) {
				return textAnnotation.getText();
			}
			
			// Fallback : utiliser les annotations de texte individuelles
			if (!annotateResponse.getTextAnnotationsList().isEmpty()) {
				return annotateResponse.getTextAnnotationsList().get(0).getDescription();
			}
			
			return "";
		} catch (Exception e) {
			throw new IOException("Erreur lors de l'extraction OCR avec Google Vision: " + e.getMessage(), e);
		}
	}

	@Override
	public List<ExtractedPharmacyDto> extractPharmaciesFromImage(byte[] imageBytes) throws Exception {
		// Extraire le texte avec Google Vision
		String extractedText = extractTextFromImage(imageBytes);
		
		// Parser directement le texte extrait (sans passer par le service de document)
		// On utilise la méthode d'extraction du DocumentProcessingService mais avec le texte brut
		return parseTextToPharmacies(extractedText);
	}

	/**
	 * Parse le texte brut pour extraire les pharmacies
	 * Format attendu : PHCIE [nom] / [pharmacien] - [téléphones] / [description]
	 */
	private List<ExtractedPharmacyDto> parseTextToPharmacies(String text) {
		List<ExtractedPharmacyDto> pharmacies = new ArrayList<>();
		
		// Pattern pour détecter une zone suivie de PHCIE sur la même ligne : ZONE PHCIE [nom] ...
		// Exemple: "ABENGOUROU PHCIE DU MARCHE / ..."
		java.util.regex.Pattern ZONE_PHARMACY_PATTERN = java.util.regex.Pattern.compile(
				"^([A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸ][A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸ\\s-]+?)\\s+PHCIE\\s+(.+?)(?:\\s*/\\s*([^-]+?))?(?:\\s*-\\s*([^/]+?))?(?:\\s*/\\s*(.+))?$",
				java.util.regex.Pattern.MULTILINE | java.util.regex.Pattern.CASE_INSENSITIVE);

		// Pattern pour détecter une ligne de pharmacie complète : PHCIE [nom] / [pharmacien] - [téléphones] / [description]
		// Format flexible : PHCIE peut être sur une ligne, puis les infos sur les lignes suivantes
		java.util.regex.Pattern PHARMACY_LINE_PATTERN = java.util.regex.Pattern.compile(
				"(?i)phcie\\s+([^/]+?)(?:\\s*/\\s*([^-]+?))?(?:\\s*-\\s*([^/]+?))?(?:\\s*/\\s*(.+))?$",
				java.util.regex.Pattern.MULTILINE);

		// Pattern pour détecter juste PHCIE + nom (sans les autres infos sur la même ligne)
		java.util.regex.Pattern PHARMACY_NAME_PATTERN = java.util.regex.Pattern.compile(
				"(?i)phcie\\s+(.+?)(?:\\s*/|\\s*-|$)",
				java.util.regex.Pattern.MULTILINE);

		// Parser le texte ligne par ligne
		String[] lines = text.split("\\r?\\n");
		String currentZone = null;
		ExtractedPharmacyDto currentPharmacy = null;

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();

			if (line.isEmpty()) {
				// Après un saut de ligne, la prochaine ligne non vide peut être une zone
				// On vérifiera à la prochaine itération
				continue;
			}

			// PRIORITÉ 1 : Détecter une zone suivie de PHCIE sur la même ligne
			// Exemple: "ABENGOUROU PHCIE DU MARCHE / ..."
			java.util.regex.Matcher zonePharmacyMatcher = ZONE_PHARMACY_PATTERN.matcher(line);
			if (zonePharmacyMatcher.find()) {
				// Finaliser la pharmacie précédente si elle existe
				if (currentPharmacy != null) {
					pharmacies.add(currentPharmacy);
				}

					// La zone est dans le premier groupe - normaliser (majuscules sans accents)
					String detectedZone = normalizeZoneName(zonePharmacyMatcher.group(1).trim());
					if (!detectedZone.equals(currentZone)) {
						currentZone = detectedZone;
					}

				// Créer la nouvelle pharmacie
				currentPharmacy = new ExtractedPharmacyDto();
				currentPharmacy.setZone(currentZone);
				currentPharmacy.setPharmacyName(cleanPharmacyName(zonePharmacyMatcher.group(2)));
				
				if (zonePharmacyMatcher.group(3) != null) {
					currentPharmacy.setPharmacistName(zonePharmacyMatcher.group(3).trim());
				}
				if (zonePharmacyMatcher.group(4) != null) {
					currentPharmacy.setPhoneNumbers(zonePharmacyMatcher.group(4).trim());
				}
				if (zonePharmacyMatcher.group(5) != null) {
					currentPharmacy.setLocation(zonePharmacyMatcher.group(5).trim());
				}
				currentPharmacy.setConfidence(0.9);
				continue;
			}

			// PRIORITÉ 2 : Détecter une zone seule (structure tableau : zone dans une colonne)
			// Une zone seule est :
			// - En majuscules
			// - Ne contient pas PHCIE
			// - Est suivie par des lignes contenant PHCIE
			boolean isAfterEmptyLine = (i == 0) || (i > 0 && lines[i - 1].trim().isEmpty());
			boolean isPotentialZone = isZoneLine(line);
			boolean lineHasPharmacy = line.toUpperCase().contains("PHCIE");
			
			// Si c'est une zone potentielle (pas de PHCIE sur la ligne)
			if (isPotentialZone && !lineHasPharmacy) {
				// Vérifier si les lignes suivantes contiennent PHCIE (structure tableau)
				boolean nextLinesHavePharmacy = false;
				for (int j = i + 1; j < Math.min(i + 5, lines.length); j++) {
					String nextLine = lines[j].trim();
					if (!nextLine.isEmpty() && nextLine.toUpperCase().contains("PHCIE")) {
						nextLinesHavePharmacy = true;
						break;
					}
					// Si on rencontre une autre zone avant une pharmacie, arrêter
					if (!nextLine.isEmpty() && isZoneLine(nextLine) && !nextLine.toUpperCase().contains("PHCIE")) {
						break;
					}
				}
				
				// Si c'est une zone valide (après ligne vide OU suivie de pharmacies)
				if (isAfterEmptyLine || nextLinesHavePharmacy) {
					// Normaliser la zone (majuscules sans accents)
					String detectedZone = normalizeZoneName(line.trim());
					// Ne mettre à jour la zone que si elle est différente de la zone actuelle
					if (!detectedZone.equals(currentZone)) {
						currentZone = detectedZone;
						// Finaliser la pharmacie précédente si elle existe
						if (currentPharmacy != null) {
							pharmacies.add(currentPharmacy);
							currentPharmacy = null;
						}
					}
					continue;
				}
			}

			// PRIORITÉ 3 : Essayer de détecter une ligne complète avec toutes les infos (sans zone)
			java.util.regex.Matcher fullLineMatcher = PHARMACY_LINE_PATTERN.matcher(line);
			if (fullLineMatcher.find()) {
				// Finaliser la pharmacie précédente si elle existe
				if (currentPharmacy != null) {
					pharmacies.add(currentPharmacy);
				}

				currentPharmacy = new ExtractedPharmacyDto();
				currentPharmacy.setZone(currentZone);
				currentPharmacy.setPharmacyName(cleanPharmacyName(fullLineMatcher.group(1)));
				
				if (fullLineMatcher.group(2) != null) {
					currentPharmacy.setPharmacistName(fullLineMatcher.group(2).trim());
				}
				if (fullLineMatcher.group(3) != null) {
					currentPharmacy.setPhoneNumbers(fullLineMatcher.group(3).trim());
				}
				if (fullLineMatcher.group(4) != null) {
					currentPharmacy.setLocation(fullLineMatcher.group(4).trim());
				}
				currentPharmacy.setConfidence(0.9);
				continue;
			}

			// Détecter juste PHCIE + nom (les autres infos peuvent être sur les lignes suivantes)
			java.util.regex.Matcher pharmacyNameMatcher = PHARMACY_NAME_PATTERN.matcher(line);
			if (pharmacyNameMatcher.find()) {
				// Finaliser la pharmacie précédente si elle existe
				if (currentPharmacy != null) {
					pharmacies.add(currentPharmacy);
				}

				currentPharmacy = new ExtractedPharmacyDto();
				currentPharmacy.setZone(currentZone);
				
				// Extraire le nom complet : PHCIE + nom
				String fullPharmacyLine = line.trim();
				// Extraire tout ce qui suit "PHCIE " jusqu'à "/" ou "-" ou fin de ligne
				java.util.regex.Pattern fullPharmacyPattern = java.util.regex.Pattern.compile(
						"(?i)phcie\\s+(.+?)(?:\\s*/|\\s*-|$)", java.util.regex.Pattern.MULTILINE);
				java.util.regex.Matcher fullMatcher = fullPharmacyPattern.matcher(fullPharmacyLine);
				if (fullMatcher.find()) {
					String pharmacyNamePart = fullMatcher.group(1).trim();
					
					// Vérifier si la ligne contient aussi d'autres infos après le nom
					if (pharmacyNamePart.contains("/")) {
						String[] parts = pharmacyNamePart.split("/", 2);
						currentPharmacy.setPharmacyName(cleanPharmacyName(parts[0]));
						if (parts.length > 1) {
							String rest = parts[1].trim();
							// Vérifier si c'est un pharmacien ou autre chose
							if (rest.contains("-")) {
								String[] restParts = rest.split("-", 2);
								currentPharmacy.setPharmacistName(restParts[0].trim());
								if (restParts.length > 1) {
									String afterDash = restParts[1].trim();
									if (afterDash.contains("/")) {
										String[] afterDashParts = afterDash.split("/", 2);
										currentPharmacy.setPhoneNumbers(afterDashParts[0].trim());
										if (afterDashParts.length > 1) {
											currentPharmacy.setLocation(afterDashParts[1].trim());
										}
									} else {
										currentPharmacy.setPhoneNumbers(afterDash);
									}
								}
							} else {
								currentPharmacy.setPharmacistName(rest);
							}
						}
					} else {
						currentPharmacy.setPharmacyName(cleanPharmacyName(pharmacyNamePart));
					}
				} else {
					// Fallback : utiliser le pattern original
					String pharmacyNamePart = pharmacyNameMatcher.group(1).trim();
					currentPharmacy.setPharmacyName(cleanPharmacyName(pharmacyNamePart));
				}
				currentPharmacy.setConfidence(0.9);
				continue;
			}

			// Si on a une pharmacie en cours, chercher les autres informations sur les lignes suivantes
			if (currentPharmacy != null) {
				// Si la ligne commence par "/", c'est probablement le pharmacien
				if (line.startsWith("/") && currentPharmacy.getPharmacistName() == null) {
					String rest = line.substring(1).trim();
					if (rest.contains("-")) {
						String[] parts = rest.split("-", 2);
						currentPharmacy.setPharmacistName(parts[0].trim());
						if (parts.length > 1) {
							String afterDash = parts[1].trim();
							if (afterDash.contains("/")) {
								String[] afterDashParts = afterDash.split("/", 2);
								currentPharmacy.setPhoneNumbers(afterDashParts[0].trim());
								if (afterDashParts.length > 1) {
									currentPharmacy.setLocation(afterDashParts[1].trim());
								}
							} else {
								currentPharmacy.setPhoneNumbers(afterDash);
							}
						}
						continue;
					} else {
						currentPharmacy.setPharmacistName(rest);
						continue;
					}
				}

				// Si la ligne commence par "-", c'est probablement les téléphones
				if (line.startsWith("-") && currentPharmacy.getPhoneNumbers() == null) {
					String rest = line.substring(1).trim();
					if (rest.contains("/")) {
						String[] parts = rest.split("/", 2);
						currentPharmacy.setPhoneNumbers(parts[0].trim());
						if (parts.length > 1) {
							currentPharmacy.setLocation(parts[1].trim());
						}
					} else {
						currentPharmacy.setPhoneNumbers(rest);
					}
					continue;
				}

				// Si la ligne commence par "/" et qu'on a déjà le pharmacien, c'est probablement la description
				if (line.startsWith("/") && currentPharmacy.getPharmacistName() != null && currentPharmacy.getLocation() == null) {
					currentPharmacy.setLocation(line.substring(1).trim());
					continue;
				}

				// Détecter les numéros de téléphone (format: séquence de chiffres avec espaces/slash/tirets)
				java.util.regex.Pattern phonePattern = java.util.regex.Pattern.compile(
						"([0-9]{2}\\s*[0-9]{2}\\s*[0-9]{2}\\s*[0-9]{2}(?:\\s*[/-]\\s*[0-9]{2}\\s*[0-9]{2}\\s*[0-9]{2}\\s*[0-9]{2})*)");
				java.util.regex.Matcher phoneMatcher = phonePattern.matcher(line);
				if (phoneMatcher.find() && currentPharmacy.getPhoneNumbers() == null) {
					currentPharmacy.setPhoneNumbers(phoneMatcher.group(1).trim());
					// Si la ligne contient aussi "/" après les téléphones, c'est la description
					int slashIndex = line.indexOf("/", phoneMatcher.end());
					if (slashIndex > 0) {
						currentPharmacy.setLocation(line.substring(slashIndex + 1).trim());
					}
					continue;
				}

				// Si on rencontre une nouvelle zone ou une nouvelle pharmacie, finaliser
				if (i < lines.length - 1 && 
				    (isZoneLine(lines[i + 1]) || PHARMACY_NAME_PATTERN.matcher(lines[i + 1]).find())) {
					pharmacies.add(currentPharmacy);
					currentPharmacy = null;
				}
			}
		}

		// Ajouter la dernière pharmacie
		if (currentPharmacy != null) {
			pharmacies.add(currentPharmacy);
		}

		return pharmacies;
	}

	private String cleanPharmacyName(String name) {
		// Garder PHCIE dans le nom de la pharmacie, juste nettoyer les espaces
		String cleaned = name.trim();
		// S'assurer que PHCIE est présent et en majuscules
		if (!cleaned.toUpperCase().startsWith("PHCIE")) {
			cleaned = "PHCIE " + cleaned;
		} else {
			// Normaliser : PHCIE en majuscules
			cleaned = cleaned.replaceAll("(?i)^phcie", "PHCIE");
		}
		return cleaned;
	}

	/**
	 * Normalise le nom d'une zone : majuscules sans accents
	 * Exemple: "Abengourou" -> "ABENGOUROU", "Adiaké" -> "ADIAKE"
	 */
	private String normalizeZoneName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return name;
		}
		
		// Convertir en majuscules
		String upper = name.trim().toUpperCase();
		
		// Remplacer les caractères accentués par leurs équivalents sans accent
		upper = upper.replace("À", "A")
					.replace("Á", "A")
					.replace("Â", "A")
					.replace("Ã", "A")
					.replace("Ä", "A")
					.replace("Å", "A")
					.replace("Æ", "AE")
					.replace("Ç", "C")
					.replace("È", "E")
					.replace("É", "E")
					.replace("Ê", "E")
					.replace("Ë", "E")
					.replace("Ì", "I")
					.replace("Í", "I")
					.replace("Î", "I")
					.replace("Ï", "I")
					.replace("Ð", "D")
					.replace("Ñ", "N")
					.replace("Ò", "O")
					.replace("Ó", "O")
					.replace("Ô", "O")
					.replace("Õ", "O")
					.replace("Ö", "O")
					.replace("Ø", "O")
					.replace("Ù", "U")
					.replace("Ú", "U")
					.replace("Û", "U")
					.replace("Ü", "U")
					.replace("Ý", "Y")
					.replace("Þ", "TH")
					.replace("Ÿ", "Y");
		
		return upper;
	}

	private boolean isZoneLine(String line) {
		// Une zone est généralement :
		// - En majuscules
		// - Fait au moins 3 caractères
		// - Ne contient pas de mots-clés de pharmacie, téléphone, ou mots spéciaux
		// - N'est pas "PERMANENCE 24H/24H" ou similaire
		// - Ne contient pas de chiffres (sauf exceptions comme années)
		// - Généralement un seul mot ou quelques mots courts (nom de ville)
		
		if (line.length() < 3) {
			return false;
		}
		
		// Exclure les lignes qui contiennent des mots-clés indésirables
		String upperLine = line.toUpperCase();
		if (upperLine.matches(".*PHCIE.*|.*PHARMACIE.*|.*TEL.*|.*TELEPHONE.*|.*PERMANENCE.*|.*24H.*")) {
			return false;
		}
		
		// Exclure les lignes qui sont clairement des descriptions (trop longues ou avec caractères spéciaux)
		if (line.length() > 50) {
			return false;
		}
		
		// Exclure les lignes avec beaucoup de chiffres (probablement des téléphones ou dates)
		if (line.matches(".*\\d{4}.*\\d{2}.*") || line.matches(".*\\d{2}\\s*\\d{2}\\s*\\d{2}.*")) {
			return false;
		}
		
		// Une zone est généralement en majuscules et ne contient que des lettres, espaces et tirets
		if (!line.equals(upperLine)) {
			return false;
		}
		
		// Vérifier que c'est principalement des lettres (pas trop de chiffres)
		long letterCount = line.chars().filter(Character::isLetter).count();
		long digitCount = line.chars().filter(Character::isDigit).count();
		if (digitCount > letterCount / 2) {
			return false;
		}
		
		// Une zone est généralement un nom de ville (1-4 mots)
		String[] words = line.split("\\s+");
		if (words.length > 4) {
			return false;
		}
		
		// VALIDATION : Vérifier si la ligne correspond à une zone existante dans la base de données
		// Cela permet de valider que c'est bien une zone (ville/commune) et non une autre information
		if (zoneRepository != null) {
			try {
				// Chercher si cette ligne correspond à une zone active dans la base
				java.util.Optional<com.buddies.pharmaciegarde.modules.common.model.Zone> zoneOpt = 
						zoneRepository.findActiveZoneByNameIgnoreCase(line.trim());
				
				// Si on trouve une zone avec ce nom, c'est probablement une zone valide
				if (zoneOpt.isPresent()) {
					return true;
				}
				
				// Aussi vérifier par correspondance partielle (insensible à la casse)
				List<com.buddies.pharmaciegarde.modules.common.model.Zone> zones = 
						zoneRepository.findActiveZonesByNameStartingWithIgnoreCase(line.trim());
				if (!zones.isEmpty()) {
					return true;
				}
			} catch (Exception e) {
				// En cas d'erreur (ex: base non disponible), on continue avec la validation basique
				// Ne pas bloquer le traitement
			}
		}
		
		// Si la validation par la base de données n'a pas confirmé, on utilise les règles de base
		// Cela permet de détecter de nouvelles villes qui ne sont pas encore dans la base
		return true;
	}
}

