package com.buddies.pharmaciegarde.modules.user.dto;

import com.buddies.pharmaciegarde.common.validation.IvorianPhone;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Inscription publique d'un client. Seul le téléphone est accepté.")
public class ClientRegisterDto {

	@Schema(description = "Numéro de téléphone ivoirien", example = "+225 07 12 34 56 78", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Le numéro de téléphone est obligatoire")
	@Size(min = 8, max = 20, message = "Le numéro de téléphone doit contenir entre 8 et 20 caractères")
	@IvorianPhone
	private String phone;

	public ClientRegisterDto() {
	}

	public ClientRegisterDto(String phone) {
		this.phone = phone;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
}
