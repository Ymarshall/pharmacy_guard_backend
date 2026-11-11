package com.buddies.pharmaciegarde.modules.duty.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ligne extraite, pharmacie proposée et alternatives")
public record DutyImportLineResponse(
		int lineIndex,
		String zone,
		String pharmacyName,
		String normalizedName,
		String pharmacistName,
		String phoneNumbers,
		String location,
		String matchStatus,
		double confidence,
		Long suggestedPharmacyId,
		String suggestedPharmacyName,
		String suggestedCity,
		boolean dutyAlreadyExists,
		List<PharmacyAlternativeResponse> alternatives) {
}
