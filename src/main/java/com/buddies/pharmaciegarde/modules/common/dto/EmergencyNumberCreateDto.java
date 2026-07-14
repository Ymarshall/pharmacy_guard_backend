package com.buddies.pharmaciegarde.modules.common.dto;

import com.buddies.pharmaciegarde.modules.common.model.EmergencyNumber.EmergencyCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Données pour créer un numéro d'urgence")
public class EmergencyNumberCreateDto {

	@Schema(description = "Nom du service d'urgence", example = "Pompiers", required = true)
	@NotBlank(message = "Le nom est obligatoire")
	@Size(min = 2, max = 255, message = "Le nom doit contenir entre 2 et 255 caractères")
	private String name;

	@Schema(description = "Numéro d'urgence", example = "117", required = true)
	@NotBlank(message = "Le numéro est obligatoire")
	@Size(min = 2, max = 30, message = "Le numéro doit contenir entre 2 et 30 caractères")
	private String number;

	@Schema(description = "URL ou nom de l'icône", example = "fire-icon.png")
	@Size(max = 500, message = "L'icône ne doit pas dépasser 500 caractères")
	private String icon;

	@Schema(description = "Description du service", example = "Service d'incendie et de secours")
	@Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
	private String description;

	@Schema(description = "Couleur (code hex ou nom)", example = "#FF0000 ou red")
	@Size(max = 50, message = "La couleur ne doit pas dépasser 50 caractères")
	private String color;

	@Schema(description = "Catégorie du numéro d'urgence", example = "medical", required = true, 
			allowableValues = {"medical", "fire", "police", "bloodBank", "other"})
	@NotNull(message = "La catégorie est obligatoire")
	private EmergencyCategory category;

	// Constructeurs
	public EmergencyNumberCreateDto() {
	}

	// Getters et Setters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public EmergencyCategory getCategory() {
		return category;
	}

	public void setCategory(EmergencyCategory category) {
		this.category = category;
	}
}

