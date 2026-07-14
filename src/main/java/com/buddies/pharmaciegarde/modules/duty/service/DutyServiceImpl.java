package com.buddies.pharmaciegarde.modules.duty.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyCreateDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyFileResponseDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyReportDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyResponseDto;
import com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto;
import com.buddies.pharmaciegarde.modules.duty.dto.PdfProcessingResultDto;
import com.buddies.pharmaciegarde.modules.duty.dto.TextExtractionResultDto;
import com.buddies.pharmaciegarde.modules.duty.model.Duty;
import com.buddies.pharmaciegarde.modules.duty.model.DutyFile;
import com.buddies.pharmaciegarde.modules.duty.model.DutyFile.ProcessingStatus;
import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;
import com.buddies.pharmaciegarde.modules.duty.model.DutyStatus;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyFileRepository;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyPeriodRepository;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyRepository;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyRepository;
import com.buddies.pharmaciegarde.modules.user.model.Client;
import com.buddies.pharmaciegarde.modules.user.repository.ClientRepository;

@Service
@Transactional
public class DutyServiceImpl implements DutyService {

	private static final Logger logger = LoggerFactory.getLogger(DutyServiceImpl.class);

	private final DutyRepository dutyRepository;
	private final DutyFileRepository dutyFileRepository;
	private final DutyPeriodRepository dutyPeriodRepository;
	private final PharmacyRepository pharmacyRepository;
	private final ClientRepository clientRepository;
	private final PDFProcessingService pdfProcessingService; // Ancien service (déprécié)
	private final DocumentProcessingService documentProcessingService; // Nouveau service
	private final GoogleVisionService googleVisionService; // Service OCR optionnel

	@Value("${file.upload-dir:uploads/duty-files}")
	private String uploadDir;

	@Value("${file.extraction-dir:uploads/extractions}")
	private String extractionDir;

	private final ObjectMapper objectMapper;

