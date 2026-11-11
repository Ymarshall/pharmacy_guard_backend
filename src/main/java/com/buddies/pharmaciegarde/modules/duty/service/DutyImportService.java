package com.buddies.pharmaciegarde.modules.duty.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.common.exception.OcrUnavailableException;
import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;
import com.buddies.pharmaciegarde.common.exception.UnreadableDocumentException;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyImportConfirmLineResult;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyImportConfirmRequest;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyImportConfirmResponse;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyImportLineDecision;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyImportLineResponse;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyImportPreviewResponse;
import com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto;
import com.buddies.pharmaciegarde.modules.duty.dto.PharmacyAlternativeResponse;
import com.buddies.pharmaciegarde.modules.duty.matching.MatchStatus;
import com.buddies.pharmaciegarde.modules.duty.matching.PharmacyCandidate;
import com.buddies.pharmaciegarde.modules.duty.matching.PharmacyMatch;
import com.buddies.pharmaciegarde.modules.duty.matching.PharmacyMatcher;
import com.buddies.pharmaciegarde.modules.duty.matching.PharmacyNameNormalizer;
import com.buddies.pharmaciegarde.modules.duty.model.Duty;
import com.buddies.pharmaciegarde.modules.duty.model.DutyFile;
import com.buddies.pharmaciegarde.modules.duty.model.DutyFile.ProcessingStatus;
import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyFileRepository;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyPeriodRepository;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyRepository;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyRepository;

@Service
public class DutyImportService {

	private static final Logger logger = LoggerFactory.getLogger(DutyImportService.class);

	private final DutyFileRepository dutyFileRepository;
	private final DutyPeriodRepository dutyPeriodRepository;
	private final DutyRepository dutyRepository;
	private final PharmacyRepository pharmacyRepository;
	private final DutyDocumentExtractor extractor;
	private final PharmacyMatcher matcher;
	private final UploadStorageService uploadStorageService;

	public DutyImportService(DutyFileRepository dutyFileRepository, DutyPeriodRepository dutyPeriodRepository,
			DutyRepository dutyRepository, PharmacyRepository pharmacyRepository, DutyDocumentExtractor extractor,
			PharmacyMatcher matcher, UploadStorageService uploadStorageService) {
		this.dutyFileRepository = dutyFileRepository;
		this.dutyPeriodRepository = dutyPeriodRepository;
		this.dutyRepository = dutyRepository;
		this.pharmacyRepository = pharmacyRepository;
		this.extractor = extractor;
		this.matcher = matcher;
		this.uploadStorageService = uploadStorageService;
	}

	@Transactional
	public DutyImportPreviewResponse preview(Long dutyFileId) {
		DutyFile dutyFile = loadFile(dutyFileId);
		DutyPeriod period = requirePeriod(dutyFile);
		Path path = uploadStorageService.resolveInsideRoot(dutyFile.getFilePath());
		try {
			byte[] bytes = Files.readAllBytes(path);
			String hash = UploadStorageService.sha256(bytes);
			dutyFile.setContentHash(hash);
			List<ExtractedPharmacyDto> extracted = extractor.extract(path, dutyFile.getFileName());
			List<PharmacyCandidate> catalog = catalog();
			List<DutyImportLineResponse> lines = new ArrayList<>();
			int matched = 0;
			int ambiguous = 0;
			int notFound = 0;
			int duplicates = 0;
			int index = 0;
			for (ExtractedPharmacyDto row : extracted) {
				PharmacyMatch match = matcher.match(row.getPharmacyName(), row.getZone(), row.getPhoneNumbers(), catalog);
				boolean dutyExists = false;
				Long suggestedId = match.suggested() == null ? null : match.suggested().id();
				if (suggestedId != null) {
					dutyExists = !dutyRepository.findByPharmacyIdAndDutyPeriodId(suggestedId, period.getId()).isEmpty();
				}
				if (match.status() == MatchStatus.MATCHED) {
					matched++;
				} else if (match.status() == MatchStatus.AMBIGUOUS) {
					ambiguous++;
				} else {
					notFound++;
				}
				if (dutyExists) {
					duplicates++;
				}
				lines.add(toLine(index++, row, match, dutyExists));
			}
			boolean already = dutyFileRepository.existsByContentHashAndDutyPeriodIdAndStatus(
					hash, period.getId(), ProcessingStatus.COMPLETED);
			dutyFileRepository.save(dutyFile);
			logger.info("Prévisualisation fichier {} : {} lignes, {} sûres, {} ambiguës, {} inconnues",
					dutyFileId, lines.size(), matched, ambiguous, notFound);
			return new DutyImportPreviewResponse(dutyFileId, period.getId(), hash, already, lines.size(), matched,
					ambiguous, notFound, duplicates, lines);
		} catch (OcrUnavailableException | UnreadableDocumentException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.error("Échec de la prévisualisation du fichier {}", dutyFileId, ex);
			throw new UnreadableDocumentException("Fichier illisible");
		}
	}

