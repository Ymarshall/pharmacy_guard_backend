package com.buddies.pharmaciegarde.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Données pour l'authentification d'un client par téléphone")
public class ClientLoginDto {

	@Schema(description = "Numéro de téléphone", example = "+225 07 12 34 56 78", required = true)
	@NotBlank(message = "Le numéro de téléphone est obligatoire")
	@Size(min = 8, max = 20, message = "Le numéro de téléphone doit contenir entre 8 et 20 caractères")
	private String phone;

	// Constructeurs
	public ClientLoginDto() {
	}

	public ClientLoginDto(String phone) {
		this.phone = phone;
	}

	// Getters et Setters
	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
}

