package com.buddies.pharmaciegarde.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Données pour créer un nouvel utilisateur")
public class UserCreateDto {

	@Schema(description = "Adresse email", example = "user@example.com", required = true)
	@NotBlank(message = "L'email est obligatoire")
	@Email(message = "L'email doit être valide")
	private String email;

	@Schema(description = "Mot de passe", example = "password123", required = true)
	@NotBlank(message = "Le mot de passe est obligatoire")
	@Size(min = 6, max = 72, message = "Le mot de passe doit contenir entre 6 et 72 caractères")
	private String password;

	@Schema(description = "Prénom", example = "Jean", required = true)
	@NotBlank(message = "Le prénom est obligatoire")
	@Size(min = 2, max = 100, message = "Le prénom doit contenir entre 2 et 100 caractères")
	private String firstName;

	@Schema(description = "Nom", example = "Dupont", required = true)
	@NotBlank(message = "Le nom est obligatoire")
	@Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
	private String lastName;

	@Schema(description = "Rôle optionnel : ADMIN ou OPERATOR. OPERATOR par défaut.", example = "OPERATOR")
	private String role;

	// Constructeurs
	public UserCreateDto() {
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
}

