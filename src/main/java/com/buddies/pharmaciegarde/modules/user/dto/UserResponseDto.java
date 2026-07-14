package com.buddies.pharmaciegarde.modules.user.dto;

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
}

