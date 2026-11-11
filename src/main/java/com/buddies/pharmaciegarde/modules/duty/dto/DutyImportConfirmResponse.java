package com.buddies.pharmaciegarde.modules.duty.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat de la validation d'une affiche")
public record DutyImportConfirmResponse(
		int dutiesCreated,
		int pharmaciesCreated,
		int skipped,
		int duplicates,
		int errors,
		List<DutyImportConfirmLineResult> lines) {
}
