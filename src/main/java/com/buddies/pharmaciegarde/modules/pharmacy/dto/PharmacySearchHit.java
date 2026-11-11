package com.buddies.pharmaciegarde.modules.pharmacy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pharmacie candidate pour la révision d'une ligne d'affiche")
public record PharmacySearchHit(
		Long id,
		String name,
		String city,
		String commune,
		String district,
		String phone,
		double score) {
}
