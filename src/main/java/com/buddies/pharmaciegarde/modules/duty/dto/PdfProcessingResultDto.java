package com.buddies.pharmaciegarde.modules.duty.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat du traitement d'un fichier PDF")
public class PdfProcessingResultDto {

	@Schema(description = "ID du fichier traité", example = "1")
	private Long dutyFileId;

	@Schema(description = "Statut du traitement", example = "COMPLETED")
	private String status;

	@Schema(description = "Nombre de pharmacies trouvées", example = "15")
	private Integer pharmaciesFound;

	@Schema(description = "Nombre de gardes créées", example = "15")
	private Integer dutiesCreated;

	@Schema(description = "Liste complète des pharmacies extraites avec toutes les informations (zone, nom, pharmacien, téléphones, localisation)")
	private java.util.List<com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto> pharmacies;

	@Schema(description = "Message d'erreur si échec")
	private String errorMessage;

	// Constructeurs
	public PdfProcessingResultDto() {
	}

	// Getters et Setters
	public Long getDutyFileId() {
		return dutyFileId;
	}

	public void setDutyFileId(Long dutyFileId) {
		this.dutyFileId = dutyFileId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getPharmaciesFound() {
		return pharmaciesFound;
	}

	public void setPharmaciesFound(Integer pharmaciesFound) {
		this.pharmaciesFound = pharmaciesFound;
	}

	public Integer getDutiesCreated() {
		return dutiesCreated;
	}

	public void setDutiesCreated(Integer dutiesCreated) {
		this.dutiesCreated = dutiesCreated;
	}

	public java.util.List<com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto> getPharmacies() {
		return pharmacies;
	}

	public void setPharmacies(
			java.util.List<com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto> pharmacies) {
		this.pharmacies = pharmacies;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}

