package com.buddies.pharmaciegarde.modules.pharmacy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Critères de recherche de pharmacie")
public class PharmacySearchDto {

	@Schema(description = "Nom de la pharmacie (recherche par début de nom)", example = "Pharmacie")
	private String name;

	@Schema(description = "Ville", example = "Antananarivo")
	private String city;

	@Schema(description = "Commune", example = "Analakely")
	private String commune;

	@Schema(description = "Quartier/District", example = "Analamanga")
	private String district;

	// Constructeurs
	public PharmacySearchDto() {
	}

	// Getters et Setters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCommune() {
		return commune;
	}

	public void setCommune(String commune) {
		this.commune = commune;
	}

	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}
}

