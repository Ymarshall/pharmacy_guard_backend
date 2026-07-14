package com.buddies.pharmaciegarde.modules.user.dto;

import java.time.LocalDateTime;

import com.buddies.pharmaciegarde.modules.user.model.Client.ProviderType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse contenant les informations d'un client")
public class ClientResponseDto {

	@Schema(description = "Identifiant unique du client", example = "1")
	private Long id;

	@Schema(description = "Adresse email", example = "client@example.com")
	private String email;

	@Schema(description = "Numéro de téléphone", example = "+261 34 12 345 67")
	private String phone;

	@Schema(description = "Prénom", example = "Jean")
	private String firstName;

	@Schema(description = "Nom", example = "Dupont")
	private String lastName;

	@Schema(description = "Type de provider", example = "GOOGLE")
	private ProviderType providerType;

	@Schema(description = "Latitude", example = "-18.8792")
	private Double latitude;

	@Schema(description = "Longitude", example = "47.5079")
	private Double longitude;

	@Schema(description = "Indique si le client peut se connecter", example = "true")
	private Boolean isActive;

	@Schema(description = "Date de création")
	private LocalDateTime createdAt;

	@Schema(description = "Date de mise à jour")
	private LocalDateTime updatedAt;

	// Constructeurs
	public ClientResponseDto() {
	}

	// Getters et Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public ProviderType getProviderType() {
		return providerType;
	}

	public void setProviderType(ProviderType providerType) {
		this.providerType = providerType;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
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

