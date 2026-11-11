package com.buddies.pharmaciegarde.modules.duty.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Traitement de fichier lancé en arrière-plan")
public record DutyJobResponse(
		String jobId,
		String status,
		String message,
		PdfProcessingResultDto result) {
}
