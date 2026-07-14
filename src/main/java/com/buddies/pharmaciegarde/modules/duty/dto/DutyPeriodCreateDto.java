package com.buddies.pharmaciegarde.modules.duty.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "DTO pour créer une période de garde")
public class DutyPeriodCreateDto {

	@Schema(description = "Date de début de la garde", example = "2026-01-09", required = true)
	@NotNull(message = "La date de début est obligatoire")
	private LocalDate startDate;

	@Schema(description = "Date de fin de la garde", example = "2026-01-15", required = true)
	@NotNull(message = "La date de fin est obligatoire")
	private LocalDate endDate;

	@Schema(description = "Marquer comme garde courante", example = "true")
	private Boolean isCurrent = false;

	@Schema(description = "Description de la période", example = "Garde de la semaine du 9 au 15 janvier 2026")
	private String description;

	// Constructeurs
	public DutyPeriodCreateDto() {
	}

	// Getters et Setters
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}

