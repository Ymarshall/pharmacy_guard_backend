package com.buddies.pharmaciegarde.modules.pharmacy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pharmacie avec sa distance par rapport à une position")
public class PharmacyWithDistanceDto extends PharmacyResponseDto {

	@Schema(description = "Distance en kilomètres", example = "2.5")
	private Double distanceInKm;

	// Constructeurs
	public PharmacyWithDistanceDto() {
		super();
	}

	// Getters et Setters
	public Double getDistanceInKm() {
		return distanceInKm;
	}

	public void setDistanceInKm(Double distanceInKm) {
		this.distanceInKm = distanceInKm;
	}
}

