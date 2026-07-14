package com.buddies.pharmaciegarde.modules.duty.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Données pour créer une garde")
public class DutyCreateDto {

	@Schema(description = "ID de la pharmacie", example = "1", required = true)
	@NotNull(message = "L'ID de la pharmacie est obligatoire")
	private Long pharmacyId;

	@Schema(description = "ID de la période de garde", example = "1", required = true)
	@NotNull(message = "L'ID de la période de garde est obligatoire")
	private Long dutyPeriodId;

	@Schema(description = "ID du fichier PDF source (optionnel)")
	private Long dutyFileId;

	// Constructeurs
	public DutyCreateDto() {
	}

	// Getters et Setters
	public Long getPharmacyId() {
		return pharmacyId;
	}

	public void setPharmacyId(Long pharmacyId) {
		this.pharmacyId = pharmacyId;
	}

	public Long getDutyPeriodId() {
		return dutyPeriodId;
	}

	public void setDutyPeriodId(Long dutyPeriodId) {
		this.dutyPeriodId = dutyPeriodId;
	}

	public Long getDutyFileId() {
		return dutyFileId;
	}

	public void setDutyFileId(Long dutyFileId) {
		this.dutyFileId = dutyFileId;
	}
}

