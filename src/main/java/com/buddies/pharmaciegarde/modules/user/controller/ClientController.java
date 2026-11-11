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

import com.buddies.pharmaciegarde.modules.user.dto.ClientCreateDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientResponseDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientStatusUpdateDto;
import com.buddies.pharmaciegarde.modules.user.dto.LocationUpdateDto;
import com.buddies.pharmaciegarde.modules.user.service.ClientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "API de gestion des clients (app mobile)")
public class ClientController {

	private final ClientService clientService;

	public ClientController(ClientService clientService) {
		this.clientService = clientService;
	}

	@Operation(
			summary = "Créer un nouveau client", 
			description = "Crée un nouveau client avec le numéro de téléphone et la position GPS. " +
					"Le client sera automatiquement créé comme actif et pourra se connecter avec son numéro de téléphone."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Client créé avec succès",
					content = @Content(schema = @Schema(implementation = ClientResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides (téléphone manquant, coordonnées invalides) ou client déjà existant"),
			@ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
	})
	@PostMapping
	public ResponseEntity<ClientResponseDto> create(@Valid @RequestBody ClientCreateDto clientCreateDto) {
		ClientResponseDto createdClient = clientService.create(clientCreateDto);
		return new ResponseEntity<>(createdClient, HttpStatus.CREATED);
	}

	@Operation(summary = "Récupérer un client par son ID", description = "Retourne les détails d'un client spécifique")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Client trouvé",
					content = @Content(schema = @Schema(implementation = ClientResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Client non trouvé")
	})
	@GetMapping("/{id}")
	public ResponseEntity<ClientResponseDto> getById(
			@Parameter(description = "ID du client", required = true) @PathVariable Long id) {
		ClientResponseDto client = clientService.getById(id);
		return ResponseEntity.ok(client);
	}

	@Operation(summary = "Lister tous les clients", description = "Retourne la liste de tous les clients")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des clients récupérée avec succès")
	})
	@GetMapping
	public ResponseEntity<?> getAll(
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@Parameter(description = "Recherche téléphone, email, prénom, nom") @RequestParam(required = false) String search,
			@Parameter(description = "Tri : id, phone, firstName, lastName, email") @RequestParam(required = false) String sort) {
		if (page != null || size != null) {
			return ResponseEntity.ok(clientService.getPage(page == null ? 0 : page, size == null ? 20 : size, search, sort));
		}
		if ((search != null && !search.isBlank()) || (sort != null && !sort.isBlank())) {
			return ResponseEntity.ok(clientService.list(search, sort));
		}
		return ResponseEntity.ok(clientService.getAll());
	}

	@Operation(summary = "Mettre à jour un client", description = "Met à jour les informations d'un client existant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Client mis à jour avec succès",
					content = @Content(schema = @Schema(implementation = ClientResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides"),
			@ApiResponse(responseCode = "404", description = "Client non trouvé")
	})
	@PutMapping("/{id}")
	public ResponseEntity<ClientResponseDto> update(
			@Parameter(description = "ID du client", required = true) @PathVariable Long id,
			@Valid @RequestBody ClientCreateDto clientCreateDto) {
		ClientResponseDto updatedClient = clientService.update(id, clientCreateDto);
		return ResponseEntity.ok(updatedClient);
	}

	@Operation(summary = "Supprimer un client", description = "Supprime un client de la base de données")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Client supprimé avec succès"),
			@ApiResponse(responseCode = "404", description = "Client non trouvé")
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@Parameter(description = "ID du client", required = true) @PathVariable Long id) {
		clientService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Mettre à jour la localisation", description = "Met à jour la localisation (latitude/longitude) d'un client")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Localisation mise à jour avec succès",
					content = @Content(schema = @Schema(implementation = ClientResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides"),
			@ApiResponse(responseCode = "404", description = "Client non trouvé")
	})
	@PutMapping("/{id}/location")
	public ResponseEntity<ClientResponseDto> updateLocation(
			@Parameter(description = "ID du client", required = true) @PathVariable Long id,
			@Valid @RequestBody LocationUpdateDto locationUpdateDto) {
		ClientResponseDto updatedClient = clientService.updateLocation(id, locationUpdateDto);
		return ResponseEntity.ok(updatedClient);
	}

	@Operation(
			summary = "Activer ou désactiver un client",
			description = "Met à jour le statut actif d'un client. Si isActive = false, le client ne pourra plus se connecter même avec des credentials valides. Si isActive = true, le client pourra se connecter normalement."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Statut mis à jour avec succès",
					content = @Content(schema = @Schema(implementation = ClientResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides"),
			@ApiResponse(responseCode = "404", description = "Client non trouvé")
	})
	@PutMapping("/{id}/status")
	public ResponseEntity<ClientResponseDto> updateStatus(
			@Parameter(description = "ID du client", required = true) @PathVariable Long id,
			@Valid @RequestBody ClientStatusUpdateDto statusUpdateDto) {
		ClientResponseDto updatedClient = clientService.updateStatus(id, statusUpdateDto.getIsActive());
		return ResponseEntity.ok(updatedClient);
	}

	@Operation(
			summary = "Activer un client",
			description = "Active un client, permettant ainsi sa connexion avec ses credentials valides."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Client activé avec succès",
					content = @Content(schema = @Schema(implementation = ClientResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Client non trouvé")
	})
	@PostMapping("/{id}/activate")
	public ResponseEntity<ClientResponseDto> activate(
			@Parameter(description = "ID du client à activer", required = true) @PathVariable Long id) {
		ClientResponseDto updatedClient = clientService.updateStatus(id, true);
		return ResponseEntity.ok(updatedClient);
	}

	@Operation(
			summary = "Désactiver un client",
			description = "Désactive un client, empêchant ainsi toute connexion même avec des credentials valides."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Client désactivé avec succès",
					content = @Content(schema = @Schema(implementation = ClientResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Client non trouvé")
	})
	@PostMapping("/{id}/deactivate")
	public ResponseEntity<ClientResponseDto> deactivate(
			@Parameter(description = "ID du client à désactiver", required = true) @PathVariable Long id) {
		ClientResponseDto updatedClient = clientService.updateStatus(id, false);
		return ResponseEntity.ok(updatedClient);
	}
}

