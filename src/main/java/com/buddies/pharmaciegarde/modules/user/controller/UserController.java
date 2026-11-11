package com.buddies.pharmaciegarde.modules.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buddies.pharmaciegarde.modules.user.dto.PasswordUpdateDto;
import com.buddies.pharmaciegarde.modules.user.dto.UserCreateDto;
import com.buddies.pharmaciegarde.modules.user.dto.UserResponseDto;
import com.buddies.pharmaciegarde.modules.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "API de gestion des utilisateurs")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@Operation(summary = "Créer un nouvel utilisateur", description = "Crée un nouvel utilisateur avec les informations fournies")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès",
					content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides ou utilisateur déjà existant"),
			@ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
	})
	@PostMapping
	public ResponseEntity<UserResponseDto> create(@Valid @RequestBody UserCreateDto userCreateDto) {
		UserResponseDto createdUser = userService.create(userCreateDto);
		return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
	}

	@Operation(summary = "Récupérer un utilisateur par son ID", description = "Retourne les détails d'un utilisateur spécifique")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Utilisateur trouvé",
					content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
	})
	@GetMapping("/{id}")
	public ResponseEntity<UserResponseDto> getById(
			@Parameter(description = "ID de l'utilisateur", required = true) @PathVariable Long id) {
		UserResponseDto user = userService.getById(id);
		return ResponseEntity.ok(user);
	}

	@Operation(summary = "Lister tous les utilisateurs", description = "Retourne la liste de tous les utilisateurs")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des utilisateurs récupérée avec succès")
	})
	@GetMapping
	public ResponseEntity<?> getAll(
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		if (page != null || size != null) {
			return ResponseEntity.ok(userService.getPage(page == null ? 0 : page, size == null ? 20 : size));
		}
		return ResponseEntity.ok(userService.getAll());
	}

	@Operation(summary = "Mettre à jour un utilisateur", description = "Met à jour les informations d'un utilisateur existante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Utilisateur mis à jour avec succès",
					content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides"),
			@ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
	})
	@PutMapping("/{id}")
	public ResponseEntity<UserResponseDto> update(
			@Parameter(description = "ID de l'utilisateur", required = true) @PathVariable Long id,
			@Valid @RequestBody UserCreateDto userCreateDto) {
		UserResponseDto updatedUser = userService.update(id, userCreateDto);
		return ResponseEntity.ok(updatedUser);
	}

	@Operation(summary = "Supprimer un utilisateur", description = "Supprime un utilisateur de la base de données")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Utilisateur supprimé avec succès"),
			@ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@Parameter(description = "ID de l'utilisateur", required = true) @PathVariable Long id) {
		userService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Mettre à jour le mot de passe", description = "Met à jour le mot de passe d'un utilisateur")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Mot de passe mis à jour avec succès",
					content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Ancien mot de passe incorrect"),
			@ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
	})
	@PutMapping("/{id}/password")
	public ResponseEntity<UserResponseDto> updatePassword(
			@Parameter(description = "ID de l'utilisateur", required = true) @PathVariable Long id,
			@Valid @RequestBody PasswordUpdateDto passwordUpdateDto) {
		UserResponseDto updatedUser = userService.updatePassword(id, passwordUpdateDto);
		return ResponseEntity.ok(updatedUser);
	}
}

