package com.buddies.pharmaciegarde.modules.duty.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "duty_periods")
public class DutyPeriod {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "La date de début est obligatoire")
	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@NotNull(message = "La date de fin est obligatoire")
	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "is_current", nullable = false)
	private Boolean isCurrent = false; // Garde courante/active

	@Column(name = "description", columnDefinition = "TEXT")
	private String description; // Description optionnelle de la période

	// Constructeurs
	public DutyPeriod() {
	}

	public DutyPeriod(LocalDate startDate, LocalDate endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.isCurrent = false;
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

