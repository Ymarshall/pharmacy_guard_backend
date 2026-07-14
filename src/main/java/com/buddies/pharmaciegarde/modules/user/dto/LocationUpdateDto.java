package com.buddies.pharmaciegarde.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Données pour mettre à jour la localisation d'un client")
public class LocationUpdateDto {

	@Schema(description = "Latitude", example = "-18.8792", required = true)
	@NotNull(message = "La latitude est obligatoire")
	private Double latitude;

	@Schema(description = "Longitude", example = "47.5079", required = true)
	@NotNull(message = "La longitude est obligatoire")
	private Double longitude;

	// Constructeurs
	public LocationUpdateDto() {
	}

	public LocationUpdateDto(Double latitude, Double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	// Getters et Setters
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
}

