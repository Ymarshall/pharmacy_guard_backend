package com.buddies.pharmaciegarde.modules.pharmacy.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résumé des statistiques de notation d'une pharmacie")
public class PharmacyRatingSummaryDto {

	@Schema(description = "Note moyenne (sur 5)", example = "4.5")
	private Double averageRating;

	@Schema(description = "Nombre total de commentaires", example = "25")
	private Long totalReviews;

	@Schema(description = "Distribution des notes (1 étoile: nombre, 2 étoiles: nombre, etc.)", 
			example = "{\"1\": 0, \"2\": 2, \"3\": 5, \"4\": 10, \"5\": 8}")
	private Map<Integer, Long> ratingDistribution;

	// Constructeurs
	public PharmacyRatingSummaryDto() {
	}

	public PharmacyRatingSummaryDto(Double averageRating, Long totalReviews, Map<Integer, Long> ratingDistribution) {
		this.averageRating = averageRating;
		this.totalReviews = totalReviews;
		this.ratingDistribution = ratingDistribution;
	}

	// Getters et Setters
	public Double getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(Double averageRating) {
		this.averageRating = averageRating;
	}

	public Long getTotalReviews() {
		return totalReviews;
	}

	public void setTotalReviews(Long totalReviews) {
		this.totalReviews = totalReviews;
	}

	public Map<Integer, Long> getRatingDistribution() {
		return ratingDistribution;
	}

	public void setRatingDistribution(Map<Integer, Long> ratingDistribution) {
		this.ratingDistribution = ratingDistribution;
	}
}
