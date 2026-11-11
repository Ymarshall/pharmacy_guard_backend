package com.buddies.pharmaciegarde.modules.duty.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Autre pharmacie candidate")
public record PharmacyAlternativeResponse(
		Long pharmacyId,
		String name,
		String city,
		String commune,
		String phone,
		double score) {
}
