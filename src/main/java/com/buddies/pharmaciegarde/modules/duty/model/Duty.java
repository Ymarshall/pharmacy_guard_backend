package com.buddies.pharmaciegarde.modules.duty.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy;

@Entity
@Table(name = "duties")
@DynamicInsert // N'insère que les colonnes non-null
@DynamicUpdate // Met à jour uniquement les colonnes modifiées
public class Duty {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pharmacy_id", nullable = false)
	@NotNull(message = "La pharmacie est obligatoire")
	private Pharmacy pharmacy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "duty_period_id", nullable = false)
	@NotNull(message = "La période de garde est obligatoire")
	private DutyPeriod dutyPeriod; // Période de garde

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "duty_file_id")
	private DutyFile dutyFile; // Référence au fichier PDF source

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private DutyStatus status = DutyStatus.ACTIVE; // Statut de la garde (ACTIVE par défaut)

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	// Constructeurs
	public Duty() {
		this.createdAt = LocalDateTime.now();
	}

	public Duty(Pharmacy pharmacy, DutyPeriod dutyPeriod) {
		this();
		this.pharmacy = pharmacy;
		this.dutyPeriod = dutyPeriod;
	}

	// Méthode pour mettre à jour la date de modification
	@jakarta.persistence.PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	// Getters et Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Pharmacy getPharmacy() {
		return pharmacy;
	}

	public void setPharmacy(Pharmacy pharmacy) {
		this.pharmacy = pharmacy;
	}

	public DutyPeriod getDutyPeriod() {
		return dutyPeriod;
	}

	public void setDutyPeriod(DutyPeriod dutyPeriod) {
		this.dutyPeriod = dutyPeriod;
	}

	public DutyFile getDutyFile() {
		return dutyFile;
	}

	public void setDutyFile(DutyFile dutyFile) {
		this.dutyFile = dutyFile;
	}

	public DutyStatus getStatus() {
		return status;
	}

	public void setStatus(DutyStatus status) {
		this.status = status;
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
}

