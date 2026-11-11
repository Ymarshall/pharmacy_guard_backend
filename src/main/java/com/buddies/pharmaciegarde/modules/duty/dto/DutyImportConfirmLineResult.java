package com.buddies.pharmaciegarde.modules.duty.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat d'une ligne validée")
public record DutyImportConfirmLineResult(
		int lineIndex,
		String status,
		Long pharmacyId,
		Long dutyId,
		String message) {
}
