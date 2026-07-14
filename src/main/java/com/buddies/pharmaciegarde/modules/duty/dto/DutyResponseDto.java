package com.buddies.pharmaciegarde.modules.duty.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse contenant les informations d'une garde")
public class DutyResponseDto {

	@Schema(description = "Identifiant unique de la garde", example = "1")
	private Long id;

	@Schema(description = "ID de la pharmacie", example = "1")
	private Long pharmacyId;

	@Schema(description = "Nom de la pharmacie", example = "Pharmacie Centrale")
	private String pharmacyName;

	@Schema(description = "Zone/Ville de la pharmacie", example = "ABENGOUROU")
	private String pharmacyZone;

	@Schema(description = "Commune de la pharmacie", example = "ABENGOUROU")
	private String pharmacyCommune;

	@Schema(description = "District de la pharmacie", example = "Indénié-Djuablin")
	private String pharmacyDistrict;

	@Schema(description = "ID de la période de garde", example = "1")
	private Long dutyPeriodId;

	@Schema(description = "Date de début de la période de garde", example = "2024-01-01")
	private LocalDate startDate;

	@Schema(description = "Date de fin de la période de garde", example = "2024-01-07")
	private LocalDate endDate;

	@Schema(description = "Garde courante/active", example = "true")
	private Boolean isCurrent;

	@Schema(description = "ID du fichier PDF source", example = "1")
	private Long dutyFileId;

	@Schema(description = "Nom du fichier PDF source", example = "garde-semaine-01.pdf")
	private String dutyFileName;

	@Schema(description = "Date de création")
	private LocalDateTime createdAt;

	@Schema(description = "Date de mise à jour")
	private LocalDateTime updatedAt;

	@Schema(description = "Statut de la garde", example = "ACTIVE")
	private String status;

	// Constructeurs
	public DutyResponseDto() {
	}

	// Getters et Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPharmacyId() {
		return pharmacyId;
	}

	public void setPharmacyId(Long pharmacyId) {
		this.pharmacyId = pharmacyId;
	}

	public String getPharmacyName() {
		return pharmacyName;
	}

	public void setPharmacyName(String pharmacyName) {
		this.pharmacyName = pharmacyName;
	}

	public String getPharmacyZone() {
		return pharmacyZone;
	}

	public void setPharmacyZone(String pharmacyZone) {
		this.pharmacyZone = pharmacyZone;
	}

	public String getPharmacyCommune() {
		return pharmacyCommune;
	}

	public void setPharmacyCommune(String pharmacyCommune) {
		this.pharmacyCommune = pharmacyCommune;
	}

	public String getPharmacyDistrict() {
		return pharmacyDistrict;
	}

	public void setPharmacyDistrict(String pharmacyDistrict) {
		this.pharmacyDistrict = pharmacyDistrict;
	}

	public Long getDutyPeriodId() {
		return dutyPeriodId;
	}

	public void setDutyPeriodId(Long dutyPeriodId) {
		this.dutyPeriodId = dutyPeriodId;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public Boolean getIsCurrent() {
		return isCurrent;
	}

	public void setIsCurrent(Boolean isCurrent) {
		this.isCurrent = isCurrent;
	}

	public Long getDutyFileId() {
		return dutyFileId;
	}

	public void setDutyFileId(Long dutyFileId) {
		this.dutyFileId = dutyFileId;
	}

	public String getDutyFileName() {
		return dutyFileName;
	}

	public void setDutyFileName(String dutyFileName) {
		this.dutyFileName = dutyFileName;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}

