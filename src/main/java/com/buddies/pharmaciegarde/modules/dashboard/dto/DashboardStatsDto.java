package com.buddies.pharmaciegarde.modules.dashboard.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Statistiques du dashboard du backoffice")
public class DashboardStatsDto {

	@Schema(description = "Vue d'ensemble - Statistiques générales")
	private OverviewStats overview;

	@Schema(description = "Période de garde actuelle")
	private CurrentDutyPeriodInfo currentPeriod;

	@Schema(description = "Statistiques sur les pharmacies")
	private PharmacyStats pharmacyStats;

	@Schema(description = "Statistiques sur les fichiers et le traitement")
	private FileProcessingStats fileStats;

	@Schema(description = "Activité récente (dernières gardes, fichiers uploadés, etc.)")
	private RecentActivity recentActivity;

	@Schema(description = "Alertes et notifications (fichiers en erreur, périodes expirées, etc.)")
	private List<AlertDto> alerts;

	// Constructeurs
	public DashboardStatsDto() {
	}

	// Getters et Setters
	public OverviewStats getOverview() {
		return overview;
	}

	public void setOverview(OverviewStats overview) {
		this.overview = overview;
	}

	public CurrentDutyPeriodInfo getCurrentPeriod() {
		return currentPeriod;
	}

	public void setCurrentPeriod(CurrentDutyPeriodInfo currentPeriod) {
		this.currentPeriod = currentPeriod;
	}

	public PharmacyStats getPharmacyStats() {
		return pharmacyStats;
	}

	public void setPharmacyStats(PharmacyStats pharmacyStats) {
		this.pharmacyStats = pharmacyStats;
	}

	public FileProcessingStats getFileStats() {
		return fileStats;
	}

	public void setFileStats(FileProcessingStats fileStats) {
		this.fileStats = fileStats;
	}

	public RecentActivity getRecentActivity() {
		return recentActivity;
	}

	public void setRecentActivity(RecentActivity recentActivity) {
		this.recentActivity = recentActivity;
	}

	public List<AlertDto> getAlerts() {
		return alerts;
	}

	public void setAlerts(List<AlertDto> alerts) {
		this.alerts = alerts;
	}

	// Classes internes pour structurer les données
	@Schema(description = "Statistiques générales du système")
	public static class OverviewStats {
		@Schema(description = "Nombre total de pharmacies", example = "150")
		private Long totalPharmacies;

		@Schema(description = "Nombre total de périodes de garde créées", example = "25")
		private Long totalPeriods;

		@Schema(description = "Nombre total de gardes (duties) créées", example = "450")
		private Long totalDuties;

		@Schema(description = "Nombre total de clients enregistrés", example = "1250")
		private Long totalClients;

		@Schema(description = "Nombre total d'utilisateurs backoffice", example = "5")
		private Long totalUsers;

		@Schema(description = "Nombre total de zones", example = "120")
		private Long totalZones;

		public OverviewStats() {
		}

		// Getters et Setters
		public Long getTotalPharmacies() {
			return totalPharmacies;
		}

		public void setTotalPharmacies(Long totalPharmacies) {
			this.totalPharmacies = totalPharmacies;
		}

		public Long getTotalPeriods() {
			return totalPeriods;
		}

		public void setTotalPeriods(Long totalPeriods) {
			this.totalPeriods = totalPeriods;
		}

		public Long getTotalDuties() {
			return totalDuties;
		}

		public void setTotalDuties(Long totalDuties) {
			this.totalDuties = totalDuties;
		}

		public Long getTotalClients() {
			return totalClients;
		}

		public void setTotalClients(Long totalClients) {
			this.totalClients = totalClients;
		}

		public Long getTotalUsers() {
			return totalUsers;
		}

		public void setTotalUsers(Long totalUsers) {
			this.totalUsers = totalUsers;
		}

		public Long getTotalZones() {
			return totalZones;
		}

		public void setTotalZones(Long totalZones) {
			this.totalZones = totalZones;
		}
	}

	@Schema(description = "Informations sur la période de garde actuelle")
	public static class CurrentDutyPeriodInfo {
		@Schema(description = "ID de la période active", example = "1")
		private Long periodId;

