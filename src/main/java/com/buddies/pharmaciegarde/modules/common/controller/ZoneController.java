package com.buddies.pharmaciegarde.modules.common.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.buddies.pharmaciegarde.modules.common.dto.ZoneCreateDto;
import com.buddies.pharmaciegarde.modules.common.dto.ZoneResponseDto;
import com.buddies.pharmaciegarde.modules.common.dto.ZoneUpdateDto;
import com.buddies.pharmaciegarde.modules.common.service.ZoneService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/zones")
@Tag(name = "Zones", description = "API pour la gestion des zones (villes et communes de Côte d'Ivoire)")
public class ZoneController {

	private final ZoneService zoneService;

	public ZoneController(ZoneService zoneService) {
		this.zoneService = zoneService;
	}

	@Operation(summary = "Récupérer toutes les zones", description = "Retourne la liste de toutes les zones")
	@GetMapping
	public ResponseEntity<?> getAllZones(
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		if (page != null || size != null) {
			return ResponseEntity.ok(zoneService.getPage(page == null ? 0 : page, size == null ? 20 : size));
		}
		List<ZoneResponseDto> zones = zoneService.getAllZones();
		return ResponseEntity.ok(zones);
	}

	@Operation(summary = "Récupérer les zones actives", description = "Retourne la liste des zones actives")
	@GetMapping("/active")
	public ResponseEntity<List<ZoneResponseDto>> getActiveZones() {
		List<ZoneResponseDto> zones = zoneService.getActiveZones();
		return ResponseEntity.ok(zones);
	}

	@Operation(summary = "Récupérer une zone par ID", description = "Retourne les détails d'une zone spécifique")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Zone trouvée"),
			@ApiResponse(responseCode = "404", description = "Zone non trouvée")
	})
	@GetMapping("/{id}")
	public ResponseEntity<ZoneResponseDto> getZoneById(
			@Parameter(description = "ID de la zone", required = true) @PathVariable Long id) {
		ZoneResponseDto zone = zoneService.getZoneById(id);
		return ResponseEntity.ok(zone);
	}

	@Operation(summary = "Récupérer les zones par type", description = "Retourne les zones filtrées par type (VILLE ou COMMUNE)")
	@GetMapping("/type/{type}")
	public ResponseEntity<List<ZoneResponseDto>> getZonesByType(
			@Parameter(description = "Type de zone (VILLE ou COMMUNE)", required = true) @PathVariable String type) {
		List<ZoneResponseDto> zones = zoneService.getZonesByType(type);
		return ResponseEntity.ok(zones);
	}

	@Operation(summary = "Récupérer les zones par région", description = "Retourne les zones filtrées par région")
	@GetMapping("/region/{region}")
	public ResponseEntity<List<ZoneResponseDto>> getZonesByRegion(
			@Parameter(description = "Nom de la région", required = true) @PathVariable String region) {
		List<ZoneResponseDto> zones = zoneService.getZonesByRegion(region);
		return ResponseEntity.ok(zones);
	}

	@Operation(summary = "Créer une nouvelle zone", description = "Crée une nouvelle zone (ville ou commune)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Zone créée avec succès"),
			@ApiResponse(responseCode = "400", description = "Données invalides")
	})
	@PostMapping
	public ResponseEntity<ZoneResponseDto> createZone(@Valid @RequestBody ZoneCreateDto createDto) {
		ZoneResponseDto createdZone = zoneService.createZone(createDto);
		return new ResponseEntity<>(createdZone, HttpStatus.CREATED);
	}

	@Operation(summary = "Mettre à jour une zone", description = "Met à jour les informations d'une zone existante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Zone mise à jour avec succès"),
			@ApiResponse(responseCode = "404", description = "Zone non trouvée"),
			@ApiResponse(responseCode = "400", description = "Données invalides")
	})
	@PutMapping("/{id}")
	public ResponseEntity<ZoneResponseDto> updateZone(
			@Parameter(description = "ID de la zone", required = true) @PathVariable Long id,
			@Valid @RequestBody ZoneUpdateDto updateDto) {
		ZoneResponseDto updatedZone = zoneService.updateZone(id, updateDto);
		return ResponseEntity.ok(updatedZone);
	}

	@Operation(summary = "Supprimer une zone", description = "Supprime une zone de la base de données")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Zone supprimée avec succès"),
			@ApiResponse(responseCode = "404", description = "Zone non trouvée")
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteZone(
			@Parameter(description = "ID de la zone", required = true) @PathVariable Long id) {
		zoneService.deleteZone(id);
		return ResponseEntity.noContent().build();
	}
}

