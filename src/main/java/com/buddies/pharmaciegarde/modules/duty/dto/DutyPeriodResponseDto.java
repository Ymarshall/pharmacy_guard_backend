package com.buddies.pharmaciegarde.modules.duty.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO pour retourner les informations d'une période de garde")
public class DutyPeriodResponseDto {

	@Schema(description = "ID de la période de garde", example = "1")
	private Long id;

	@Schema(description = "Date de début de la garde", example = "2026-01-09")
	private LocalDate startDate;

	@Schema(description = "Date de fin de la garde", example = "2026-01-15")
	private LocalDate endDate;

	@Schema(description = "Garde courante/active", example = "true")
	private Boolean isCurrent;

	@Schema(description = "Description de la période", example = "Garde de la semaine du 9 au 15 janvier 2026")
	private String description;

	// Constructeurs
	public DutyPeriodResponseDto() {
	}

	// Getters et Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}

