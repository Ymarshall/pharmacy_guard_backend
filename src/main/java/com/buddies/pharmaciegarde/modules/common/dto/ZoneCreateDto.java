package com.buddies.pharmaciegarde.modules.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO pour créer une zone (ville ou commune)")
public class ZoneCreateDto {

	@Schema(description = "Nom de la zone (ville ou commune)", example = "Abidjan", required = true)
	@NotBlank(message = "Le nom de la zone est obligatoire")
	@Size(max = 255, message = "Le nom de la zone ne doit pas dépasser 255 caractères")
	private String name;

	@Schema(description = "Type de zone", example = "VILLE", allowableValues = {"VILLE", "COMMUNE"})
	@Size(max = 100, message = "Le type ne doit pas dépasser 100 caractères")
	private String type;

	@Schema(description = "Région de Côte d'Ivoire", example = "Lagunes")
	@Size(max = 100, message = "La région ne doit pas dépasser 100 caractères")
	private String region;

	@Schema(description = "Département", example = "Abidjan")
	@Size(max = 100, message = "Le département ne doit pas dépasser 100 caractères")
	private String departement;

	@Schema(description = "Zone active", example = "true")
	private Boolean isActive = true;

	// Constructeurs
	public ZoneCreateDto() {
	}

	// Getters et Setters
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

