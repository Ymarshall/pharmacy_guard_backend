package com.buddies.pharmaciegarde.modules.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO pour retourner les informations d'une zone")
public class ZoneResponseDto {

	@Schema(description = "ID de la zone", example = "1")
	private Long id;

	@Schema(description = "Nom de la zone (ville ou commune)", example = "Abidjan")
	private String name;

	@Schema(description = "Type de zone", example = "VILLE")
	private String type;

	@Schema(description = "Région de Côte d'Ivoire", example = "Lagunes")
	private String region;

	@Schema(description = "Département", example = "Abidjan")
	private String departement;

	@Schema(description = "Zone active", example = "true")
	private Boolean isActive;

	// Constructeurs
	public ZoneResponseDto() {
	}

	// Getters et Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getDepartement() {
		return departement;
	}

	public void setDepartement(String departement) {
		this.departement = departement;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}
}

