package com.buddies.pharmaciegarde.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Données pour mettre à jour le statut actif d'un client")
public class ClientStatusUpdateDto {

	@Schema(description = "Indique si le client peut se connecter", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Le statut actif est obligatoire")
	private Boolean isActive;

	// Constructeurs
	public ClientStatusUpdateDto() {
	}

	public ClientStatusUpdateDto(Boolean isActive) {
		this.isActive = isActive;
	}

	// Getters et Setters
	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}
}

