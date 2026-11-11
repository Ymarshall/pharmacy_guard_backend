package com.buddies.pharmaciegarde.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse de connexion d'un client")
public class ClientLoginResponseDto {

	@Schema(description = "Informations du client")
	private ClientResponseDto client;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "Jeton JWT")
	private String token;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "Type de jeton", example = "Bearer")
	private String tokenType;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "Durée de validité du jeton en secondes")
	private Long expiresInSeconds;

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

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public Long getExpiresInSeconds() {
		return expiresInSeconds;
	}

	public void setExpiresInSeconds(Long expiresInSeconds) {
		this.expiresInSeconds = expiresInSeconds;
	}
}

