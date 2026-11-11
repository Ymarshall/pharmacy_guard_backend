package com.buddies.pharmaciegarde.modules.duty.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Décision sur une ligne extraite")
public record DutyImportLineDecision(
		@NotBlank @Size(max = 255) String pharmacyName,
		@Size(max = 120) String zone,
		@Size(max = 255) String pharmacistName,
		@Size(max = 80) String phoneNumbers,
		@Size(max = 500) String location,
		Long pharmacyId,
		boolean createPharmacy,
		boolean skip) {
}
