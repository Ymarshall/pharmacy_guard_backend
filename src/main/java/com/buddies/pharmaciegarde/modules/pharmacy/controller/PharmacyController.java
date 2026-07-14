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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyCreateDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyResponseDto;
import com.buddies.pharmaciegarde.modules.pharmacy.service.PharmacyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pharmacies")
@Tag(name = "Pharmacies", description = "API de gestion des pharmacies de garde")
public class PharmacyController {

	private final PharmacyService pharmacyService;

	public PharmacyController(PharmacyService pharmacyService) {
		this.pharmacyService = pharmacyService;
	}

	@Operation(summary = "Créer une nouvelle pharmacie", description = "Crée une nouvelle pharmacie avec les informations fournies")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Pharmacie créée avec succès",
					content = @Content(schema = @Schema(implementation = PharmacyResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides ou pharmacie déjà existante"),
			@ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
	})
	@PostMapping
	public ResponseEntity<PharmacyResponseDto> create(@Valid @RequestBody PharmacyCreateDto pharmacyCreateDto) {
		PharmacyResponseDto createdPharmacy = pharmacyService.create(pharmacyCreateDto);
		return new ResponseEntity<>(createdPharmacy, HttpStatus.CREATED);
	}

	@Operation(summary = "Récupérer une pharmacie par son ID", description = "Retourne les détails d'une pharmacie spécifique")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Pharmacie trouvée",
					content = @Content(schema = @Schema(implementation = PharmacyResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Pharmacie non trouvée")
	})
	@GetMapping("/{id}")
	public ResponseEntity<PharmacyResponseDto> getById(
			@Parameter(description = "ID de la pharmacie", required = true) @PathVariable Long id) {
		PharmacyResponseDto pharmacy = pharmacyService.getById(id);
		return ResponseEntity.ok(pharmacy);
	}

	@Operation(summary = "Lister toutes les pharmacies", description = "Retourne la liste de toutes les pharmacies avec possibilité de filtrage par ville, commune ou statut")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des pharmacies récupérée avec succès")
	})
	@GetMapping
	public ResponseEntity<List<PharmacyResponseDto>> getAll(
			@Parameter(description = "Filtrer par ville") @RequestParam(required = false) String city,
			@Parameter(description = "Filtrer par commune") @RequestParam(required = false) String commune,
			@Parameter(description = "Filtrer par statut (ACTIF ou INACTIF)") @RequestParam(required = false) String statut) {
		
		if (city != null) {
			return ResponseEntity.ok(pharmacyService.getByCity(city));
		}
		if (commune != null) {
			return ResponseEntity.ok(pharmacyService.getByCommune(commune));
		}
		if (statut != null) {
			return ResponseEntity.ok(pharmacyService.getByStatut(statut));
		}
		
		return ResponseEntity.ok(pharmacyService.getAll());
	}

	@Operation(summary = "Mettre à jour une pharmacie", description = "Met à jour les informations d'une pharmacie existante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Pharmacie mise à jour avec succès",
					content = @Content(schema = @Schema(implementation = PharmacyResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides"),
			@ApiResponse(responseCode = "404", description = "Pharmacie non trouvée")
	})
	@PutMapping("/{id}")
	public ResponseEntity<PharmacyResponseDto> update(
			@Parameter(description = "ID de la pharmacie", required = true) @PathVariable Long id,
			@Valid @RequestBody PharmacyCreateDto pharmacyCreateDto) {
		PharmacyResponseDto updatedPharmacy = pharmacyService.update(id, pharmacyCreateDto);
		return ResponseEntity.ok(updatedPharmacy);
	}

	@Operation(summary = "Supprimer une pharmacie", description = "Supprime une pharmacie de la base de données")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Pharmacie supprimée avec succès"),
			@ApiResponse(responseCode = "404", description = "Pharmacie non trouvée")
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@Parameter(description = "ID de la pharmacie", required = true) @PathVariable Long id) {
		pharmacyService.delete(id);
		return ResponseEntity.noContent().build();
	}
}

