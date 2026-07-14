package com.buddies.pharmaciegarde.modules.report.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buddies.pharmaciegarde.modules.report.dto.ChartDataDto;
import com.buddies.pharmaciegarde.modules.report.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Rapports et Statistiques", description = "APIs pour générer des graphiques et statistiques pour le menu Rapport")
public class ReportController {

	private final ReportService reportService;

	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	@Operation(
			summary = "Graphique en camembert - Répartition des pharmacies par statut",
			description = "Retourne les données pour un graphique en camembert (pie chart) montrant la répartition des pharmacies entre Actif et Inactif. " +
					"Utile pour visualiser rapidement la proportion de pharmacies opérationnelles."
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Données du graphique récupérées avec succès",
					content = @Content(schema = @Schema(implementation = ChartDataDto.class))
			)
	})
	@GetMapping("/chart/pharmacy-status-pie")
	public ResponseEntity<ChartDataDto> getPharmacyStatusDistribution() {
		ChartDataDto chart = reportService.getPharmacyStatusDistribution();
		return ResponseEntity.ok(chart);
	}

	@Operation(
			summary = "Graphique en ligne - Évolution du nombre de gardes par période",
			description = "Retourne les données pour un graphique en ligne (line chart) montrant l'évolution du nombre de périodes de garde créées par mois. " +
					"Permet d'analyser les tendances temporelles et la fréquence des créations de périodes de garde. " +
					"Par défaut, affiche les 12 derniers mois si aucune date n'est spécifiée."
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Données du graphique récupérées avec succès",
					content = @Content(schema = @Schema(implementation = ChartDataDto.class))
			)
	})
	@GetMapping("/chart/pharmacy-creation-trend-line")
	public ResponseEntity<ChartDataDto> getPharmacyCreationTrend(
			@Parameter(description = "Date de début (format: YYYY-MM-DD). Par défaut: 12 mois avant aujourd'hui", required = false)
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@Parameter(description = "Date de fin (format: YYYY-MM-DD). Par défaut: aujourd'hui", required = false)
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		ChartDataDto chart = reportService.getPharmacyCreationTrend(startDate, endDate);
		return ResponseEntity.ok(chart);
	}

	@Operation(
			summary = "Histogramme - Top 10 des zones avec le plus de pharmacies",
			description = "Retourne les données pour un histogramme (bar chart) affichant les 10 zones géographiques avec le plus grand nombre de pharmacies. " +
					"Utile pour identifier les zones principales et la répartition géographique du réseau de pharmacies."
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Données du graphique récupérées avec succès",
					content = @Content(schema = @Schema(implementation = ChartDataDto.class))
			)
	})
	@GetMapping("/chart/top-zones-bar")
	public ResponseEntity<ChartDataDto> getTopZonesByPharmacyCount() {
		ChartDataDto chart = reportService.getTopZonesByPharmacyCount();
		return ResponseEntity.ok(chart);
	}

	@Operation(
			summary = "Graphique en zone - Évolution cumulée des périodes de garde et gardes assignées",
			description = "Retourne les données pour un graphique en zone (area chart) montrant l'évolution cumulée du nombre de périodes de garde créées " +
					"et du nombre total de gardes assignées au fil du temps. Permet de visualiser la croissance globale du système. " +
					"Par défaut, affiche les 12 derniers mois si aucune date n'est spécifiée."
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Données du graphique récupérées avec succès",
					content = @Content(schema = @Schema(implementation = ChartDataDto.class))
			)
	})
	@GetMapping("/chart/cumulative-trend-area")
	public ResponseEntity<ChartDataDto> getCumulativePharmacyAndDutyTrend(
			@Parameter(description = "Date de début (format: YYYY-MM-DD). Par défaut: 12 mois avant aujourd'hui", required = false)
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@Parameter(description = "Date de fin (format: YYYY-MM-DD). Par défaut: aujourd'hui", required = false)
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		ChartDataDto chart = reportService.getCumulativePharmacyAndDutyTrend(startDate, endDate);
		return ResponseEntity.ok(chart);
	}
}

