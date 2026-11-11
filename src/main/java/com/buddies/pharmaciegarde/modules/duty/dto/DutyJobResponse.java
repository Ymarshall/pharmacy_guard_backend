package com.buddies.pharmaciegarde.modules.duty.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Traitement ou prévisualisation lancé en arrière-plan")
public record DutyJobResponse(
		@Schema(example = "3f1c...") String jobId,
		@Schema(allowableValues = {"PENDING", "RUNNING", "COMPLETED", "FAILED"}) String status,
		String message,
		@Schema(description = "PROCESS pour l'ancien traitement, PREVIEW pour la prévisualisation", allowableValues = {"PROCESS", "PREVIEW"}) String jobType,
		@Schema(description = "Présent pour un job PROCESS terminé") PdfProcessingResultDto result,
		@Schema(description = "Présent pour un job PREVIEW terminé") DutyImportPreviewResponse preview) {
}
