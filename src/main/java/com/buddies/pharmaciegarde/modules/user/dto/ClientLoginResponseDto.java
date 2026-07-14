package com.buddies.pharmaciegarde.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse de connexion d'un client")
public class ClientLoginResponseDto {

	@Schema(description = "Informations du client")
	private ClientResponseDto client;

	// Constructeurs
	public ClientLoginResponseDto() {
	}

	public ClientLoginResponseDto(ClientResponseDto client) {
		this.client = client;
	}

	// Getters et Setters
	public ClientResponseDto getClient() {
		return client;
	}

	public void setClient(ClientResponseDto client) {
		this.client = client;
	}
}

