package com.buddies.pharmaciegarde.modules.common.dto;

import com.buddies.pharmaciegarde.modules.common.model.EmergencyNumber.EmergencyCategory;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse contenant les informations d'un numéro d'urgence")
public class EmergencyNumberResponseDto {

	@Schema(description = "Identifiant unique du numéro d'urgence", example = "1")
	private Long id;

	@Schema(description = "Nom du service d'urgence", example = "Pompiers")
	private String name;

	@Schema(description = "Numéro d'urgence", example = "117")
	private String number;

	@Schema(description = "URL ou nom de l'icône", example = "fire-icon.png")
	private String icon;

	@Schema(description = "Description du service", example = "Service d'incendie et de secours")
	private String description;

	@Schema(description = "Couleur (code hex ou nom)", example = "#FF0000")
	private String color;

	@Schema(description = "Catégorie du numéro d'urgence", example = "medical")
	private EmergencyCategory category;

	// Constructeurs
	public EmergencyNumberResponseDto() {
	}

	public EmergencyNumberResponseDto(Long id, String name, String number, String icon, String description, String color, EmergencyCategory category) {
		this.id = id;
		this.name = name;
		this.number = number;
		this.icon = icon;
		this.description = description;
		this.color = color;
		this.category = category;
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

