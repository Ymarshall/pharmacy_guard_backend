package com.buddies.pharmaciegarde.modules.pharmacy.controller;

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

import com.buddies.pharmaciegarde.modules.pharmacy.dto.AssuranceCreateDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.AssuranceResponseDto;
import com.buddies.pharmaciegarde.modules.pharmacy.service.AssuranceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/assurances")
@Tag(name = "Assurances", description = "API de gestion des assurances")
public class AssuranceController {

	private final AssuranceService assuranceService;

	public AssuranceController(AssuranceService assuranceService) {
		this.assuranceService = assuranceService;
	}

	@Operation(summary = "Créer une nouvelle assurance", description = "Crée une nouvelle assurance avec les informations fournies")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Assurance créée avec succès",
					content = @Content(schema = @Schema(implementation = AssuranceResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides ou assurance déjà existante"),
			@ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
	})
	@PostMapping
	public ResponseEntity<AssuranceResponseDto> create(@Valid @RequestBody AssuranceCreateDto assuranceCreateDto) {
		AssuranceResponseDto createdAssurance = assuranceService.create(assuranceCreateDto);
		return new ResponseEntity<>(createdAssurance, HttpStatus.CREATED);
	}

	@Operation(summary = "Récupérer une assurance par son ID", description = "Retourne les détails d'une assurance spécifique")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Assurance trouvée",
					content = @Content(schema = @Schema(implementation = AssuranceResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Assurance non trouvée")
	})
	@GetMapping("/{id}")
	public ResponseEntity<AssuranceResponseDto> getById(
			@Parameter(description = "ID de l'assurance", required = true) @PathVariable Long id) {
		AssuranceResponseDto assurance = assuranceService.getById(id);
		return ResponseEntity.ok(assurance);
	}

	@Operation(summary = "Lister toutes les assurances", description = "Retourne la liste de toutes les assurances")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des assurances récupérée avec succès")
	})
	@GetMapping
	public ResponseEntity<List<AssuranceResponseDto>> getAll() {
		return ResponseEntity.ok(assuranceService.getAll());
	}

	@Operation(summary = "Mettre à jour une assurance", description = "Met à jour les informations d'une assurance existante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Assurance mise à jour avec succès",
					content = @Content(schema = @Schema(implementation = AssuranceResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides"),
			@ApiResponse(responseCode = "404", description = "Assurance non trouvée")
	})
	@PutMapping("/{id}")
	public ResponseEntity<AssuranceResponseDto> update(
			@Parameter(description = "ID de l'assurance", required = true) @PathVariable Long id,
			@Valid @RequestBody AssuranceCreateDto assuranceCreateDto) {
		AssuranceResponseDto updatedAssurance = assuranceService.update(id, assuranceCreateDto);
		return ResponseEntity.ok(updatedAssurance);
	}

	@Operation(summary = "Supprimer une assurance", description = "Supprime une assurance de la base de données")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Assurance supprimée avec succès"),
			@ApiResponse(responseCode = "404", description = "Assurance non trouvée")
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@Parameter(description = "ID de l'assurance", required = true) @PathVariable Long id) {
		assuranceService.delete(id);
		return ResponseEntity.noContent().build();
	}
}

