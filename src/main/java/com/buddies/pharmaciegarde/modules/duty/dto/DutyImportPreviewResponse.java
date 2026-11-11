package com.buddies.pharmaciegarde.modules.duty.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Prévisualisation d'une affiche avant création des gardes")
public record DutyImportPreviewResponse(
		Long dutyFileId,
		Long dutyPeriodId,
		String contentHash,
		boolean alreadyProcessed,
		int linesExtracted,
		int matched,
		int ambiguous,
		int notFound,
		int duplicates,
		List<DutyImportLineResponse> lines) {
}
