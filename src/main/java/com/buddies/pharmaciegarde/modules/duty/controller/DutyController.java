package com.buddies.pharmaciegarde.modules.duty.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.buddies.pharmaciegarde.modules.duty.dto.DutyCreateDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyResponseDto;
import com.buddies.pharmaciegarde.modules.duty.service.DutyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/duties")
@Tag(name = "Duties", description = "API de gestion des gardes de pharmacies")
public class DutyController {

	private final DutyService dutyService;

	public DutyController(DutyService dutyService) {
		this.dutyService = dutyService;
	}

	@Operation(summary = "Créer une garde", description = "Crée une nouvelle garde pour une pharmacie")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Garde créée avec succès",
					content = @Content(schema = @Schema(implementation = DutyResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides ou chevauchement de dates"),
			@ApiResponse(responseCode = "404", description = "Pharmacie non trouvée")
	})
	@PostMapping
	public ResponseEntity<DutyResponseDto> create(@Valid @RequestBody DutyCreateDto dutyCreateDto) {
		DutyResponseDto createdDuty = dutyService.create(dutyCreateDto);
		return new ResponseEntity<>(createdDuty, HttpStatus.CREATED);
	}

	@Operation(summary = "Récupérer une garde par son ID", description = "Retourne les détails d'une garde spécifique")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Garde trouvée",
					content = @Content(schema = @Schema(implementation = DutyResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Garde non trouvée")
	})
	@GetMapping("/{id}")
	public ResponseEntity<DutyResponseDto> getById(
			@Parameter(description = "ID de la garde", required = true) @PathVariable Long id) {
		DutyResponseDto duty = dutyService.getById(id);
		return ResponseEntity.ok(duty);
	}

