package com.buddies.pharmaciegarde.modules.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.modules.common.repository.ZoneRepository;
import com.buddies.pharmaciegarde.modules.dashboard.dto.DashboardStatsDto;
import com.buddies.pharmaciegarde.modules.dashboard.dto.DashboardStatsDto.AlertDto;
import com.buddies.pharmaciegarde.modules.dashboard.dto.DashboardStatsDto.CurrentDutyPeriodInfo;
import com.buddies.pharmaciegarde.modules.dashboard.dto.DashboardStatsDto.FileProcessingStats;
import com.buddies.pharmaciegarde.modules.dashboard.dto.DashboardStatsDto.OverviewStats;
import com.buddies.pharmaciegarde.modules.dashboard.dto.DashboardStatsDto.PharmacyStats;
import com.buddies.pharmaciegarde.modules.dashboard.dto.DashboardStatsDto.RecentActivity;
import com.buddies.pharmaciegarde.modules.dashboard.dto.DashboardStatsDto.RecentFileDto;
import com.buddies.pharmaciegarde.modules.dashboard.dto.DashboardStatsDto.RecentPeriodDto;
import com.buddies.pharmaciegarde.modules.dashboard.dto.DashboardStatsDto.RecentPharmacyDto;
import com.buddies.pharmaciegarde.modules.dashboard.dto.DashboardStatsDto.ZoneCountDto;
import com.buddies.pharmaciegarde.modules.duty.model.DutyFile;
import com.buddies.pharmaciegarde.modules.duty.model.DutyFile.ProcessingStatus;
import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyFileRepository;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyPeriodRepository;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyRepository;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy.PharmacyStatus;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyRepository;
import com.buddies.pharmaciegarde.modules.user.repository.ClientRepository;
import com.buddies.pharmaciegarde.modules.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

	private final PharmacyRepository pharmacyRepository;
	private final DutyPeriodRepository dutyPeriodRepository;
	private final DutyRepository dutyRepository;
	private final DutyFileRepository dutyFileRepository;
	private final ClientRepository clientRepository;
	private final UserRepository userRepository;
	private final ZoneRepository zoneRepository;

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	public DashboardServiceImpl(
			PharmacyRepository pharmacyRepository,
			DutyPeriodRepository dutyPeriodRepository,
			DutyRepository dutyRepository,
			DutyFileRepository dutyFileRepository,
			ClientRepository clientRepository,
			UserRepository userRepository,
			ZoneRepository zoneRepository) {
		this.pharmacyRepository = pharmacyRepository;
		this.dutyPeriodRepository = dutyPeriodRepository;
		this.dutyRepository = dutyRepository;
		this.dutyFileRepository = dutyFileRepository;
		this.clientRepository = clientRepository;
		this.userRepository = userRepository;
		this.zoneRepository = zoneRepository;
	}

	@Override
	public DashboardStatsDto getDashboardStats() {
		DashboardStatsDto dashboard = new DashboardStatsDto();

		// 1. Vue d'ensemble
		dashboard.setOverview(buildOverviewStats());

		// 2. Période de garde actuelle
		dashboard.setCurrentPeriod(buildCurrentPeriodInfo());

		// 3. Statistiques pharmacies
		dashboard.setPharmacyStats(buildPharmacyStats());

		// 4. Statistiques fichiers
		dashboard.setFileStats(buildFileProcessingStats());

		// 5. Activité récente
		dashboard.setRecentActivity(buildRecentActivity());

		// 6. Alertes
		dashboard.setAlerts(buildAlerts());

		return dashboard;
	}

	private OverviewStats buildOverviewStats() {
		OverviewStats overview = new OverviewStats();
		overview.setTotalPharmacies(pharmacyRepository.count());
		overview.setTotalPeriods(dutyPeriodRepository.count());
		overview.setTotalDuties(dutyRepository.count());
		overview.setTotalClients(clientRepository.count());
		overview.setTotalUsers(userRepository.count());
		overview.setTotalZones(zoneRepository.count());
		return overview;
	}

	private CurrentDutyPeriodInfo buildCurrentPeriodInfo() {
		CurrentDutyPeriodInfo currentPeriod = new CurrentDutyPeriodInfo();

		Optional<DutyPeriod> activePeriodOpt = dutyPeriodRepository.findByIsCurrentTrue();
		if (activePeriodOpt.isPresent()) {
			DutyPeriod activePeriod = activePeriodOpt.get();
			currentPeriod.setPeriodId(activePeriod.getId());
			currentPeriod.setStartDate(activePeriod.getStartDate().format(DATE_FORMATTER));
			currentPeriod.setEndDate(activePeriod.getEndDate().format(DATE_FORMATTER));

			// Compter les pharmacies en garde pour cette période
			long dutiesCount = dutyRepository.findByDutyPeriodId(activePeriod.getId()).size();
			currentPeriod.setPharmaciesInDuty((int) dutiesCount);

			// Compter les zones uniques couvertes
			List<String> uniqueZones = dutyRepository.findByDutyPeriodId(activePeriod.getId()).stream()
					.map(d -> d.getPharmacy().getCity())
					.distinct()
					.collect(Collectors.toList());
			currentPeriod.setZonesCovered(uniqueZones.size());

			// Compter les fichiers uploadés pour cette période
			long filesUploaded = dutyFileRepository.findByDutyPeriodIdOrderByUploadDateDesc(activePeriod.getId())
					.size();
			currentPeriod.setFilesUploaded((int) filesUploaded);

			// Compter les fichiers traités avec succès
			long filesProcessed = dutyFileRepository.findByDutyPeriodIdOrderByUploadDateDesc(activePeriod.getId())
					.stream()
					.filter(f -> f.getStatus() == ProcessingStatus.COMPLETED)
					.count();
			currentPeriod.setFilesProcessed((int) filesProcessed);

			// Calculer les jours restants
			LocalDate today = LocalDate.now();
			LocalDate endDate = activePeriod.getEndDate();
			if (endDate.isAfter(today)) {
				long daysRemaining = ChronoUnit.DAYS.between(today, endDate);
				currentPeriod.setDaysRemaining((int) daysRemaining);
			} else {
				currentPeriod.setDaysRemaining(0);
			}
		} else {
			// Pas de période active
			currentPeriod.setPeriodId(null);
			currentPeriod.setPharmaciesInDuty(0);
			currentPeriod.setZonesCovered(0);
			currentPeriod.setFilesUploaded(0);
			currentPeriod.setFilesProcessed(0);
			currentPeriod.setDaysRemaining(0);
		}

		return currentPeriod;
	}

	private PharmacyStats buildPharmacyStats() {
		PharmacyStats stats = new PharmacyStats();

		// Compter par statut
		long activeCount = pharmacyRepository.findByStatut(PharmacyStatus.ACTIF).size();
		long inactiveCount = pharmacyRepository.findByStatut(PharmacyStatus.INACTIF).size();
		stats.setActivePharmacies(activeCount);
		stats.setInactivePharmacies(inactiveCount);

		// Distribution par zone (city)
		Map<String, Long> pharmaciesByZone = pharmacyRepository.findAll().stream()
				.collect(Collectors.groupingBy(
						Pharmacy::getCity,
						Collectors.counting()));

		// Trier et prendre le top 5
		List<ZoneCountDto> topZones = pharmaciesByZone.entrySet().stream()
				.sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
				.limit(5)
				.map(e -> new ZoneCountDto(e.getKey(), e.getValue()))
				.collect(Collectors.toList());

		stats.setPharmaciesByZone(pharmaciesByZone);
		stats.setTopZones(topZones);

		// Pharmacies créées ce mois
		// TODO: Ajouter un champ createdAt à Pharmacy pour un calcul précis
		// Pour l'instant, on ne peut pas calculer cela sans createdAt
		stats.setPharmaciesCreatedThisMonth(0L); // Temporaire - nécessite createdAt dans Pharmacy

		return stats;
	}

	private FileProcessingStats buildFileProcessingStats() {
		FileProcessingStats stats = new FileProcessingStats();

		List<DutyFile> allFiles = dutyFileRepository.findAll();
		stats.setTotalFiles((long) allFiles.size());

		long pending = allFiles.stream()
				.filter(f -> f.getStatus() == ProcessingStatus.PENDING)
				.count();
		stats.setPendingFiles(pending);

		long processing = allFiles.stream()
				.filter(f -> f.getStatus() == ProcessingStatus.PROCESSING)
				.count();
		stats.setProcessingFiles(processing);

		long completed = allFiles.stream()
				.filter(f -> f.getStatus() == ProcessingStatus.COMPLETED)
				.count();
		stats.setCompletedFiles(completed);

		long failed = allFiles.stream()
				.filter(f -> f.getStatus() == ProcessingStatus.FAILED)
				.count();
		stats.setFailedFiles(failed);

		// Calculer le taux de succès
		long processedFiles = completed + failed;
		if (processedFiles > 0) {
			double successRate = (double) completed / processedFiles * 100.0;
			stats.setSuccessRate(Math.round(successRate * 100.0) / 100.0); // Arrondir à 2 décimales
		} else {
			stats.setSuccessRate(0.0);
		}

		// Total de pharmacies extraites
		long totalExtracted = allFiles.stream()
				.filter(f -> f.getPharmaciesCount() != null)
				.mapToLong(DutyFile::getPharmaciesCount)
				.sum();
		stats.setTotalPharmaciesExtracted(totalExtracted);

		return stats;
	}

	private RecentActivity buildRecentActivity() {
		RecentActivity activity = new RecentActivity();

		// Dernières périodes (5 dernières, triées par date de création décroissante)
		List<RecentPeriodDto> recentPeriods = dutyPeriodRepository.findAll().stream()
				.sorted((p1, p2) -> p2.getStartDate().compareTo(p1.getStartDate()))
				.limit(5)
				.map(period -> {
					RecentPeriodDto dto = new RecentPeriodDto();
					dto.setId(period.getId());
					dto.setStartDate(period.getStartDate().format(DATE_FORMATTER));
					dto.setEndDate(period.getEndDate().format(DATE_FORMATTER));
					dto.setIsCurrent(period.getIsCurrent());
					dto.setDutiesCount(dutyRepository.findByDutyPeriodId(period.getId()).size());
					return dto;
				})
				.collect(Collectors.toList());
		activity.setRecentPeriods(recentPeriods);

		// Derniers fichiers uploadés (5 derniers)
		List<RecentFileDto> recentFiles = dutyFileRepository.findAll().stream()
				.sorted((f1, f2) -> f2.getUploadDate().compareTo(f1.getUploadDate()))
				.limit(5)
				.map(file -> {
					RecentFileDto dto = new RecentFileDto();
					dto.setId(file.getId());
					dto.setFileName(file.getFileName());
					dto.setStatus(file.getStatus().name());
					dto.setUploadDate(file.getUploadDate().format(DATETIME_FORMATTER));
					dto.setPharmaciesCount(file.getPharmaciesCount());
					return dto;
				})
				.collect(Collectors.toList());
		activity.setRecentFiles(recentFiles);

		// Dernières pharmacies créées (5 dernières)
		// Note: Pharmacy n'a pas de createdAt, donc on prend les dernières par ID
		List<RecentPharmacyDto> recentPharmacies = pharmacyRepository.findAll().stream()
				.sorted((p1, p2) -> Long.compare(p2.getId(), p1.getId()))
				.limit(5)
				.map(pharmacy -> {
					RecentPharmacyDto dto = new RecentPharmacyDto();
					dto.setId(pharmacy.getId());
					dto.setName(pharmacy.getName());
					dto.setZone(pharmacy.getCity());
					// dto.setCreatedAt(...); // TODO: Ajouter createdAt à Pharmacy
					return dto;
				})
				.collect(Collectors.toList());
		activity.setRecentPharmacies(recentPharmacies);

		return activity;
	}

	private List<AlertDto> buildAlerts() {
		List<AlertDto> alerts = new ArrayList<>();

		// Vérifier s'il y a des fichiers en erreur
		List<DutyFile> failedFiles = dutyFileRepository.findByStatus(ProcessingStatus.FAILED);
		for (DutyFile file : failedFiles) {
			AlertDto alert = new AlertDto();
			alert.setType("FILE_ERROR");
			alert.setTitle("Fichier en erreur");
			alert.setMessage("Le fichier '" + file.getFileName() + "' a échoué lors du traitement. "
					+ (file.getErrorMessage() != null ? "Erreur: " + file.getErrorMessage() : ""));
			alert.setPriority("HIGH");
			alert.setEntityId(file.getId());
			if (file.getUploadDate() != null) {
				alert.setCreatedAt(file.getUploadDate().format(DATETIME_FORMATTER));
			}
			alerts.add(alert);
		}

		// Vérifier s'il y a des fichiers en traitement depuis trop longtemps (plus de 30 minutes)
		List<DutyFile> processingFiles = dutyFileRepository.findByStatus(ProcessingStatus.PROCESSING);
		LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
		for (DutyFile file : processingFiles) {
			if (file.getUploadDate() != null && file.getUploadDate().isBefore(thirtyMinutesAgo)) {
				AlertDto alert = new AlertDto();
				alert.setType("PROCESSING_STUCK");
				alert.setTitle("Traitement bloqué");
				alert.setMessage("Le fichier '" + file.getFileName()
						+ "' est en cours de traitement depuis plus de 30 minutes. Il pourrait être bloqué.");
				alert.setPriority("MEDIUM");
				alert.setEntityId(file.getId());
				if (file.getUploadDate() != null) {
					alert.setCreatedAt(file.getUploadDate().format(DATETIME_FORMATTER));
				}
				alerts.add(alert);
			}
		}

		// Vérifier s'il y a une période active
		Optional<DutyPeriod> activePeriodOpt = dutyPeriodRepository.findByIsCurrentTrue();
		if (activePeriodOpt.isEmpty()) {
			AlertDto alert = new AlertDto();
			alert.setType("NO_ACTIVE_PERIOD");
			alert.setTitle("Aucune période active");
			alert.setMessage("Aucune période de garde n'est actuellement active. Veuillez activer une période.");
			alert.setPriority("HIGH");
			alert.setEntityId(null);
			alert.setCreatedAt(LocalDateTime.now().format(DATETIME_FORMATTER));
			alerts.add(alert);
		} else {
			// Vérifier si la période active expire dans moins de 3 jours
			DutyPeriod activePeriod = activePeriodOpt.get();
			LocalDate today = LocalDate.now();
			LocalDate endDate = activePeriod.getEndDate();
			long daysUntilEnd = ChronoUnit.DAYS.between(today, endDate);
			if (daysUntilEnd <= 3 && daysUntilEnd >= 0) {
				AlertDto alert = new AlertDto();
				alert.setType("PERIOD_EXPIRING");
				alert.setTitle("Période de garde expire bientôt");
				alert.setMessage("La période de garde active expire dans " + daysUntilEnd
						+ " jour(s) (le " + endDate.format(DATE_FORMATTER) + "). Pensez à préparer la prochaine période.");
				alert.setPriority("MEDIUM");
				alert.setEntityId(activePeriod.getId());
				alert.setCreatedAt(LocalDateTime.now().format(DATETIME_FORMATTER));
				alerts.add(alert);
			}
		}

		// Vérifier s'il y a des fichiers en attente de traitement
		long pendingFilesCount = dutyFileRepository.findByStatus(ProcessingStatus.PENDING).size();
		if (pendingFilesCount > 0) {
			AlertDto alert = new AlertDto();
			alert.setType("PENDING_FILES");
			alert.setTitle("Fichiers en attente de traitement");
			alert.setMessage(pendingFilesCount + " fichier(s) sont en attente de traitement.");
			alert.setPriority("LOW");
			alert.setEntityId(null);
			alert.setCreatedAt(LocalDateTime.now().format(DATETIME_FORMATTER));
			alerts.add(alert);
		}

		return alerts;
	}
}

