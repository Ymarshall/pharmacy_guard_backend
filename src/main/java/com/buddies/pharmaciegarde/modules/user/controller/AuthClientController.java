package com.buddies.pharmaciegarde.modules.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buddies.pharmaciegarde.modules.user.dto.ClientLoginDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientLoginResponseDto;
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
}

