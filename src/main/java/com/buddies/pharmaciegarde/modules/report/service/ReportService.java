package com.buddies.pharmaciegarde.modules.report.service;

import java.time.LocalDate;

import com.buddies.pharmaciegarde.modules.report.dto.ChartDataDto;

public interface ReportService {

	/**
	 * Graphique en camembert (Pie Chart) : Répartition des pharmacies par statut
	 */
	ChartDataDto getPharmacyStatusDistribution();

	/**
	 * Graphique en ligne (Line Chart) : Évolution du nombre de pharmacies créées par mois
	 * 
	 * @param startDate Date de début (optionnel, défaut: 12 mois avant)
	 * @param endDate Date de fin (optionnel, défaut: aujourd'hui)
	 */
	ChartDataDto getPharmacyCreationTrend(LocalDate startDate, LocalDate endDate);

	/**
	 * Histogramme (Bar Chart) : Top 10 des zones avec le plus de pharmacies
	 */
	ChartDataDto getTopZonesByPharmacyCount();

	/**
	 * Graphique en zone (Area Chart) : Évolution cumulée des pharmacies et gardes par mois
	 * 
	 * @param startDate Date de début (optionnel, défaut: 12 mois avant)
	 * @param endDate Date de fin (optionnel, défaut: aujourd'hui)
	 */
	ChartDataDto getCumulativePharmacyAndDutyTrend(LocalDate startDate, LocalDate endDate);
}