	public DutyServiceImpl(DutyRepository dutyRepository, DutyFileRepository dutyFileRepository,
			DutyPeriodRepository dutyPeriodRepository, PharmacyRepository pharmacyRepository,
			ClientRepository clientRepository,
			PDFProcessingService pdfProcessingService, DocumentProcessingService documentProcessingService, 
			GoogleVisionService googleVisionService) {
		this.dutyRepository = dutyRepository;
		this.dutyFileRepository = dutyFileRepository;
		this.dutyPeriodRepository = dutyPeriodRepository;
		this.pharmacyRepository = pharmacyRepository;
		this.clientRepository = clientRepository;
		this.pdfProcessingService = pdfProcessingService;
		this.documentProcessingService = documentProcessingService;
		this.googleVisionService = googleVisionService;
		
		// Configurer ObjectMapper pour formater le JSON de manière lisible
		this.objectMapper = new ObjectMapper();
		this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	@Override
	public DutyResponseDto create(DutyCreateDto dutyCreateDto) {
		Pharmacy pharmacy = pharmacyRepository.findById(dutyCreateDto.getPharmacyId())
				.orElseThrow(() -> new ResourceNotFoundException("Pharmacie", dutyCreateDto.getPharmacyId()));

		DutyPeriod dutyPeriod = dutyPeriodRepository.findById(dutyCreateDto.getDutyPeriodId())
				.orElseThrow(() -> new ResourceNotFoundException("Période de garde", dutyCreateDto.getDutyPeriodId()));

		// Vérifier si une garde existe déjà pour cette pharmacie et cette période
		List<Duty> existingDuties = dutyRepository.findByPharmacyIdAndDutyPeriodId(
				dutyCreateDto.getPharmacyId(), dutyCreateDto.getDutyPeriodId());
		if (!existingDuties.isEmpty()) {
			throw new IllegalArgumentException("Une garde existe déjà pour cette pharmacie sur cette période");
		}

		Duty duty = new Duty();
		duty.setPharmacy(pharmacy);
		duty.setDutyPeriod(dutyPeriod);

		if (dutyCreateDto.getDutyFileId() != null) {
			DutyFile dutyFile = dutyFileRepository.findById(dutyCreateDto.getDutyFileId())
					.orElseThrow(() -> new ResourceNotFoundException("Fichier de garde", dutyCreateDto.getDutyFileId()));
			duty.setDutyFile(dutyFile);
		}

		Duty savedDuty = dutyRepository.save(duty);
		return mapToResponseDto(savedDuty);
	}

	@Override
	@Transactional(readOnly = true)
	public DutyResponseDto getById(Long id) {
		Duty duty = dutyRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Garde", id));
		return mapToResponseDto(duty);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DutyResponseDto> getAll() {
		return dutyRepository.findAll().stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<DutyResponseDto> getByPharmacyId(Long pharmacyId) {
		return dutyRepository.findByPharmacyId(pharmacyId).stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<DutyResponseDto> getPharmacyHistory(Long pharmacyId) {
		// Vérifier que la pharmacie existe
		if (!pharmacyRepository.existsById(pharmacyId)) {
			throw new ResourceNotFoundException("Pharmacie", pharmacyId);
		}
		// Retourner toutes les gardes de la pharmacie triées par date de début (plus récentes en premier)
		return dutyRepository.findByPharmacyId(pharmacyId).stream()
				.sorted((d1, d2) -> d2.getDutyPeriod().getStartDate().compareTo(d1.getDutyPeriod().getStartDate()))
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<DutyResponseDto> getByDateRange(LocalDate startDate, LocalDate endDate) {
		// Trouver les périodes qui chevauchent la plage de dates
		List<DutyPeriod> periods = dutyPeriodRepository.findOverlappingPeriods(startDate, endDate);
		return dutyRepository.findByDutyPeriodIdIn(periods.stream().map(DutyPeriod::getId).collect(Collectors.toList())).stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	public DutyResponseDto update(Long id, DutyCreateDto dutyCreateDto) {
		Duty duty = dutyRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Garde", id));

		Pharmacy pharmacy = pharmacyRepository.findById(dutyCreateDto.getPharmacyId())
				.orElseThrow(() -> new ResourceNotFoundException("Pharmacie", dutyCreateDto.getPharmacyId()));

		DutyPeriod dutyPeriod = dutyPeriodRepository.findById(dutyCreateDto.getDutyPeriodId())
				.orElseThrow(() -> new ResourceNotFoundException("Période de garde", dutyCreateDto.getDutyPeriodId()));

		// Vérifier les chevauchements (exclure la garde actuelle)
		List<Duty> existingDuties = dutyRepository.findByPharmacyIdAndDutyPeriodId(
				dutyCreateDto.getPharmacyId(), dutyCreateDto.getDutyPeriodId());
		existingDuties.removeIf(d -> d.getId().equals(id));
		if (!existingDuties.isEmpty()) {
			throw new IllegalArgumentException("Une garde existe déjà pour cette pharmacie sur cette période");
		}

		duty.setPharmacy(pharmacy);
		duty.setDutyPeriod(dutyPeriod);

		Duty updatedDuty = dutyRepository.save(duty);
		return mapToResponseDto(updatedDuty);
	}

	@Override
	public void delete(Long id) {
		if (!dutyRepository.existsById(id)) {
			throw new ResourceNotFoundException("Garde", id);
		}
		dutyRepository.deleteById(id);
	}

	@Override
	public DutyFileResponseDto uploadPdfFile(MultipartFile file, String description, Long dutyPeriodId) {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("Le fichier est vide");
		}

		String contentType = file.getContentType();
		boolean isPdf = contentType != null && contentType.equals("application/pdf");
		boolean isJpeg = contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/jpg"));
		
		if (!isPdf && !isJpeg) {
			throw new IllegalArgumentException("Le fichier doit être un PDF ou une image JPEG. Type reçu: " + contentType);
		}

		try {
			// Créer le répertoire s'il n'existe pas
			Path uploadPath = Paths.get(uploadDir);
			if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}

			// Générer un nom de fichier unique
			String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
			Path filePath = uploadPath.resolve(fileName);

			// Sauvegarder le fichier
			Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

			// Créer l'entrée dans la base de données
			DutyFile dutyFile = new DutyFile();
			dutyFile.setFileName(file.getOriginalFilename());
			dutyFile.setFilePath(filePath.toString());
			dutyFile.setFileSize(file.getSize());
			dutyFile.setStatus(ProcessingStatus.PENDING);
			// Sauvegarder la description si fournie
			if (description != null && !description.trim().isEmpty()) {
				dutyFile.setDescription(description.trim());
			}
			// Lier à la période de garde si fournie
			if (dutyPeriodId != null) {
				DutyPeriod dutyPeriod = dutyPeriodRepository.findById(dutyPeriodId)
						.orElseThrow(() -> new ResourceNotFoundException("Période de garde", dutyPeriodId));
				dutyFile.setDutyPeriod(dutyPeriod);
			}

			DutyFile savedFile = dutyFileRepository.save(dutyFile);
			return mapToFileResponseDto(savedFile);
		} catch (IOException e) {
			throw new RuntimeException("Erreur lors de l'upload du fichier: " + e.getMessage(), e);
		}
	}

	@Override
	public List<DutyFileResponseDto> uploadPdfFiles(MultipartFile[] files, String description, Long dutyPeriodId) {
		if (files == null || files.length == 0) {
			throw new IllegalArgumentException("Aucun fichier fourni");
		}

		List<DutyFileResponseDto> uploadedFiles = new ArrayList<>();

		for (MultipartFile file : files) {
			if (file.isEmpty()) {
				logger.warn("Un fichier vide a été ignoré: {}", file.getOriginalFilename());
				continue;
			}

			try {
				DutyFileResponseDto uploadedFile = uploadPdfFile(file, description, dutyPeriodId);
				uploadedFiles.add(uploadedFile);
			} catch (Exception e) {
				logger.error("Erreur lors de l'upload du fichier {}: {}", file.getOriginalFilename(), e.getMessage());
				// Continuer avec les autres fichiers même en cas d'erreur
				// Vous pouvez aussi choisir de lancer l'exception pour arrêter tout le processus
				throw new RuntimeException("Erreur lors de l'upload du fichier " + file.getOriginalFilename() + ": " + e.getMessage(), e);
			}
		}

		if (uploadedFiles.isEmpty()) {
			throw new IllegalArgumentException("Aucun fichier valide n'a pu être uploadé");
		}

		logger.info("{} fichier(s) uploadé(s) avec succès pour la période {}", uploadedFiles.size(), dutyPeriodId);
		return uploadedFiles;
	}

	@Override
	public PdfProcessingResultDto processPdfFile(Long dutyFileId) {
		DutyFile dutyFile = dutyFileRepository.findById(dutyFileId)
				.orElseThrow(() -> new ResourceNotFoundException("Fichier de garde", dutyFileId));

		if (dutyFile.getStatus() == ProcessingStatus.PROCESSING) {
			throw new IllegalArgumentException("Le fichier est déjà en cours de traitement");
		}

		// Vérifier que le fichier est lié à une période de garde
		if (dutyFile.getDutyPeriod() == null) {
			throw new IllegalArgumentException("Le fichier doit être lié à une période de garde avant le traitement");
		}

		DutyPeriod dutyPeriod = dutyFile.getDutyPeriod();
		LocalDate startDate = dutyPeriod.getStartDate();
		LocalDate endDate = dutyPeriod.getEndDate();

		dutyFile.setStatus(ProcessingStatus.PROCESSING);
		dutyFileRepository.save(dutyFile);

		PdfProcessingResultDto result = new PdfProcessingResultDto();
		result.setDutyFileId(dutyFileId);

		try {
			// Charger le fichier
			Path filePath = Paths.get(dutyFile.getFilePath());
			if (!Files.exists(filePath)) {
				throw new IOException("Le fichier n'existe plus sur le serveur");
			}

			// Extraire les pharmacies du document (PDF ou image)
			List<ExtractedPharmacyDto> extractedPharmacies;
			try (java.io.InputStream inputStream = Files.newInputStream(filePath)) {
				// Vérifier si OCR est nécessaire
				boolean needsOCR = documentProcessingService.requiresOCR(inputStream, dutyFile.getFileName());
				
				if (needsOCR && googleVisionService != null) {
					try {
						// Utiliser Google Vision API pour les images/PDF scannés
						byte[] fileBytes = Files.readAllBytes(filePath);
						extractedPharmacies = googleVisionService.extractPharmaciesFromImage(fileBytes);
					} catch (Exception e) {
						// Si Google Vision échoue, essayer l'extraction directe si c'est un PDF
						if (dutyFile.getFileName().toLowerCase().endsWith(".pdf")) {
							System.err.println("⚠ Google Vision a échoué, tentative d'extraction directe: " + e.getMessage());
							try (java.io.InputStream inputStream2 = Files.newInputStream(filePath)) {
								extractedPharmacies = documentProcessingService.extractPharmacies(
										inputStream2, dutyFile.getFileName());
							}
						} else {
							// Pour les images, on ne peut pas continuer sans OCR
							throw new IllegalStateException(
									"L'extraction OCR avec Google Vision a échoué. " +
									"Vérifiez la configuration des credentials. Erreur: " + e.getMessage(), e);
						}
					}
				} else {
					// Utiliser l'extraction directe pour les PDF texte
					try (java.io.InputStream inputStream2 = Files.newInputStream(filePath)) {
						extractedPharmacies = documentProcessingService.extractPharmacies(
								inputStream2, dutyFile.getFileName());
					}
				}
			}

			result.setPharmaciesFound(extractedPharmacies.size());

			// Créer les pharmacies et les gardes pour les pharmacies trouvées
			int dutiesCreated = 0;
			int pharmaciesCreated = 0;

			for (ExtractedPharmacyDto extractedPharmacy : extractedPharmacies) {
				// Chercher la pharmacie dans la base de données par nom
				java.util.Optional<Pharmacy> pharmacyOpt = pharmacyRepository.findByName(
						extractedPharmacy.getPharmacyName());

				Pharmacy pharmacy;
				if (pharmacyOpt.isPresent()) {
					// Pharmacie existante
					pharmacy = pharmacyOpt.get();
					extractedPharmacy.setPharmacyId(pharmacy.getId());
					extractedPharmacy.setExistsInDatabase(true);
				} else {
					// Créer la pharmacie avec les informations extraites
					pharmacy = createPharmacyFromExtracted(extractedPharmacy);
					pharmacy = pharmacyRepository.save(pharmacy);
					pharmaciesCreated++;
					extractedPharmacy.setPharmacyId(pharmacy.getId());
					extractedPharmacy.setExistsInDatabase(false); // Nouvellement créée
				}

				// Vérifier si une garde existe déjà pour cette pharmacie et cette période
				List<Duty> existingDuties = dutyRepository.findByPharmacyIdAndDutyPeriodId(
						pharmacy.getId(), dutyPeriod.getId());

				if (existingDuties.isEmpty()) {
					// Créer la garde
					Duty duty = new Duty();
					duty.setPharmacy(pharmacy);
					duty.setDutyPeriod(dutyPeriod);
					duty.setDutyFile(dutyFile);
					dutyRepository.save(duty);
					dutiesCreated++;
				}
			}

			logger.info("Pharmacies créées: {}, Gardes créées: {}", pharmaciesCreated, dutiesCreated);

			result.setDutiesCreated(dutiesCreated);
			result.setPharmacies(extractedPharmacies); // Utiliser directement ExtractedPharmacyDto
			result.setStatus("COMPLETED");

			// Sauvegarder le JSON dans un fichier texte
			saveExtractionToFile(dutyFile, result);

			dutyFile.setStatus(ProcessingStatus.COMPLETED);
			dutyFile.setProcessedDate(LocalDateTime.now());
			dutyFile.setPharmaciesCount(extractedPharmacies.size());
		} catch (Exception e) {
			result.setStatus("FAILED");
			result.setErrorMessage(e.getMessage());
			dutyFile.setStatus(ProcessingStatus.FAILED);
			dutyFile.setErrorMessage(e.getMessage());
		}

		dutyFileRepository.save(dutyFile);
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<DutyFileResponseDto> getAllDutyFiles() {
		return dutyFileRepository.findAll().stream()
				.map(this::mapToFileResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<DutyFileResponseDto> getDutyFilesByPeriodId(Long dutyPeriodId) {
		// Vérifier que la période existe
		if (!dutyPeriodRepository.existsById(dutyPeriodId)) {
			throw new ResourceNotFoundException("Période de garde", dutyPeriodId);
		}
		// Récupérer les fichiers triés par date d'upload (plus récents en premier)
		return dutyFileRepository.findByDutyPeriodIdOrderByUploadDateDesc(dutyPeriodId).stream()
				.map(this::mapToFileResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	public void deleteDutyFile(Long id) {
		DutyFile dutyFile = dutyFileRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Fichier de garde", id));

		// Supprimer les gardes associées à ce fichier
		List<Duty> associatedDuties = dutyRepository.findByDutyFileId(id);
		if (!associatedDuties.isEmpty()) {
			dutyRepository.deleteAll(associatedDuties);
		}

		// Supprimer le fichier physique s'il existe
		if (dutyFile.getFilePath() != null) {
			try {
				Path filePath = Paths.get(dutyFile.getFilePath());
				if (Files.exists(filePath)) {
					Files.delete(filePath);
				}
			} catch (IOException e) {
				// Logger l'erreur mais continuer la suppression de l'entrée en base
				System.err.println("Erreur lors de la suppression du fichier physique: " + e.getMessage());
			}
		}

		// Supprimer l'entrée en base de données
		dutyFileRepository.delete(dutyFile);
	}

	private DutyResponseDto mapToResponseDto(Duty duty) {
		DutyResponseDto dto = new DutyResponseDto();
		dto.setId(duty.getId());
		dto.setPharmacyId(duty.getPharmacy().getId());
		dto.setPharmacyName(duty.getPharmacy().getName());
		// Ajouter les informations de zone de la pharmacie
		dto.setPharmacyZone(duty.getPharmacy().getCity());
		dto.setPharmacyCommune(duty.getPharmacy().getCommune());
		dto.setPharmacyDistrict(duty.getPharmacy().getDistrict());
		if (duty.getDutyPeriod() != null) {
			dto.setDutyPeriodId(duty.getDutyPeriod().getId());
			dto.setStartDate(duty.getDutyPeriod().getStartDate());
			dto.setEndDate(duty.getDutyPeriod().getEndDate());
			dto.setIsCurrent(duty.getDutyPeriod().getIsCurrent());
		}
		if (duty.getDutyFile() != null) {
			dto.setDutyFileId(duty.getDutyFile().getId());
			dto.setDutyFileName(duty.getDutyFile().getFileName());
		}
		dto.setCreatedAt(duty.getCreatedAt());
		dto.setUpdatedAt(duty.getUpdatedAt());
		if (duty.getStatus() != null) {
			dto.setStatus(duty.getStatus().name());
		}
		return dto;
	}

	private void saveExtractionToFile(DutyFile dutyFile, PdfProcessingResultDto result) {
		try {
			// Créer le répertoire s'il n'existe pas
			Path extractionPath = Paths.get(extractionDir);
			if (!Files.exists(extractionPath)) {
				Files.createDirectories(extractionPath);
			}

			// Générer le nom du fichier avec timestamp
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			String baseFileName = dutyFile.getFileName();
			if (baseFileName != null && baseFileName.contains(".")) {
				baseFileName = baseFileName.substring(0, baseFileName.lastIndexOf('.'));
			} else {
				baseFileName = "extraction";
			}
			String txtFileName = baseFileName + "_" + timestamp + "_" + dutyFile.getId() + ".txt";
			Path txtFilePath = extractionPath.resolve(txtFileName);

			// Convertir le résultat en JSON formaté
			String jsonContent = objectMapper.writeValueAsString(result);

			// Ajouter un en-tête informatif
			StringBuilder content = new StringBuilder();
			content.append("=".repeat(80)).append("\n");
			content.append("EXTRACTION DES DONNÉES - FICHIER DE GARDE\n");
			content.append("=".repeat(80)).append("\n");
			content.append("Fichier source: ").append(dutyFile.getFileName()).append("\n");
			content.append("Date d'extraction: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
			content.append("ID du fichier: ").append(dutyFile.getId()).append("\n");
			content.append("Statut: ").append(result.getStatus()).append("\n");
			content.append("Pharmacies trouvées: ").append(result.getPharmaciesFound()).append("\n");
			content.append("Gardes créées: ").append(result.getDutiesCreated()).append("\n");
			content.append("=".repeat(80)).append("\n\n");
			content.append("DONNÉES JSON:\n");
			content.append("-".repeat(80)).append("\n");
			content.append(jsonContent);
			content.append("\n").append("-".repeat(80)).append("\n");

			// Sauvegarder dans le fichier
			Files.write(txtFilePath, content.toString().getBytes(StandardCharsets.UTF_8));

			System.out.println("✓ Extraction sauvegardée dans: " + txtFilePath.toAbsolutePath());
		} catch (Exception e) {
			// Logger l'erreur mais ne pas faire échouer le traitement
			System.err.println("⚠ Erreur lors de la sauvegarde de l'extraction: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public TextExtractionResultDto extractTextOnly(Long dutyFileId) {
		DutyFile dutyFile = dutyFileRepository.findById(dutyFileId)
				.orElseThrow(() -> new ResourceNotFoundException("Fichier de garde", dutyFileId));

		TextExtractionResultDto result = new TextExtractionResultDto();
		result.setDutyFileId(dutyFileId);

		try {
			// Charger le fichier
			Path filePath = Paths.get(dutyFile.getFilePath());
			if (!Files.exists(filePath)) {
				throw new IOException("Le fichier n'existe plus sur le serveur");
			}

			String extractedText = "";

			// Extraire le texte selon le type de fichier
			String fileName = dutyFile.getFileName().toLowerCase();
			if (fileName.endsWith(".pdf")) {
				// Extraction depuis PDF avec PDFBox
				try (java.io.InputStream inputStream = Files.newInputStream(filePath)) {
					extractedText = extractTextFromPdf(inputStream);
				}
			} else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
				// Pour les images, on ne peut pas extraire sans OCR
				throw new UnsupportedOperationException(
						"L'extraction de texte depuis les images JPEG nécessite OCR. " +
						"Utilisez l'API /process avec Google Vision activé.");
			} else {
				throw new IllegalArgumentException("Type de fichier non supporté pour l'extraction de texte: " + fileName);
			}

			// Sauvegarder le texte dans un fichier
			String textFilePath = saveExtractedTextToFile(dutyFile, extractedText);

			result.setStatus("COMPLETED");
			result.setTextFilePath(textFilePath);
			result.setCharacterCount(extractedText.length());
			result.setLineCount(extractedText.split("\\r?\\n").length);

		} catch (Exception e) {
			result.setStatus("FAILED");
			result.setErrorMessage(e.getMessage());
		}

		return result;
	}

	private String extractTextFromPdf(InputStream inputStream) throws IOException {
		try (org.apache.pdfbox.pdmodel.PDDocument document = 
				org.apache.pdfbox.Loader.loadPDF(inputStream.readAllBytes())) {
			org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
			return stripper.getText(document);
		}
	}

	private String saveExtractedTextToFile(DutyFile dutyFile, String extractedText) throws IOException {
		// Créer le répertoire s'il n'existe pas
		Path extractionPath = Paths.get(extractionDir);
		if (!Files.exists(extractionPath)) {
			Files.createDirectories(extractionPath);
		}

		// Générer le nom du fichier avec timestamp
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		String baseFileName = dutyFile.getFileName();
		if (baseFileName != null && baseFileName.contains(".")) {
			baseFileName = baseFileName.substring(0, baseFileName.lastIndexOf('.'));
		} else {
			baseFileName = "extraction";
		}
		String txtFileName = baseFileName + "_TEXT_" + timestamp + "_" + dutyFile.getId() + ".txt";
		Path txtFilePath = extractionPath.resolve(txtFileName);

		// Ajouter un en-tête informatif
		StringBuilder content = new StringBuilder();
		content.append("=".repeat(80)).append("\n");
		content.append("EXTRACTION DE TEXTE BRUT - FICHIER DE GARDE\n");
		content.append("=".repeat(80)).append("\n");
		content.append("Fichier source: ").append(dutyFile.getFileName()).append("\n");
		content.append("Date d'extraction: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
		content.append("ID du fichier: ").append(dutyFile.getId()).append("\n");
		content.append("Nombre de caractères: ").append(extractedText.length()).append("\n");
		content.append("Nombre de lignes: ").append(extractedText.split("\\r?\\n").length).append("\n");
		content.append("=".repeat(80)).append("\n\n");
		content.append("TEXTE EXTRAIT:\n");
		content.append("-".repeat(80)).append("\n");
		content.append(extractedText);
		content.append("\n").append("-".repeat(80)).append("\n");

		// Sauvegarder dans le fichier
		Files.write(txtFilePath, content.toString().getBytes(StandardCharsets.UTF_8));

		System.out.println("✓ Texte brut sauvegardé dans: " + txtFilePath.toAbsolutePath());

		return txtFilePath.toString();
	}

	private DutyFileResponseDto mapToFileResponseDto(DutyFile dutyFile) {
		DutyFileResponseDto dto = new DutyFileResponseDto();
		dto.setId(dutyFile.getId());
		dto.setFileName(dutyFile.getFileName());
		dto.setFileSize(dutyFile.getFileSize());
		dto.setStatus(dutyFile.getStatus());
		dto.setUploadDate(dutyFile.getUploadDate());
		dto.setProcessedDate(dutyFile.getProcessedDate());
		dto.setErrorMessage(dutyFile.getErrorMessage());
		dto.setPharmaciesCount(dutyFile.getPharmaciesCount());
		dto.setDescription(dutyFile.getDescription());
		return dto;
	}

	/**
	 * Crée une pharmacie à partir des informations extraites d'un fichier
	 */
	private Pharmacy createPharmacyFromExtracted(ExtractedPharmacyDto extracted) {
		Pharmacy pharmacy = new Pharmacy();
		
		// Nom de la pharmacie (obligatoire)
		pharmacy.setName(extracted.getPharmacyName());
		
		// Extraire les numéros de téléphone
		String phoneNumbers = extracted.getPhoneNumbers();
		if (phoneNumbers != null && !phoneNumbers.trim().isEmpty()) {
			// Nettoyer le numéro (enlever "TEL.", espaces, etc.)
			String cleanedPhone = phoneNumbers.replaceAll("(?i)TEL\\.?", "").trim();
			
			// Si plusieurs numéros séparés par /, prendre le premier comme phone1
			if (cleanedPhone.contains("/")) {
				String[] phones = cleanedPhone.split("/");
				pharmacy.setPhone1(cleanPhoneNumber(phones[0].trim()));
				if (phones.length > 1) {
					pharmacy.setPhone2(cleanPhoneNumber(phones[1].trim()));
				}
			} else {
				pharmacy.setPhone1(cleanPhoneNumber(cleanedPhone));
			}
		} else {
			// Si pas de téléphone, utiliser une valeur par défaut pour éviter l'erreur de validation
			pharmacy.setPhone1("00000000");
		}
		
		// Ville et commune depuis la zone
		String zone = extracted.getZone();
		if (zone != null && !zone.trim().isEmpty()) {
			pharmacy.setCity(zone.trim());
			// Utiliser la zone comme commune par défaut (peut être amélioré avec la table zones)
			pharmacy.setCommune(zone.trim());
		} else {
			// Valeurs par défaut si pas de zone
			pharmacy.setCity("NON SPECIFIE");
			pharmacy.setCommune("NON SPECIFIE");
		}
		
		// Description (localisation extraite)
		if (extracted.getLocation() != null && !extracted.getLocation().trim().isEmpty()) {
			pharmacy.setDescription(extracted.getLocation().trim());
		}
		
		// Nom du pharmacien
		if (extracted.getPharmacistName() != null && !extracted.getPharmacistName().trim().isEmpty()) {
			pharmacy.setPharmacistName(extracted.getPharmacistName().trim());
		}
		
		// Localisation GPS (par défaut, peut être amélioré)
		pharmacy.setLocation("0,0");
		
		// Statut par défaut : ACTIF
		pharmacy.setStatut(Pharmacy.PharmacyStatus.ACTIF);
		
		return pharmacy;
	}

	/**
	 * Nettoie un numéro de téléphone (enlève les espaces, garde seulement les chiffres)
	 */
	private String cleanPhoneNumber(String phone) {
		if (phone == null) {
			return "00000000";
		}
		// Garder seulement les chiffres et espaces (format: 27 35 91 30 09)
		String cleaned = phone.replaceAll("[^0-9\\s]", "").trim();
		// Si vide après nettoyage, retourner une valeur par défaut
		if (cleaned.isEmpty()) {
			return "00000000";
		}
		// Limiter à 20 caractères
		if (cleaned.length() > 20) {
			cleaned = cleaned.substring(0, 20);
		}
		return cleaned;
	}

	@Override
	@Transactional(readOnly = true)
	public List<DutyResponseDto> getDutiesByPeriodId(Long dutyPeriodId) {
		// Vérifier que la période existe
		if (!dutyPeriodRepository.existsById(dutyPeriodId)) {
			throw new ResourceNotFoundException("Période de garde", dutyPeriodId);
		}
		return dutyRepository.findByDutyPeriodId(dutyPeriodId).stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void clearDutiesByPeriodId(Long dutyPeriodId) {
		// Vérifier que la période existe
		if (!dutyPeriodRepository.existsById(dutyPeriodId)) {
			throw new ResourceNotFoundException("Période de garde", dutyPeriodId);
		}
		// Supprimer toutes les duties de cette période
		List<Duty> duties = dutyRepository.findByDutyPeriodId(dutyPeriodId);
		dutyRepository.deleteAll(duties);
		logger.info("{} gardes supprimées pour la période {}", duties.size(), dutyPeriodId);
	}

	@Override
	@Transactional
	public com.buddies.pharmaciegarde.modules.duty.dto.ExcelProcessingResultDto processExcelFile(MultipartFile file, Long dutyPeriodId) {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("Le fichier Excel est vide");
		}

		// Vérifier le type de fichier
		String contentType = file.getContentType();
		boolean isExcel = contentType != null && (
			contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") || // .xlsx
			contentType.equals("application/vnd.ms-excel") // .xls
		);
		
		if (!isExcel) {
			throw new IllegalArgumentException("Le fichier doit être un fichier Excel (.xlsx ou .xls). Type reçu: " + contentType);
		}

		// Vérifier que la période de garde existe
		DutyPeriod dutyPeriod = dutyPeriodRepository.findById(dutyPeriodId)
				.orElseThrow(() -> new ResourceNotFoundException("Période de garde", dutyPeriodId));

		com.buddies.pharmaciegarde.modules.duty.dto.ExcelProcessingResultDto result = 
				new com.buddies.pharmaciegarde.modules.duty.dto.ExcelProcessingResultDto();
		result.setDutyPeriodId(dutyPeriodId);
		result.setStatus("SUCCESS");

		List<com.buddies.pharmaciegarde.modules.duty.dto.ExcelPharmacyDto> processedPharmacies = new ArrayList<>();
		int pharmaciesFound = 0;
		int pharmaciesCreated = 0;
		int pharmaciesUpdated = 0;
		int dutiesCreated = 0;

		try {
			// Lire le fichier Excel
			Workbook workbook;
			if (contentType != null && contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
				workbook = new XSSFWorkbook(file.getInputStream());
			} else {
				workbook = new HSSFWorkbook(file.getInputStream());
			}

			Sheet sheet = workbook.getSheetAt(0); // Première feuille

			// Ignorer la première ligne (en-têtes)
			int startRow = 1;
			int lastRowNum = sheet.getLastRowNum();

			for (int i = startRow; i <= lastRowNum; i++) {
				Row row = sheet.getRow(i);
				if (row == null) {
					continue;
				}

				// Lire les colonnes
				String zone = getCellValueAsString(row.getCell(0));
				String pharmacyName = getCellValueAsString(row.getCell(1));
				String pharmacistName = getCellValueAsString(row.getCell(2));
				String contacts = getCellValueAsString(row.getCell(3));
				String indication = getCellValueAsString(row.getCell(4));

				// Ignorer les lignes vides
				if (pharmacyName == null || pharmacyName.trim().isEmpty()) {
					continue;
				}

				pharmaciesFound++;

				com.buddies.pharmaciegarde.modules.duty.dto.ExcelPharmacyDto excelPharmacy = 
						new com.buddies.pharmaciegarde.modules.duty.dto.ExcelPharmacyDto();
				excelPharmacy.setZone(zone);
				excelPharmacy.setPharmacyName(pharmacyName.trim());
				excelPharmacy.setPharmacistName(pharmacistName);
				excelPharmacy.setContacts(contacts);
				excelPharmacy.setIndication(indication);

				try {
					// Traiter les contacts (peut contenir plusieurs numéros séparés par des virgules)
					String[] phoneNumbers = contacts != null ? contacts.split("[,;]") : new String[0];
					String phone1 = phoneNumbers.length > 0 ? cleanPhoneNumber(phoneNumbers[0].trim()) : "00000000";
					String phone2 = phoneNumbers.length > 1 ? cleanPhoneNumber(phoneNumbers[1].trim()) : null;

					// Normaliser le nom de la pharmacie (comme dans PharmacyInitializationService)
					String normalizedName = normalizePharmacyName(pharmacyName.trim());

					// Chercher une pharmacie existante par nom + commune + ville
					// Pour l'instant, on utilise seulement le nom car on n'a pas la commune/ville dans l'Excel
					java.util.Optional<Pharmacy> existingPharmacyOpt = pharmacyRepository.findByName(normalizedName);

					Pharmacy pharmacy;
					boolean wasCreated;

					if (existingPharmacyOpt.isPresent()) {
						// Mettre à jour la pharmacie existante
						pharmacy = existingPharmacyOpt.get();
						pharmacy.setPharmacistName(pharmacistName != null ? pharmacistName.trim() : pharmacy.getPharmacistName());
						pharmacy.setPhone1(phone1);
						if (phone2 != null && !phone2.isEmpty()) {
							pharmacy.setPhone2(phone2);
						}
						// Mettre à jour la description avec l'indication si fournie
						if (indication != null && !indication.trim().isEmpty()) {
							String currentDesc = pharmacy.getDescription();
							if (currentDesc == null || currentDesc.trim().isEmpty()) {
								pharmacy.setDescription(indication.trim());
							} else if (!currentDesc.contains(indication.trim())) {
								pharmacy.setDescription(currentDesc + " | " + indication.trim());
							}
						}
						pharmacy = pharmacyRepository.save(pharmacy);
						wasCreated = false;
						pharmaciesUpdated++;
						excelPharmacy.setStatus("UPDATED");
					} else {
						// Créer une nouvelle pharmacie
						pharmacy = new Pharmacy();
						pharmacy.setName(normalizedName);
						pharmacy.setPhone1(phone1);
						pharmacy.setPhone2(phone2);
						pharmacy.setPharmacistName(pharmacistName != null ? pharmacistName.trim() : null);
						pharmacy.setDescription(indication != null ? indication.trim() : null);
						pharmacy.setCity(""); // Valeur par défaut car non fournie dans l'Excel
						pharmacy.setCommune(""); // Valeur par défaut car non fournie dans l'Excel
						pharmacy.setStatut(Pharmacy.PharmacyStatus.ACTIF);
						pharmacy.setLocation("0,0"); // Valeur par défaut
						pharmacy = pharmacyRepository.save(pharmacy);
						wasCreated = true;
						pharmaciesCreated++;
						excelPharmacy.setStatus("CREATED");
					}

					excelPharmacy.setPharmacyId(pharmacy.getId());

					// Créer la garde pour cette pharmacie et cette période
					// Vérifier si une garde existe déjà
					List<Duty> existingDuties = dutyRepository.findByPharmacyIdAndDutyPeriodId(
							pharmacy.getId(), dutyPeriodId);
					
					if (existingDuties.isEmpty()) {
						Duty duty = new Duty();
						duty.setPharmacy(pharmacy);
						duty.setDutyPeriod(dutyPeriod);
						duty = dutyRepository.save(duty);
						excelPharmacy.setDutyId(duty.getId());
						dutiesCreated++;
					} else {
						// Utiliser la garde existante
						excelPharmacy.setDutyId(existingDuties.get(0).getId());
					}

				} catch (Exception e) {
					logger.error("Erreur lors du traitement de la pharmacie {}: {}", pharmacyName, e.getMessage(), e);
					excelPharmacy.setStatus("ERROR");
					excelPharmacy.setErrorMessage(e.getMessage());
				}

				processedPharmacies.add(excelPharmacy);
			}

			workbook.close();

		} catch (IOException e) {
			logger.error("Erreur lors de la lecture du fichier Excel: {}", e.getMessage(), e);
			result.setStatus("ERROR");
			result.setErrorMessage("Erreur lors de la lecture du fichier Excel: " + e.getMessage());
			return result;
		} catch (Exception e) {
			logger.error("Erreur lors du traitement du fichier Excel: {}", e.getMessage(), e);
			result.setStatus("ERROR");
			result.setErrorMessage("Erreur lors du traitement du fichier Excel: " + e.getMessage());
			return result;
		}

		result.setPharmaciesFound(pharmaciesFound);
		result.setPharmaciesCreated(pharmaciesCreated);
		result.setPharmaciesUpdated(pharmaciesUpdated);
		result.setDutiesCreated(dutiesCreated);
		result.setPharmacies(processedPharmacies);

		logger.info("Traitement Excel terminé: {} pharmacies trouvées, {} créées, {} mises à jour, {} gardes créées", 
				pharmaciesFound, pharmaciesCreated, pharmaciesUpdated, dutiesCreated);

		return result;
	}

	/**
	 * Normalise le nom de la pharmacie: remplace "Pharmacie" ou "PHIE" par "PHCIE"
	 */
	private String normalizePharmacyName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return name;
		}
		String normalized = name.trim();

		// Remplacer "PHARMACIE" par "PHCIE" (insensible à la casse)
		normalized = normalized.replaceAll("(?i)\\bPHARMACIE\\b", "PHCIE");

		// Remplacer "PHIE" par "PHCIE" (insensible à la casse)
		normalized = normalized.replaceAll("(?i)\\bPHIE\\b", "PHCIE");

		// Si le nom ne commence pas par PHCIE, l'ajouter
		if (!normalized.toUpperCase().startsWith("PHCIE")) {
			normalized = "PHCIE " + normalized;
		}

		return normalized;
	}

	/**
	 * Récupère la valeur d'une cellule Excel sous forme de chaîne
	 */
	private String getCellValueAsString(Cell cell) {
		if (cell == null) {
			return null;
		}

		CellType cellType = cell.getCellType();
		if (cellType == CellType.FORMULA) {
			cellType = cell.getCachedFormulaResultType();
		}

		switch (cellType) {
			case STRING:
				return cell.getStringCellValue().trim();
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					return cell.getDateCellValue().toString();
				} else {
					// Éviter les décimales inutiles pour les nombres entiers
					double numericValue = cell.getNumericCellValue();
					if (numericValue == Math.floor(numericValue)) {
						return String.valueOf((long) numericValue);
					} else {
						return String.valueOf(numericValue);
					}
				}
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			default:
				return null;
		}
	}

	@Override
	public DutyResponseDto reportDuty(DutyReportDto dutyReportDto) {
		// Log pour déboguer
		logger.debug("Signalement de pharmacie - clientId: {}, pharmacyId: {}", 
				dutyReportDto.getClientId(), dutyReportDto.getPharmacyId());
		
		// Vérifier que le client existe (même approche que pour les commentaires)
		Long clientId = dutyReportDto.getClientId();
		if (clientId == null) {
			throw new IllegalArgumentException("L'ID du client est obligatoire");
		}
		
		Client client = clientRepository.findById(clientId)
				.orElseThrow(() -> {
					logger.error("Client avec l'ID {} non trouvé dans la base de données", clientId);
					return new ResourceNotFoundException("Client", clientId);
				});

		// Vérifier que la pharmacie existe
		Pharmacy pharmacy = pharmacyRepository.findById(dutyReportDto.getPharmacyId())
				.orElseThrow(() -> new ResourceNotFoundException("Pharmacie", dutyReportDto.getPharmacyId()));

		// Récupérer la période de garde active
		java.util.Optional<DutyPeriod> activePeriodOpt = dutyPeriodRepository.findByIsCurrentTrue();
		if (activePeriodOpt.isEmpty()) {
			throw new IllegalArgumentException(
					"Impossible de signaler : aucune période de garde active n'est actuellement en cours. " +
					"Vous ne pouvez signaler que les pharmacies de la période de garde active.");
		}

		DutyPeriod activePeriod = activePeriodOpt.get();

		// Trouver la garde pour cette pharmacie et cette période active uniquement
		List<Duty> duties = dutyRepository.findByPharmacyIdAndDutyPeriodId(
				dutyReportDto.getPharmacyId(), activePeriod.getId());

		if (duties.isEmpty()) {
			throw new IllegalArgumentException(
					"Impossible de signaler cette pharmacie : elle n'est pas en garde pour la période active " +
					"(du " + activePeriod.getStartDate() + " au " + activePeriod.getEndDate() + "). " +
					"Vous ne pouvez signaler que les pharmacies de la période de garde active.");
		}

		// Filtrer pour ne garder que les gardes avec statut ACTIVE ou TO_VERIFY (pas DISABLED)
		List<Duty> activeDuties = duties.stream()
				.filter(d -> d.getStatus() == DutyStatus.ACTIVE || d.getStatus() == DutyStatus.TO_VERIFY)
				.collect(java.util.stream.Collectors.toList());

		if (activeDuties.isEmpty()) {
			throw new IllegalArgumentException(
					"Impossible de signaler cette pharmacie : elle a déjà été désactivée pour la période de garde active.");
		}

		// Prendre la première garde trouvée (normalement il ne devrait y en avoir qu'une)
		Duty duty = activeDuties.get(0);

		// Mettre le statut à TO_VERIFY
		duty.setStatus(DutyStatus.TO_VERIFY);

		Duty updatedDuty = dutyRepository.save(duty);
		return mapToResponseDto(updatedDuty);
	}

	@Override
	public DutyResponseDto disableDuty(Long dutyId) {
		// Vérifier que la garde existe
		Duty duty = dutyRepository.findById(dutyId)
				.orElseThrow(() -> new ResourceNotFoundException("Garde", dutyId));

		// Mettre le statut à DISABLED
		duty.setStatus(DutyStatus.DISABLED);

		Duty updatedDuty = dutyRepository.save(duty);
		return mapToResponseDto(updatedDuty);
	}

	@Override
	public DutyResponseDto enableDuty(Long dutyId) {
		// Vérifier que la garde existe
		Duty duty = dutyRepository.findById(dutyId)
				.orElseThrow(() -> new ResourceNotFoundException("Garde", dutyId));

		// Mettre le statut à ACTIVE
		duty.setStatus(DutyStatus.ACTIVE);

		Duty updatedDuty = dutyRepository.save(duty);
		return mapToResponseDto(updatedDuty);
	}
}