		@Schema(description = "Date de début", example = "2026-01-09")
		private String startDate;

		@Schema(description = "Date de fin", example = "2026-01-15")
		private String endDate;

		@Schema(description = "Nombre de pharmacies en garde pour cette période", example = "38")
		private Integer pharmaciesInDuty;

		@Schema(description = "Nombre de zones couvertes", example = "12")
		private Integer zonesCovered;

		@Schema(description = "Nombre de fichiers uploadés pour cette période", example = "3")
		private Integer filesUploaded;

		@Schema(description = "Nombre de fichiers traités avec succès", example = "2")
		private Integer filesProcessed;

		@Schema(description = "Jours restants dans la période", example = "5")
		private Integer daysRemaining;

		public CurrentDutyPeriodInfo() {
		}

		// Getters et Setters
		public Long getPeriodId() {
			return periodId;
		}

		public void setPeriodId(Long periodId) {
			this.periodId = periodId;
		}

		public String getStartDate() {
			return startDate;
		}

		public void setStartDate(String startDate) {
			this.startDate = startDate;
		}

		public String getEndDate() {
			return endDate;
		}

		public void setEndDate(String endDate) {
			this.endDate = endDate;
		}

		public Integer getPharmaciesInDuty() {
			return pharmaciesInDuty;
		}

		public void setPharmaciesInDuty(Integer pharmaciesInDuty) {
			this.pharmaciesInDuty = pharmaciesInDuty;
		}

		public Integer getZonesCovered() {
			return zonesCovered;
		}

		public void setZonesCovered(Integer zonesCovered) {
			this.zonesCovered = zonesCovered;
		}

		public Integer getFilesUploaded() {
			return filesUploaded;
		}

		public void setFilesUploaded(Integer filesUploaded) {
			this.filesUploaded = filesUploaded;
		}

		public Integer getFilesProcessed() {
			return filesProcessed;
		}

		public void setFilesProcessed(Integer filesProcessed) {
			this.filesProcessed = filesProcessed;
		}

		public Integer getDaysRemaining() {
			return daysRemaining;
		}

		public void setDaysRemaining(Integer daysRemaining) {
			this.daysRemaining = daysRemaining;
		}
	}

	@Schema(description = "Statistiques sur les pharmacies")
	public static class PharmacyStats {
		@Schema(description = "Pharmacies actives", example = "145")
		private Long activePharmacies;

		@Schema(description = "Pharmacies inactives", example = "5")
		private Long inactivePharmacies;

		@Schema(description = "Distribution par zone (zone -> nombre de pharmacies)")
		private Map<String, Long> pharmaciesByZone;

		@Schema(description = "Top 5 zones avec le plus de pharmacies")
		private List<ZoneCountDto> topZones;

		@Schema(description = "Pharmacies créées ce mois", example = "12")
		private Long pharmaciesCreatedThisMonth;

		public PharmacyStats() {
		}

		// Getters et Setters
		public Long getActivePharmacies() {
			return activePharmacies;
		}

		public void setActivePharmacies(Long activePharmacies) {
			this.activePharmacies = activePharmacies;
		}

		public Long getInactivePharmacies() {
			return inactivePharmacies;
		}

		public void setInactivePharmacies(Long inactivePharmacies) {
			this.inactivePharmacies = inactivePharmacies;
		}

		public Map<String, Long> getPharmaciesByZone() {
			return pharmaciesByZone;
		}

		public void setPharmaciesByZone(Map<String, Long> pharmaciesByZone) {
			this.pharmaciesByZone = pharmaciesByZone;
		}

		public List<ZoneCountDto> getTopZones() {
			return topZones;
		}

		public void setTopZones(List<ZoneCountDto> topZones) {
			this.topZones = topZones;
		}

		public Long getPharmaciesCreatedThisMonth() {
			return pharmaciesCreatedThisMonth;
		}

		public void setPharmaciesCreatedThisMonth(Long pharmaciesCreatedThisMonth) {
			this.pharmaciesCreatedThisMonth = pharmaciesCreatedThisMonth;
		}
	}

