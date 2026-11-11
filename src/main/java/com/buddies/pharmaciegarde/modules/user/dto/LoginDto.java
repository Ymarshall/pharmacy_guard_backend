package com.buddies.pharmaciegarde.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Données pour l'authentification")
public class LoginDto {

	@Schema(description = "Adresse email", example = "user@example.com", required = true)
	@NotBlank(message = "L'email est obligatoire")
	@Email(message = "L'email doit être valide")
	private String email;

	@Schema(description = "Mot de passe", example = "password123", required = true)
	@NotBlank(message = "Le mot de passe est obligatoire")
	@Size(max = 72, message = "Le mot de passe ne doit pas dépasser 72 caractères")
	private String password;

	// Constructeurs
	public LoginDto() {
	}

	public LoginDto(String email, String password) {
		this.email = email;
		this.password = password;
	}

	// Getters et Setters
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}

