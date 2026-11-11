package com.buddies.pharmaciegarde.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse contenant les informations d'un utilisateur")
public class UserResponseDto {

	@Schema(description = "Identifiant unique de l'utilisateur", example = "1")
	private Long id;

	@Schema(description = "Adresse email", example = "user@example.com")
	private String email;

	@Schema(description = "Prénom", example = "Jean")
	private String firstName;

	@Schema(description = "Nom", example = "Dupont")
	private String lastName;

	@Schema(description = "Rôle back-office", example = "ADMIN")
	private String role;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "Jeton JWT, présent uniquement à la connexion")
	private String token;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "Type de jeton", example = "Bearer")
	private String tokenType;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "Durée de validité du jeton en secondes", example = "86400")
	private Long expiresInSeconds;

	// Constructeurs
	public UserResponseDto() {
	}

	public UserResponseDto(Long id, String email, String firstName, String lastName) {
		this.id = id;
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	// Getters et Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
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

