package com.buddies.pharmaciegarde.modules.duty.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto;

@Service
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

	private final com.buddies.pharmaciegarde.modules.common.repository.ZoneRepository zoneRepository;

	public DocumentProcessingServiceImpl(
			com.buddies.pharmaciegarde.modules.common.repository.ZoneRepository zoneRepository) {
		this.zoneRepository = zoneRepository;
	}

	// Patterns pour extraire les informations
	private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
			"(?i)(?:semaine|week)\\s+du\\s+(\\d{1,2})\\s+(\\w+)\\s+au\\s+(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern ZONE_PATTERN = Pattern.compile(
			"^([A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸ][A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸ\\s-]+)$",
			Pattern.MULTILINE);

	private static final Pattern PHARMACY_PATTERN = Pattern.compile(
			"(?i)(?:phcie|pharmacie|pharma)\\s+([A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸ][A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸ\\s'-]+)",
			Pattern.MULTILINE);

	private static final Pattern PHARMACIST_PATTERN = Pattern.compile(
			"(?i)(?:mme|m\\.|mr|monsieur|madame)\\s+([A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸ][A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸ\\s\\.'-]+)",
			Pattern.MULTILINE);

	private static final Pattern PHONE_PATTERN = Pattern.compile(
			"(?i)(?:tel\\.?|telephone|phone)\\s*:?\\s*([0-9\\s\\/\\-]+)",
			Pattern.MULTILINE);

	@Override
	public List<ExtractedPharmacyDto> extractPharmacies(InputStream inputStream, String fileName) throws Exception {
		byte[] bytes = inputStream.readAllBytes();
		
		// Détecter le type de fichier
		if (fileName.toLowerCase().endsWith(".pdf")) {
			return extractFromPdf(new ByteArrayInputStream(bytes));
		} else if (isImageFile(fileName)) {
			// Pour les images, on utilisera OCR (à implémenter)
			// Pour l'instant, on retourne une liste vide
			throw new UnsupportedOperationException("Le traitement des images nécessite OCR. Utilisez Google Vision API.");
		}
		
		throw new IllegalArgumentException("Type de fichier non supporté: " + fileName);
	}

	@Override
	public boolean requiresOCR(InputStream inputStream, String fileName) throws Exception {
		if (isImageFile(fileName)) {
			return true;
		}
		
		if (fileName.toLowerCase().endsWith(".pdf")) {
			// Vérifier si le PDF contient du texte extractible
			try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
				PDFTextStripper stripper = new PDFTextStripper();
				String text = stripper.getText(document);
				// Si le texte est très court ou vide, c'est probablement un PDF scanné
				return text.trim().length() < 100;
			}
		}
		
		return false;
	}

	private List<ExtractedPharmacyDto> extractFromPdf(InputStream inputStream) throws IOException {
		List<ExtractedPharmacyDto> pharmacies = new ArrayList<>();
		
		try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(document);
			
			// Extraire la période de garde
			LocalDate[] dateRange = extractDateRange(text);
			
			// Pattern pour détecter une zone suivie de PHCIE sur la même ligne : ZONE PHCIE [nom] ...
			// Exemple: "ABENGOUROU PHCIE DU MARCHE / ..."
			Pattern ZONE_PHARMACY_PATTERN = Pattern.compile(
					"^([A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸ][A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸ\\s-]+?)\\s+PHCIE\\s+(.+?)(?:\\s*/\\s*([^-]+?))?(?:\\s*-\\s*([^/]+?))?(?:\\s*/\\s*(.+))?$",
					Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

			// Pattern pour détecter une ligne de pharmacie complète : PHCIE [nom] / [pharmacien] - [téléphones] / [description]
			Pattern PHARMACY_LINE_PATTERN = Pattern.compile(
					"(?i)phcie\\s+([^/]+?)(?:\\s*/\\s*([^-]+?))?(?:\\s*-\\s*([^/]+?))?(?:\\s*/\\s*(.+))?$",
					Pattern.MULTILINE);

			Pattern PHARMACY_NAME_PATTERN = Pattern.compile(
					"(?i)phcie\\s+(.+?)(?:\\s*/|\\s*-|$)",
					Pattern.MULTILINE);
			
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
				Matcher zonePharmacyMatcher = ZONE_PHARMACY_PATTERN.matcher(line);
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
				
				// Essayer de détecter une ligne complète avec toutes les infos
				Matcher fullLineMatcher = PHARMACY_LINE_PATTERN.matcher(line);
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
				
				// Détecter juste PHCIE + nom
				Matcher pharmacyNameMatcher = PHARMACY_NAME_PATTERN.matcher(line);
				if (pharmacyNameMatcher.find()) {
					// Finaliser la pharmacie précédente si elle existe
					if (currentPharmacy != null) {
						pharmacies.add(currentPharmacy);
					}
					
					currentPharmacy = new ExtractedPharmacyDto();
					currentPharmacy.setZone(currentZone);
					String pharmacyNamePart = pharmacyNameMatcher.group(1).trim();
					// Vérifier si la ligne contient aussi d'autres infos après le nom
					if (pharmacyNamePart.contains("/")) {
						String[] parts = pharmacyNamePart.split("/", 2);
						currentPharmacy.setPharmacyName(cleanPharmacyName(parts[0]));
						if (parts.length > 1) {
							String rest = parts[1].trim();
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
					
					// Détecter les numéros de téléphone (format: séquence de chiffres avec espaces/slash/tirets)
					Pattern phonePattern = Pattern.compile(
							"([0-9]{2}\\s*[0-9]{2}\\s*[0-9]{2}\\s*[0-9]{2}(?:\\s*[/-]\\s*[0-9]{2}\\s*[0-9]{2}\\s*[0-9]{2}\\s*[0-9]{2})*)");
					Matcher phoneMatcher = phonePattern.matcher(line);
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

	private LocalDate[] extractDateRange(String text) {
		Matcher matcher = DATE_RANGE_PATTERN.matcher(text);
		if (matcher.find()) {
			try {
				int startDay = Integer.parseInt(matcher.group(1));
				String startMonth = matcher.group(2);
				int endDay = Integer.parseInt(matcher.group(3));
				String endMonth = matcher.group(4);
				int year = Integer.parseInt(matcher.group(5));
				
				// Convertir les mois français en numéros (simplifié)
				LocalDate startDate = parseDate(startDay, startMonth, year);
				LocalDate endDate = parseDate(endDay, endMonth, year);
				
				return new LocalDate[]{startDate, endDate};
			} catch (Exception e) {
				// Ignorer les erreurs de parsing de date
			}
		}
		return null;
	}

	private LocalDate parseDate(int day, String monthName, int year) {
		// Mapping simplifié des mois français
		String[] months = {"janvier", "fevrier", "mars", "avril", "mai", "juin",
		                   "juillet", "aout", "septembre", "octobre", "novembre", "decembre"};
		
		String monthLower = monthName.toLowerCase();
		int month = 1;
		for (int i = 0; i < months.length; i++) {
			if (monthLower.contains(months[i])) {
				month = i + 1;
				break;
			}
		}
		
		return LocalDate.of(year, month, day);
	}

	private boolean isImageFile(String fileName) {
		if (fileName == null) {
			return false;
		}
		String lower = fileName.toLowerCase();
		return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
		       lower.endsWith(".png") || lower.endsWith(".gif") || 
		       lower.endsWith(".bmp") || lower.endsWith(".tiff");
	}
}

