package com.buddies.pharmaciegarde.modules.pharmacy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Données pour créer une nouvelle assurance")
public class AssuranceCreateDto {

	@Schema(description = "Nom de l'assurance", example = "CNaPS", required = true)
	@NotBlank(message = "Le nom de l'assurance est obligatoire")
	@Size(min = 2, max = 255, message = "Le nom doit contenir entre 2 et 255 caractères")
	private String name;

	@Schema(description = "Description de l'assurance", example = "Caisse Nationale de Prévoyance Sociale")
	@Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
	private String description;

	// Constructeurs
	public AssuranceCreateDto() {
	}

	// Getters et Setters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}

