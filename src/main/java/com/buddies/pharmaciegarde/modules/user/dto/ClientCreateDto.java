package com.buddies.pharmaciegarde.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Données pour créer un nouveau client")
public class ClientCreateDto {

	@Schema(description = "Numéro de téléphone", example = "+225 07 12 34 56 78", required = true)
	@NotNull(message = "Le numéro de téléphone est obligatoire")
	@Size(min = 8, max = 20, message = "Le numéro de téléphone doit contenir entre 8 et 20 caractères")
	private String phone;

	@Schema(description = "Latitude GPS", example = "5.3600", required = true)
	@NotNull(message = "La latitude est obligatoire")
	private Double latitude;

	@Schema(description = "Longitude GPS", example = "-4.0083", required = true)
	@NotNull(message = "La longitude est obligatoire")
	private Double longitude;

	@Schema(description = "Indique si le client peut se connecter (par défaut: true)", example = "true")
	private Boolean isActive;

	// Constructeurs
	public ClientCreateDto() {
	}

	// Getters et Setters
	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
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
}

