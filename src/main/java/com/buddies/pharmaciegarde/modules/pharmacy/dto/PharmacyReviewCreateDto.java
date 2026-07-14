package com.buddies.pharmaciegarde.modules.pharmacy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Données pour créer ou modifier un commentaire et une note sur une pharmacie")
public class PharmacyReviewCreateDto {

	@Schema(description = "ID de la pharmacie", example = "1", required = true)
	@NotNull(message = "L'ID de la pharmacie est obligatoire")
	private Long pharmacyId;

	@Schema(description = "ID du client", example = "1", required = true)
	@NotNull(message = "L'ID du client est obligatoire")
	private Long clientId;

	@Schema(description = "Note de 1 à 5 étoiles", example = "5", required = true)
	@NotNull(message = "La note est obligatoire")
	@Min(value = 1, message = "La note doit être au moins 1")
	@Max(value = 5, message = "La note doit être au plus 5")
	private Integer rating;

	@Schema(description = "Commentaire (optionnel, max 1000 caractères)", example = "Excellent service, personnel très professionnel")
	@Size(max = 1000, message = "Le commentaire ne doit pas dépasser 1000 caractères")
	private String comment;

	// Constructeurs
	public PharmacyReviewCreateDto() {
	}

	public PharmacyReviewCreateDto(Long pharmacyId, Long clientId, Integer rating, String comment) {
		this.pharmacyId = pharmacyId;
		this.clientId = clientId;
		this.rating = rating;
		this.comment = comment;
	}

	// Getters et Setters
	public Long getPharmacyId() {
		return pharmacyId;
	}

	public void setPharmacyId(Long pharmacyId) {
		this.pharmacyId = pharmacyId;
	}

	public Long getClientId() {
		return clientId;
	}

	public void setClientId(Long clientId) {
		this.clientId = clientId;
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
}
