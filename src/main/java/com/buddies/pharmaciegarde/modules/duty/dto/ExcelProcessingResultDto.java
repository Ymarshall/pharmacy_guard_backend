package com.buddies.pharmaciegarde.modules.duty.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat du traitement d'un fichier Excel")
public class ExcelProcessingResultDto {

	@Schema(description = "ID de la période de garde", example = "1")
	private Long dutyPeriodId;

	@Schema(description = "Statut du traitement", example = "SUCCESS")
	private String status;

	@Schema(description = "Nombre de pharmacies trouvées dans le fichier", example = "15")
	private Integer pharmaciesFound;

	@Schema(description = "Nombre de pharmacies créées", example = "10")
	private Integer pharmaciesCreated;

	@Schema(description = "Nombre de pharmacies mises à jour", example = "5")
	private Integer pharmaciesUpdated;

	@Schema(description = "Nombre de gardes créées", example = "15")
	private Integer dutiesCreated;

	@Schema(description = "Liste des pharmacies traitées")
	private List<ExcelPharmacyDto> pharmacies;

	@Schema(description = "Message d'erreur si échec")
	private String errorMessage;

	// Constructeurs
	public ExcelProcessingResultDto() {
	}

	// Getters et Setters
	public Long getDutyPeriodId() {
		return dutyPeriodId;
	}

	public void setDutyPeriodId(Long dutyPeriodId) {
		this.dutyPeriodId = dutyPeriodId;
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

	public Integer getPharmaciesCreated() {
		return pharmaciesCreated;
	}

	public void setPharmaciesCreated(Integer pharmaciesCreated) {
		this.pharmaciesCreated = pharmaciesCreated;
	}

	public Integer getPharmaciesUpdated() {
		return pharmaciesUpdated;
	}

	public void setPharmaciesUpdated(Integer pharmaciesUpdated) {
		this.pharmaciesUpdated = pharmaciesUpdated;
	}

	public Integer getDutiesCreated() {
		return dutiesCreated;
	}

	public void setDutiesCreated(Integer dutiesCreated) {
		this.dutiesCreated = dutiesCreated;
	}

	public List<ExcelPharmacyDto> getPharmacies() {
		return pharmacies;
	}

	public void setPharmacies(List<ExcelPharmacyDto> pharmacies) {
		this.pharmacies = pharmacies;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