	@Schema(description = "Statistiques sur le traitement des fichiers")
	public static class FileProcessingStats {
		@Schema(description = "Total de fichiers uploadés", example = "50")
		private Long totalFiles;

		@Schema(description = "Fichiers en attente de traitement", example = "2")
		private Long pendingFiles;

		@Schema(description = "Fichiers en cours de traitement", example = "1")
		private Long processingFiles;

		@Schema(description = "Fichiers traités avec succès", example = "45")
		private Long completedFiles;

		@Schema(description = "Fichiers en erreur", example = "2")
		private Long failedFiles;

		@Schema(description = "Taux de succès du traitement (%)", example = "95.7")
		private Double successRate;

		@Schema(description = "Total de pharmacies extraites de tous les fichiers", example = "580")
		private Long totalPharmaciesExtracted;

		public FileProcessingStats() {
		}

		// Getters et Setters
		public Long getTotalFiles() {
			return totalFiles;
		}

		public void setTotalFiles(Long totalFiles) {
			this.totalFiles = totalFiles;
		}

		public Long getPendingFiles() {
			return pendingFiles;
		}

		public void setPendingFiles(Long pendingFiles) {
			this.pendingFiles = pendingFiles;
		}

		public Long getProcessingFiles() {
			return processingFiles;
		}

		public void setProcessingFiles(Long processingFiles) {
			this.processingFiles = processingFiles;
		}

		public Long getCompletedFiles() {
			return completedFiles;
		}

		public void setCompletedFiles(Long completedFiles) {
			this.completedFiles = completedFiles;
		}

		public Long getFailedFiles() {
			return failedFiles;
		}

		public void setFailedFiles(Long failedFiles) {
			this.failedFiles = failedFiles;
		}

		public Double getSuccessRate() {
			return successRate;
		}

		public void setSuccessRate(Double successRate) {
			this.successRate = successRate;
		}

		public Long getTotalPharmaciesExtracted() {
			return totalPharmaciesExtracted;
		}

		public void setTotalPharmaciesExtracted(Long totalPharmaciesExtracted) {
			this.totalPharmaciesExtracted = totalPharmaciesExtracted;
		}
	}

	@Schema(description = "Activité récente")
	public static class RecentActivity {
		@Schema(description = "Dernières périodes de garde créées (5 dernières)")
		private List<RecentPeriodDto> recentPeriods;

		@Schema(description = "Derniers fichiers uploadés (5 derniers)")
		private List<RecentFileDto> recentFiles;

		@Schema(description = "Dernières pharmacies créées (5 dernières)")
		private List<RecentPharmacyDto> recentPharmacies;

		public RecentActivity() {
		}

		// Getters et Setters
		public List<RecentPeriodDto> getRecentPeriods() {
			return recentPeriods;
		}

		public void setRecentPeriods(List<RecentPeriodDto> recentPeriods) {
			this.recentPeriods = recentPeriods;
		}

		public List<RecentFileDto> getRecentFiles() {
			return recentFiles;
		}

		public void setRecentFiles(List<RecentFileDto> recentFiles) {
			this.recentFiles = recentFiles;
		}

		public List<RecentPharmacyDto> getRecentPharmacies() {
			return recentPharmacies;
		}

		public void setRecentPharmacies(List<RecentPharmacyDto> recentPharmacies) {
			this.recentPharmacies = recentPharmacies;
		}
	}

	@Schema(description = "Alerte ou notification")
	public static class AlertDto {
		@Schema(description = "Type d'alerte", example = "FILE_ERROR", allowableValues = {"FILE_ERROR", "PERIOD_EXPIRING", "NO_ACTIVE_PERIOD", "PROCESSING_STUCK"})
		private String type;

		@Schema(description = "Titre de l'alerte", example = "Fichier en erreur")
		private String title;

		@Schema(description = "Message de l'alerte", example = "Le fichier garde-semaine-01.pdf a échoué lors du traitement")
		private String message;

		@Schema(description = "Priorité (HIGH, MEDIUM, LOW)", example = "HIGH")
		private String priority;

		@Schema(description = "ID de l'entité concernée (fichier, période, etc.)", example = "5")
		private Long entityId;

		@Schema(description = "Date de création de l'alerte")
		private String createdAt;

