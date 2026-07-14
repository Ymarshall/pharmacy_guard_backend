package com.buddies.pharmaciegarde.modules.pharmacy.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buddies.pharmaciegarde.modules.duty.dto.DutyReportDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyResponseDto;
import com.buddies.pharmaciegarde.modules.duty.service.DutyService;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyResponseDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacySearchDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyWithDistanceDto;
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
@RequestMapping("/api/client/pharmacies")
@Tag(name = "Client Pharmacies", description = "API de recherche de pharmacies pour les clients (app mobile)")
public class ClientPharmacyController {

	private final PharmacyService pharmacyService;
	private final DutyService dutyService;

	public ClientPharmacyController(PharmacyService pharmacyService, DutyService dutyService) {
		this.pharmacyService = pharmacyService;
		this.dutyService = dutyService;
	}

	@Operation(summary = "Obtenir les pharmacies de garde les plus proches", 
			description = "Retourne les pharmacies de garde les plus proches d'une position GPS donnée pour la période de garde active. " +
					"Seules les pharmacies en garde pour la période active (statut ACTIVE ou TO_VERIFY) sont retournées. " +
					"Cette API est l'API principale pour récupérer les pharmacies de garde.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des pharmacies de garde les plus proches récupérée avec succès"),
			@ApiResponse(responseCode = "400", description = "Coordonnées GPS invalides")
	})
	@GetMapping("/nearest-duty")
	public ResponseEntity<List<PharmacyWithDistanceDto>> getNearestDutyPharmacies(
			@Parameter(description = "Latitude de l'utilisateur", example = "5.3600", required = true) 
			@RequestParam Double latitude,
			@Parameter(description = "Longitude de l'utilisateur", example = "-4.0083", required = true) 
			@RequestParam Double longitude,
			@Parameter(description = "Nombre de pharmacies à retourner (par défaut: 3)") 
			@RequestParam(defaultValue = "3") int limit) {
		List<PharmacyWithDistanceDto> pharmacies = pharmacyService.getNearestDutyPharmacies(latitude, longitude, limit);
		return ResponseEntity.ok(pharmacies);
	}

	@Operation(summary = "Obtenir les pharmacies les plus proches", 
			description = "Retourne toutes les pharmacies actives les plus proches d'une position GPS donnée, " +
					"qu'elles soient de garde ou non. Le champ 'isOnDuty' indiquera si chaque pharmacie est de garde pour la période active.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des pharmacies les plus proches récupérée avec succès"),
			@ApiResponse(responseCode = "400", description = "Coordonnées GPS invalides")
	})
	@GetMapping("/nearest")
	public ResponseEntity<List<PharmacyWithDistanceDto>> getNearestPharmacies(
			@Parameter(description = "Latitude", example = "5.3600", required = true) 
			@RequestParam Double latitude,
			@Parameter(description = "Longitude", example = "-4.0083", required = true) 
			@RequestParam Double longitude,
			@Parameter(description = "Nombre de pharmacies à retourner (par défaut: 3)") 
			@RequestParam(defaultValue = "3") int limit) {
		List<PharmacyWithDistanceDto> pharmacies = pharmacyService.getNearestPharmacies(latitude, longitude, limit);
		return ResponseEntity.ok(pharmacies);
	}

	@Operation(summary = "Rechercher des pharmacies", 
			description = "Recherche des pharmacies par nom (début de nom), ville, commune ou quartier")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Résultats de recherche récupérés avec succès")
	})
	@GetMapping("/search")
	public ResponseEntity<List<PharmacyResponseDto>> searchPharmacies(
			@Parameter(description = "Nom de la pharmacie (début de nom)") @RequestParam(required = false) String name,
			@Parameter(description = "Ville") @RequestParam(required = false) String city,
			@Parameter(description = "Commune") @RequestParam(required = false) String commune,
			@Parameter(description = "Quartier/District") @RequestParam(required = false) String district) {
		
		PharmacySearchDto searchDto = new PharmacySearchDto();
		searchDto.setName(name);
		searchDto.setCity(city);
		searchDto.setCommune(commune);
		searchDto.setDistrict(district);

		List<PharmacyResponseDto> pharmacies = pharmacyService.searchPharmacies(searchDto);
		return ResponseEntity.ok(pharmacies);
	}

	@Operation(summary = "Signaler une pharmacie de garde", 
			description = "Signale qu'une pharmacie mentionnée comme de garde ne l'est pas réellement. " +
					"Le statut de la garde sera mis à TO_VERIFY pour vérification. " +
					"IMPORTANT: " +
					"- Le clientId doit être l'ID retourné lors de la connexion via /api/auth/client/login. " +
					"- Vous ne pouvez signaler QUE les pharmacies de la période de garde active. " +
					"- Si la pharmacie n'est pas en garde pour la période active, le signalement sera refusé.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Pharmacie signalée avec succès",
					content = @Content(schema = @Schema(implementation = DutyResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides, client inexistant, pharmacie non en garde pour la période active, ou aucune période de garde active"),
			@ApiResponse(responseCode = "404", description = "Pharmacie non trouvée")
	})
	@PostMapping(value = "/duty/report", consumes = "application/json", produces = "application/json")
	public ResponseEntity<DutyResponseDto> reportDuty(@Valid @RequestBody DutyReportDto dutyReportDto) {
		DutyResponseDto duty = dutyService.reportDuty(dutyReportDto);
		return ResponseEntity.ok(duty);
	}
}

