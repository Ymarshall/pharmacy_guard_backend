package com.buddies.pharmaciegarde.modules.duty.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Données pour signaler une pharmacie de garde")
public class DutyReportDto {

	@Schema(description = "ID du client qui signale", example = "1", required = true)
	@NotNull(message = "L'ID du client est obligatoire")
	private Long clientId;

	@Schema(description = "ID de la pharmacie signalée", example = "1", required = true)
	@NotNull(message = "L'ID de la pharmacie est obligatoire")
	private Long pharmacyId;

	// Constructeurs
	public DutyReportDto() {
	}

	public DutyReportDto(Long clientId, Long pharmacyId) {
		this.clientId = clientId;
		this.pharmacyId = pharmacyId;
	}

	// Getters et Setters
	public Long getClientId() {
		return clientId;
	}

	public void setClientId(Long clientId) {
		this.clientId = clientId;
	}

	public Long getPharmacyId() {
		return pharmacyId;
	}

	public void setPharmacyId(Long pharmacyId) {
		this.pharmacyId = pharmacyId;
	}
}
