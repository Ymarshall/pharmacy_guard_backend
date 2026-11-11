package com.buddies.pharmaciegarde.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Données pour mettre à jour le mot de passe")
public class PasswordUpdateDto {

	@Schema(description = "Ancien mot de passe", example = "oldPassword123", required = true)
	@NotBlank(message = "L'ancien mot de passe est obligatoire")
	private String oldPassword;

	@Schema(description = "Nouveau mot de passe", example = "newPassword123", required = true)
	@NotBlank(message = "Le nouveau mot de passe est obligatoire")
	@Size(min = 6, max = 72, message = "Le nouveau mot de passe doit contenir entre 6 et 72 caractères")
	private String newPassword;

	// Constructeurs
	public PasswordUpdateDto() {
	}

	public PasswordUpdateDto(String oldPassword, String newPassword) {
		this.oldPassword = oldPassword;
		this.newPassword = newPassword;
	}

	// Getters et Setters
	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}
}

