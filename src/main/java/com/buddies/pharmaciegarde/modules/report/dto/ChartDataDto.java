package com.buddies.pharmaciegarde.modules.report.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Données génériques pour un graphique")
public class ChartDataDto {

	@Schema(description = "Titre du graphique", example = "Répartition des pharmacies par statut")
	private String title;

	@Schema(description = "Type de graphique", example = "pie", allowableValues = {"pie", "line", "bar", "area", "donut"})
	private String chartType;

	@Schema(description = "Labels des axes/catégories", example = "[\"Actif\", \"Inactif\"]")
	private List<String> labels;

	@Schema(description = "Valeurs correspondantes aux labels", example = "[145, 5]")
	private List<Long> values;

	@Schema(description = "Données multiples pour graphiques complexes (ex: multi-series line)")
	private List<ChartSeriesDto> series;

	@Schema(description = "Données supplémentaires (ex: couleurs, pourcentages)")
	private Map<String, Object> metadata;

	// Constructeurs
	public ChartDataDto() {
	}

	public ChartDataDto(String title, String chartType, List<String> labels, List<Long> values) {
		this.title = title;
		this.chartType = chartType;
		this.labels = labels;
		this.values = values;
	}

	// Getters et Setters
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getChartType() {
		return chartType;
	}

	public void setChartType(String chartType) {
		this.chartType = chartType;
	}

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(List<String> labels) {
		this.labels = labels;
	}

	public List<Long> getValues() {
		return values;
	}

	public void setValues(List<Long> values) {
		this.values = values;
	}

	public List<ChartSeriesDto> getSeries() {
		return series;
	}

	public void setSeries(List<ChartSeriesDto> series) {
		this.series = series;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	@Schema(description = "Série de données pour graphiques multi-séries")
	public static class ChartSeriesDto {
		@Schema(description = "Nom de la série", example = "Pharmacies Actives")
		private String name;

		@Schema(description = "Valeurs de la série", example = "[10, 15, 20, 25]")
		private List<Long> data;

		@Schema(description = "Couleur de la série (optionnel)", example = "#3B82F6")
		private String color;

		public ChartSeriesDto() {
		}

		public ChartSeriesDto(String name, List<Long> data) {
			this.name = name;
			this.data = data;
		}

		// Getters et Setters
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Long> getData() {
			return data;
		}

		public void setData(List<Long> data) {
			this.data = data;
		}

		public String getColor() {
			return color;
		}

		public void setColor(String color) {
			this.color = color;
		}
	}
}