	@Transactional
	public DutyImportConfirmResponse confirm(Long dutyFileId, DutyImportConfirmRequest request) {
		DutyFile dutyFile = loadFile(dutyFileId);
		DutyPeriod period = requirePeriod(dutyFile);
		int createdDuties = 0;
		int createdPharmacies = 0;
		int skipped = 0;
		int duplicates = 0;
		int errors = 0;
		List<DutyImportConfirmLineResult> results = new ArrayList<>();
		int index = 0;
		for (DutyImportLineDecision line : request.lines()) {
			if (line.skip()) {
				skipped++;
				results.add(new DutyImportConfirmLineResult(index++, "SKIPPED", null, null, "Ligne ignorée"));
				continue;
			}
			try {
				Pharmacy pharmacy = resolvePharmacy(line);
				if (pharmacy.getId() == null) {
					pharmacy = pharmacyRepository.save(pharmacy);
					createdPharmacies++;
				}
				List<Duty> existing = dutyRepository.findByPharmacyIdAndDutyPeriodId(pharmacy.getId(), period.getId());
				if (!existing.isEmpty()) {
					duplicates++;
					results.add(new DutyImportConfirmLineResult(index++, "DUPLICATE", pharmacy.getId(),
							existing.get(0).getId(), "Une garde existe déjà pour cette pharmacie sur la période"));
					continue;
				}
				Duty duty = new Duty();
				duty.setPharmacy(pharmacy);
				duty.setDutyPeriod(period);
				duty.setDutyFile(dutyFile);
				duty = dutyRepository.save(duty);
				createdDuties++;
				results.add(new DutyImportConfirmLineResult(index++, "CREATED", pharmacy.getId(), duty.getId(), null));
			} catch (IllegalArgumentException | ResourceNotFoundException ex) {
				errors++;
				results.add(new DutyImportConfirmLineResult(index++, "ERROR", line.pharmacyId(), null, ex.getMessage()));
			}
		}
		dutyFile.setStatus(errors > 0 && createdDuties == 0 ? ProcessingStatus.FAILED : ProcessingStatus.COMPLETED);
		dutyFile.setPharmaciesCount(request.lines().size());
		dutyFile.setProcessedDate(java.time.LocalDateTime.now());
		dutyFileRepository.save(dutyFile);
		return new DutyImportConfirmResponse(createdDuties, createdPharmacies, skipped, duplicates, errors, results);
	}

	private Pharmacy resolvePharmacy(DutyImportLineDecision line) {
		if (line.pharmacyId() != null) {
			return pharmacyRepository.findById(line.pharmacyId())
					.orElseThrow(() -> new ResourceNotFoundException("Pharmacie", line.pharmacyId()));
		}
		String displayName = line.pharmacyName().trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
		return pharmacyRepository.findByName(displayName).orElseGet(() -> {
			if (!line.createPharmacy()) {
				throw new IllegalArgumentException(
						"Aucune pharmacie sélectionnée pour « " + line.pharmacyName() + " ». Choisissez une alternative ou créez-la.");
			}
			String phone = realPhone(line);
			if (pharmacyRepository.existsByPhone1(phone)) {
				throw new IllegalArgumentException("Une pharmacie utilise déjà le téléphone " + phone);
			}
			String city = upperPlace(firstText(line.city(), line.zone()));
			String commune = upperPlace(firstText(line.commune(), line.zone()));
			Pharmacy created = new Pharmacy();
			created.setName(trim(displayName, 255));
			created.setPharmacistName(blankToNull(line.pharmacistName()));
			created.setPhone1(phone);
			created.setDescription(blankToNull(firstText(line.description(), line.location())));
			created.setCity(trim(city, 100));
			created.setCommune(trim(commune, 100));
			created.setStatut(Pharmacy.PharmacyStatus.ACTIF);
			created.setLocation(blankToNull(line.coordinates()));
			return created;
		});
	}

