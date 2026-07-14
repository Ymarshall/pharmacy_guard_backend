package com.buddies.pharmaciegarde.modules.pharmacy.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse contenant les informations d'un commentaire et d'une note sur une pharmacie")
public class PharmacyReviewResponseDto {

	@Schema(description = "Identifiant unique du commentaire", example = "1")
	private Long id;

	@Schema(description = "ID de la pharmacie", example = "1")
	private Long pharmacyId;

	@Schema(description = "Nom de la pharmacie", example = "Pharmacie Centrale")
	private String pharmacyName;

	@Schema(description = "ID du client", example = "1")
	private Long clientId;

	@Schema(description = "Nom du client", example = "Jean Dupont")
	private String clientName;

	@Schema(description = "Note de 1 à 5 étoiles", example = "5")
	private Integer rating;

	@Schema(description = "Commentaire", example = "Excellent service, personnel très professionnel")
	private String comment;

	@Schema(description = "Date de création")
	private LocalDateTime createdAt;

	@Schema(description = "Date de modification")
	private LocalDateTime updatedAt;

	// Constructeurs
	public PharmacyReviewResponseDto() {
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

	public Long getClientId() {
		return clientId;
	}

	public void setClientId(Long clientId) {
		this.clientId = clientId;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public Integer getRating() {
		return rating;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
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
