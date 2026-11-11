package com.buddies.pharmaciegarde.modules.duty.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Décisions de l'administrateur après prévisualisation")
public record DutyImportConfirmRequest(
		@NotNull @Valid @NotEmpty List<DutyImportLineDecision> lines) {
}