	private String realPhone(DutyImportLineDecision line) {
		String raw = firstText(line.phone(), firstToken(line.phoneNumbers()));
		if (raw == null) {
			throw new IllegalArgumentException("Un numéro de téléphone réel est obligatoire");
		}
		String trimmed = raw.trim();
		if (trimmed.length() <= 20 && PharmacyNameNormalizer.phoneKey(trimmed).length() >= 8) {
			return trimmed;
		}
		String digits = PharmacyNameNormalizer.phoneKey(trimmed);
		if (digits.length() < 8) {
			throw new IllegalArgumentException("Un numéro de téléphone réel est obligatoire");
		}
		return digits.length() > 20 ? digits.substring(0, 20) : digits;
	}

	private String firstToken(String phones) {
		if (phones == null || phones.isBlank()) {
			return null;
		}
		String[] parts = phones.split("[/,;]");
		return parts.length == 0 ? phones : parts[0];
	}

	private String firstText(String preferred, String fallback) {
		if (preferred != null && !preferred.isBlank()) {
			return preferred.trim();
		}
		if (fallback != null && !fallback.isBlank()) {
			return fallback.trim();
		}
		return null;
	}

	private String upperPlace(String value) {
		if (value == null) {
			throw new IllegalArgumentException("La ville et la commune sont obligatoires");
		}
		return value.toUpperCase(Locale.ROOT);
	}

	private List<PharmacyCandidate> catalog() {
		return pharmacyRepository.findAll().stream()
				.map(pharmacy -> new PharmacyCandidate(pharmacy.getId(), pharmacy.getName(), pharmacy.getCity(),
						pharmacy.getCommune(), pharmacy.getDistrict(), pharmacy.getPhone1(), pharmacy.getPhone2()))
				.toList();
	}

	private DutyImportLineResponse toLine(int index, ExtractedPharmacyDto row, PharmacyMatch match, boolean dutyExists) {
		Long suggestedId = match.suggested() == null ? null : match.suggested().id();
		String suggestedName = match.suggested() == null ? null : match.suggested().name();
		String suggestedCity = match.suggested() == null ? null : match.suggested().city();
		List<PharmacyAlternativeResponse> alternatives = match.alternatives().stream()
				.map(item -> new PharmacyAlternativeResponse(item.pharmacy().id(), item.pharmacy().name(),
						item.pharmacy().city(), item.pharmacy().commune(), item.pharmacy().phone1(),
						Math.round(item.score() * 1000.0) / 1000.0))
				.toList();
		return new DutyImportLineResponse(index, row.getZone(), row.getPharmacyName(), match.normalizedName(),
				row.getPharmacistName(), row.getPhoneNumbers(), row.getLocation(), match.status().name(),
				match.confidence(), suggestedId, suggestedName, suggestedCity, dutyExists, alternatives);
	}

	private DutyFile loadFile(Long id) {
		return dutyFileRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Fichier de garde", id));
	}

	public void requireExisting(Long dutyFileId) {
		loadFile(dutyFileId);
	}

	private DutyPeriod requirePeriod(DutyFile dutyFile) {
		if (dutyFile.getDutyPeriod() == null || dutyFile.getDutyPeriod().getId() == null) {
			throw new IllegalArgumentException("Le fichier doit être lié à une période de garde");
		}
		return dutyPeriodRepository.findById(dutyFile.getDutyPeriod().getId())
				.orElseThrow(() -> new ResourceNotFoundException("Période de garde", dutyFile.getDutyPeriod().getId()));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String trim(String value, int max) {
		if (value.length() <= max) {
			return value;
		}
		return value.substring(0, max);
	}
}