	@Operation(summary = "Lister toutes les gardes", description = "Retourne la liste de toutes les gardes")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des gardes récupérée avec succès")
	})
	@GetMapping
	public ResponseEntity<?> getAll(
			@Parameter(description = "Index de page. Absent = liste complète historique.") @RequestParam(required = false) Integer page,
			@Parameter(description = "Taille de page, max 100") @RequestParam(required = false) Integer size,
			@Parameter(description = "Recherche insensible à la casse et aux accents (nom, ville, statut)") @RequestParam(required = false) String search,
			@Parameter(description = "Tri : id, status, createdAt. Format champ,asc|desc") @RequestParam(required = false) String sort) {
		if (page != null || size != null) {
			return ResponseEntity.ok(dutyService.getPage(page == null ? 0 : page, size == null ? 20 : size, search, sort));
		}
		if ((search != null && !search.isBlank()) || (sort != null && !sort.isBlank())) {
			return ResponseEntity.ok(dutyService.list(search, sort));
		}
		return ResponseEntity.ok(dutyService.getAll());
	}

	@Operation(summary = "Récupérer l'historique des gardes d'une pharmacie", 
			description = "Retourne toutes les gardes d'une pharmacie triées par date (plus récentes en premier)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Historique récupéré avec succès"),
			@ApiResponse(responseCode = "404", description = "Pharmacie non trouvée")
	})
	@GetMapping("/pharmacy/{pharmacyId}/history")
	public ResponseEntity<List<DutyResponseDto>> getPharmacyHistory(
			@Parameter(description = "ID de la pharmacie", required = true) @PathVariable Long pharmacyId) {
		return ResponseEntity.ok(dutyService.getPharmacyHistory(pharmacyId));
	}

	@Operation(summary = "Récupérer les gardes d'une pharmacie", description = "Retourne toutes les gardes d'une pharmacie")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des gardes récupérée avec succès")
	})
	@GetMapping("/pharmacy/{pharmacyId}")
	public ResponseEntity<List<DutyResponseDto>> getByPharmacyId(
			@Parameter(description = "ID de la pharmacie", required = true) @PathVariable Long pharmacyId) {
		return ResponseEntity.ok(dutyService.getByPharmacyId(pharmacyId));
	}

	@Operation(summary = "Récupérer les gardes par période", description = "Retourne les gardes dans une période donnée")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des gardes récupérée avec succès")
	})
	@GetMapping("/date-range")
	public ResponseEntity<List<DutyResponseDto>> getByDateRange(
			@Parameter(description = "Date de début", required = true) 
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@Parameter(description = "Date de fin", required = true) 
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		return ResponseEntity.ok(dutyService.getByDateRange(startDate, endDate));
	}

	@Operation(summary = "Récupérer les gardes d'une période", 
			description = "Retourne toutes les gardes (duties) créées pour une période de garde spécifique. " +
					"Utilisé pour vérifier les données après le traitement des fichiers (étape 4 du processus nominal).")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des gardes récupérée avec succès"),
			@ApiResponse(responseCode = "404", description = "Période de garde non trouvée")
	})
	@GetMapping("/period/{dutyPeriodId}")
	public ResponseEntity<List<DutyResponseDto>> getDutiesByPeriodId(
			@Parameter(description = "ID de la période de garde", required = true) @PathVariable Long dutyPeriodId) {
		return ResponseEntity.ok(dutyService.getDutiesByPeriodId(dutyPeriodId));
	}

	@Operation(summary = "Supprimer toutes les gardes d'une période", 
			description = "Supprime toutes les gardes (duties) créées pour une période de garde. " +
					"Permet de recommencer le traitement si les données ne sont pas correctes (étape 5 du processus nominal).")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Toutes les gardes supprimées avec succès"),
			@ApiResponse(responseCode = "404", description = "Période de garde non trouvée")
	})
	@DeleteMapping("/period/{dutyPeriodId}/clear")
	public ResponseEntity<Void> clearDutiesByPeriodId(
			@Parameter(description = "ID de la période de garde", required = true) @PathVariable Long dutyPeriodId) {
		dutyService.clearDutiesByPeriodId(dutyPeriodId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Mettre à jour une garde", description = "Met à jour les informations d'une garde existante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Garde mise à jour avec succès",
					content = @Content(schema = @Schema(implementation = DutyResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides"),
			@ApiResponse(responseCode = "404", description = "Garde non trouvée")
	})
	@PutMapping("/{id}")
	public ResponseEntity<DutyResponseDto> update(
			@Parameter(description = "ID de la garde", required = true) @PathVariable Long id,
			@Valid @RequestBody DutyCreateDto dutyCreateDto) {
		DutyResponseDto updatedDuty = dutyService.update(id, dutyCreateDto);
		return ResponseEntity.ok(updatedDuty);
	}

	@Operation(summary = "Supprimer une garde", description = "Supprime une garde de la base de données")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Garde supprimée avec succès"),
			@ApiResponse(responseCode = "404", description = "Garde non trouvée")
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@Parameter(description = "ID de la garde", required = true) @PathVariable Long id) {
		dutyService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Désactiver une pharmacie de garde", 
			description = "Désactive une pharmacie de garde en mettant son statut à DISABLED. " +
					"Cette API est destinée au back office web.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Pharmacie de garde désactivée avec succès",
					content = @Content(schema = @Schema(implementation = DutyResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Garde non trouvée")
	})
	@PutMapping("/{id}/disable")
	public ResponseEntity<DutyResponseDto> disableDuty(
			@Parameter(description = "ID de la garde", required = true) @PathVariable Long id) {
		DutyResponseDto duty = dutyService.disableDuty(id);
		return ResponseEntity.ok(duty);
	}

	@Operation(summary = "Réactiver une pharmacie de garde", 
			description = "Réactive une pharmacie de garde en mettant son statut à ACTIVE. " +
					"Permet de corriger une erreur si une pharmacie a été désactivée par erreur. " +
					"Cette API est destinée au back office web.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Pharmacie de garde réactivée avec succès",
					content = @Content(schema = @Schema(implementation = DutyResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Garde non trouvée")
	})
	@PutMapping("/{id}/enable")
	public ResponseEntity<DutyResponseDto> enableDuty(
			@Parameter(description = "ID de la garde", required = true) @PathVariable Long id) {
		DutyResponseDto duty = dutyService.enableDuty(id);
		return ResponseEntity.ok(duty);
	}
}

