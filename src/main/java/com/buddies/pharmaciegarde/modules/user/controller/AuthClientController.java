package com.buddies.pharmaciegarde.modules.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buddies.pharmaciegarde.modules.user.dto.ClientLoginDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientLoginResponseDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientRegisterDto;
import com.buddies.pharmaciegarde.modules.user.service.ClientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/client")
@Tag(name = "Client Authentication", description = "API d'authentification pour les clients (app mobile)")
public class AuthClientController {

	private final ClientService clientService;

	public AuthClientController(ClientService clientService) {
		this.clientService = clientService;
	}

	@Operation(
			summary = "Connexion client", 
			description = "Authentifie un client par numéro de téléphone uniquement. " +
					"Retourne les informations du client et les 3 pharmacies de garde les plus proches de sa position pour la période active. " +
					"Si aucune période n'est active, la liste des pharmacies sera vide."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Connexion réussie",
					content = @Content(schema = @Schema(implementation = ClientLoginResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Numéro de téléphone non trouvé, compte désactivé, ou position GPS manquante")
	})
	@PostMapping("/login")
	public ResponseEntity<ClientLoginResponseDto> login(@Valid @RequestBody ClientLoginDto clientLoginDto) {
		ClientLoginResponseDto response = clientService.login(clientLoginDto);
		return ResponseEntity.ok(response);
	}

	@Operation(
			summary = "Inscription client",
			description = "Crée un compte client à partir du seul numéro de téléphone ivoirien. "
					+ "Le compte est actif, de type PHONE, sans rôle ni mot de passe. "
					+ "Retourne le client et un jeton Bearer, comme la connexion. "
					+ "La position GPS se met à jour ensuite avec PUT /api/clients/{id}/location."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Compte créé",
					content = @Content(schema = @Schema(implementation = ClientLoginResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Numéro absent ou non ivoirien"),
			@ApiResponse(responseCode = "409", description = "Ce numéro est déjà inscrit"),
			@ApiResponse(responseCode = "429", description = "Trop de tentatives")
	})
	@PostMapping("/register")
	public ResponseEntity<ClientLoginResponseDto> register(@Valid @RequestBody ClientRegisterDto clientRegisterDto) {
		ClientLoginResponseDto response = clientService.register(clientRegisterDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}

