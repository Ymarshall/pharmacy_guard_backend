package com.buddies.pharmaciegarde.modules.pharmacy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse contenant les informations d'une assurance")
public class AssuranceResponseDto {

	@Schema(description = "Identifiant unique de l'assurance", example = "1")
	private Long id;

	@Schema(description = "Nom de l'assurance", example = "CNaPS")
	private String name;

	@Schema(description = "Description de l'assurance", example = "Caisse Nationale de Prévoyance Sociale")
	private String description;

	// Constructeurs
	public AssuranceResponseDto() {
	}

	public AssuranceResponseDto(Long id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}