		public AlertDto() {
		}

		// Getters et Setters
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getPriority() {
			return priority;
		}

		public void setPriority(String priority) {
			this.priority = priority;
		}

		public Long getEntityId() {
			return entityId;
		}

		public void setEntityId(Long entityId) {
			this.entityId = entityId;
		}

		public String getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(String createdAt) {
			this.createdAt = createdAt;
		}
	}

	@Schema(description = "Zone avec comptage")
	public static class ZoneCountDto {
		@Schema(description = "Nom de la zone", example = "ABENGOUROU")
		private String zoneName;

		@Schema(description = "Nombre de pharmacies", example = "15")
		private Long pharmacyCount;

		public ZoneCountDto() {
		}

		public ZoneCountDto(String zoneName, Long pharmacyCount) {
			this.zoneName = zoneName;
			this.pharmacyCount = pharmacyCount;
		}

		// Getters et Setters
		public String getZoneName() {
			return zoneName;
		}

		public void setZoneName(String zoneName) {
			this.zoneName = zoneName;
		}

		public Long getPharmacyCount() {
			return pharmacyCount;
		}

		public void setPharmacyCount(Long pharmacyCount) {
			this.pharmacyCount = pharmacyCount;
		}
	}

	@Schema(description = "Période récente")
	public static class RecentPeriodDto {
		@Schema(description = "ID de la période", example = "5")
		private Long id;

		@Schema(description = "Date de début", example = "2026-01-16")
		private String startDate;

		@Schema(description = "Date de fin", example = "2026-01-22")
		private String endDate;

		@Schema(description = "Est active", example = "false")
		private Boolean isCurrent;

		@Schema(description = "Nombre de gardes", example = "42")
		private Integer dutiesCount;

		public RecentPeriodDto() {
		}

		// Getters et Setters
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getStartDate() {
			return startDate;
		}

		public void setStartDate(String startDate) {
			this.startDate = startDate;
		}

		public String getEndDate() {
			return endDate;
		}

		public void setEndDate(String endDate) {
			this.endDate = endDate;
		}

		public Boolean getIsCurrent() {
			return isCurrent;
		}

		public void setIsCurrent(Boolean isCurrent) {
			this.isCurrent = isCurrent;
		}

		public Integer getDutiesCount() {
			return dutiesCount;
		}

		public void setDutiesCount(Integer dutiesCount) {
			this.dutiesCount = dutiesCount;
		}
	}

	@Schema(description = "Fichier récent")
	public static class RecentFileDto {
		@Schema(description = "ID du fichier", example = "10")
		private Long id;

		@Schema(description = "Nom du fichier", example = "garde-semaine-05.pdf")
		private String fileName;

		@Schema(description = "Statut", example = "COMPLETED")
		private String status;

		@Schema(description = "Date d'upload", example = "2026-01-10T14:30:00")
		private String uploadDate;

		@Schema(description = "Nombre de pharmacies extraites", example = "38")
		private Integer pharmaciesCount;

		public RecentFileDto() {
		}

		// Getters et Setters
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getUploadDate() {
			return uploadDate;
		}

		public void setUploadDate(String uploadDate) {
			this.uploadDate = uploadDate;
		}

		public Integer getPharmaciesCount() {
			return pharmaciesCount;
		}

		public void setPharmaciesCount(Integer pharmaciesCount) {
			this.pharmaciesCount = pharmaciesCount;
		}
	}

	@Schema(description = "Pharmacie récente")
	public static class RecentPharmacyDto {
		@Schema(description = "ID de la pharmacie", example = "25")
		private Long id;

		@Schema(description = "Nom de la pharmacie", example = "PHCIE DU CENTRE")
		private String name;

		@Schema(description = "Zone/Ville", example = "ABIDJAN")
		private String zone;

		@Schema(description = "Date de création", example = "2026-01-10T10:15:00")
		private String createdAt;

		public RecentPharmacyDto() {
		}

		// Getters et Setters
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getZone() {
			return zone;
		}

		public void setZone(String zone) {
			this.zone = zone;
		}

		public String getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(String createdAt) {
			this.createdAt = createdAt;
		}
	}
}

