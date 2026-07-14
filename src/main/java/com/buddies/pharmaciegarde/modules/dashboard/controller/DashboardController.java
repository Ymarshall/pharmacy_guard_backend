package com.buddies.pharmaciegarde.modules.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buddies.pharmaciegarde.modules.dashboard.dto.DashboardStatsDto;
import com.buddies.pharmaciegarde.modules.dashboard.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "API du dashboard du backoffice avec statistiques et informations pertinentes")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@Operation(
			summary = "Récupérer toutes les statistiques du dashboard",
			description = "Retourne un ensemble complet de statistiques et informations pour le dashboard du backoffice. " +
					"Inclut : vue d'ensemble, période de garde active, statistiques pharmacies, statistiques fichiers, " +
					"activité récente et alertes.")
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Statistiques récupérées avec succès",
					content = @Content(schema = @Schema(implementation = DashboardStatsDto.class))
			)
	})
	@GetMapping("/stats")
	public ResponseEntity<DashboardStatsDto> getDashboardStats() {
		DashboardStatsDto stats = dashboardService.getDashboardStats();
		return ResponseEntity.ok(stats);
	}
}

