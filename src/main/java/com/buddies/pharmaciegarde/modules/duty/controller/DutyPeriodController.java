package com.buddies.pharmaciegarde.modules.duty.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.buddies.pharmaciegarde.modules.duty.dto.DutyPeriodCreateDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyPeriodResponseDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyPeriodUpdateDto;
import com.buddies.pharmaciegarde.modules.duty.service.DutyPeriodService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/duty-periods")
@Tag(name = "Périodes de garde", description = "API pour la gestion des périodes de garde")
public class DutyPeriodController {

	private final DutyPeriodService dutyPeriodService;

	public DutyPeriodController(DutyPeriodService dutyPeriodService) {
		this.dutyPeriodService = dutyPeriodService;
	}

	@Operation(summary = "Récupérer toutes les périodes de garde", description = "Retourne la liste de toutes les périodes de garde")
	@GetMapping
	public ResponseEntity<?> getAllPeriods(
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@Parameter(description = "Recherche sur la description et les dates") @RequestParam(required = false) String search,
			@Parameter(description = "Tri : id, startDate, endDate, isCurrent") @RequestParam(required = false) String sort) {
		if (page != null || size != null) {
			return ResponseEntity.ok(dutyPeriodService.getPage(page == null ? 0 : page, size == null ? 20 : size, search, sort));
		}
		if ((search != null && !search.isBlank()) || (sort != null && !sort.isBlank())) {
			return ResponseEntity.ok(dutyPeriodService.list(search, sort));
		}
		return ResponseEntity.ok(dutyPeriodService.getAllPeriods());
	}

	@Operation(summary = "Récupérer la garde courante", description = "Retourne la période de garde marquée comme courante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Garde courante trouvée"),
			@ApiResponse(responseCode = "404", description = "Aucune garde courante trouvée")
	})
	@GetMapping("/current")
	public ResponseEntity<DutyPeriodResponseDto> getCurrentPeriod() {
		DutyPeriodResponseDto period = dutyPeriodService.getCurrentPeriod();
		return ResponseEntity.ok(period);
	}

	@Operation(summary = "Récupérer toutes les gardes courantes", description = "Retourne toutes les périodes de garde marquées comme courantes")
	@GetMapping("/current/all")
	public ResponseEntity<List<DutyPeriodResponseDto>> getCurrentPeriods() {
		List<DutyPeriodResponseDto> periods = dutyPeriodService.getCurrentPeriods();
		return ResponseEntity.ok(periods);
	}

	@Operation(summary = "Récupérer une période par ID", description = "Retourne les détails d'une période spécifique")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Période trouvée"),
			@ApiResponse(responseCode = "404", description = "Période non trouvée")
	})
	@GetMapping("/{id}")
	public ResponseEntity<DutyPeriodResponseDto> getPeriodById(
			@Parameter(description = "ID de la période", required = true) @PathVariable Long id) {
		DutyPeriodResponseDto period = dutyPeriodService.getPeriodById(id);
		return ResponseEntity.ok(period);
	}

	@Operation(summary = "Créer une nouvelle période de garde", description = "Crée une nouvelle période de garde avec dates de début et fin")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Période créée avec succès"),
			@ApiResponse(responseCode = "400", description = "Données invalides ou chevauchement")
	})
	@PostMapping
	public ResponseEntity<DutyPeriodResponseDto> createPeriod(@Valid @RequestBody DutyPeriodCreateDto createDto) {
		DutyPeriodResponseDto createdPeriod = dutyPeriodService.createPeriod(createDto);
		return new ResponseEntity<>(createdPeriod, HttpStatus.CREATED);
	}

	@Operation(summary = "Mettre à jour une période de garde", description = "Met à jour les informations d'une période existante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Période mise à jour avec succès"),
			@ApiResponse(responseCode = "404", description = "Période non trouvée"),
			@ApiResponse(responseCode = "400", description = "Données invalides")
	})
	@PutMapping("/{id}")
	public ResponseEntity<DutyPeriodResponseDto> updatePeriod(
			@Parameter(description = "ID de la période", required = true) @PathVariable Long id,
			@Valid @RequestBody DutyPeriodUpdateDto updateDto) {
		DutyPeriodResponseDto updatedPeriod = dutyPeriodService.updatePeriod(id, updateDto);
		return ResponseEntity.ok(updatedPeriod);
	}

	@Operation(summary = "Supprimer une période de garde", description = "Supprime une période de garde de la base de données")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Période supprimée avec succès"),
			@ApiResponse(responseCode = "404", description = "Période non trouvée")
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletePeriod(
			@Parameter(description = "ID de la période", required = true) @PathVariable Long id) {
		dutyPeriodService.deletePeriod(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Activer une période de garde", 
			description = "Marque une période de garde comme courante/active (isCurrent = true). " +
					"Désactive automatiquement les autres périodes courantes. " +
					"Étape 6 du processus nominal : activer la garde après vérification des données.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Période activée avec succès",
					content = @Content(schema = @Schema(implementation = DutyPeriodResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Période non trouvée")
	})
	@PostMapping("/{id}/activate")
	public ResponseEntity<DutyPeriodResponseDto> activatePeriod(
			@Parameter(description = "ID de la période à activer", required = true) @PathVariable Long id) {
		DutyPeriodResponseDto activatedPeriod = dutyPeriodService.activatePeriod(id);
		return ResponseEntity.ok(activatedPeriod);
	}
}

