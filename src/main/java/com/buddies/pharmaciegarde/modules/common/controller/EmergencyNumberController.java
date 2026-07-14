package com.buddies.pharmaciegarde.modules.common.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.buddies.pharmaciegarde.modules.common.dto.EmergencyNumberCreateDto;
import com.buddies.pharmaciegarde.modules.common.dto.EmergencyNumberResponseDto;
import com.buddies.pharmaciegarde.modules.common.service.EmergencyNumberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/emergency-numbers")
@Tag(name = "Emergency Numbers", description = "API de gestion des numéros d'urgence")
public class EmergencyNumberController {

	private final EmergencyNumberService emergencyNumberService;

	public EmergencyNumberController(EmergencyNumberService emergencyNumberService) {
		this.emergencyNumberService = emergencyNumberService;
	}

	@Operation(summary = "Créer un numéro d'urgence", description = "Crée un nouveau numéro d'urgence")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Numéro d'urgence créé avec succès",
					content = @Content(schema = @Schema(implementation = EmergencyNumberResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides ou numéro déjà existant"),
			@ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
	})
	@PostMapping
	public ResponseEntity<EmergencyNumberResponseDto> create(@Valid @RequestBody EmergencyNumberCreateDto emergencyNumberCreateDto) {
		EmergencyNumberResponseDto createdEmergencyNumber = emergencyNumberService.create(emergencyNumberCreateDto);
		return new ResponseEntity<>(createdEmergencyNumber, HttpStatus.CREATED);
	}

	@Operation(summary = "Récupérer un numéro d'urgence par son ID", description = "Retourne les détails d'un numéro d'urgence spécifique")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Numéro d'urgence trouvé",
					content = @Content(schema = @Schema(implementation = EmergencyNumberResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Numéro d'urgence non trouvé")
	})
	@GetMapping("/{id}")
	public ResponseEntity<EmergencyNumberResponseDto> getById(
			@Parameter(description = "ID du numéro d'urgence", required = true) @PathVariable Long id) {
		EmergencyNumberResponseDto emergencyNumber = emergencyNumberService.getById(id);
		return ResponseEntity.ok(emergencyNumber);
	}

	@Operation(summary = "Lister tous les numéros d'urgence", description = "Retourne la liste de tous les numéros d'urgence")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des numéros d'urgence récupérée avec succès")
	})
	@GetMapping
	public ResponseEntity<List<EmergencyNumberResponseDto>> getAll() {
		return ResponseEntity.ok(emergencyNumberService.getAll());
	}

	@Operation(summary = "Mettre à jour un numéro d'urgence", description = "Met à jour les informations d'un numéro d'urgence existant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Numéro d'urgence mis à jour avec succès",
					content = @Content(schema = @Schema(implementation = EmergencyNumberResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides"),
			@ApiResponse(responseCode = "404", description = "Numéro d'urgence non trouvé")
	})
	@PutMapping("/{id}")
	public ResponseEntity<EmergencyNumberResponseDto> update(
			@Parameter(description = "ID du numéro d'urgence", required = true) @PathVariable Long id,
			@Valid @RequestBody EmergencyNumberCreateDto emergencyNumberCreateDto) {
		EmergencyNumberResponseDto updatedEmergencyNumber = emergencyNumberService.update(id, emergencyNumberCreateDto);
		return ResponseEntity.ok(updatedEmergencyNumber);
	}

	@Operation(summary = "Supprimer un numéro d'urgence", description = "Supprime un numéro d'urgence de la base de données")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Numéro d'urgence supprimé avec succès"),
			@ApiResponse(responseCode = "404", description = "Numéro d'urgence non trouvé")
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@Parameter(description = "ID du numéro d'urgence", required = true) @PathVariable Long id) {
		emergencyNumberService.delete(id);
		return ResponseEntity.noContent().build();
	}
}

