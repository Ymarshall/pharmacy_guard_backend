package com.buddies.pharmaciegarde.modules.duty.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@ValidImportLine
@Schema(description = "Décision sur une ligne extraite. Les champs city, commune, phone, description et coordinates servent à créer une pharmacie.")
public record DutyImportLineDecision(
		@NotBlank @Size(max = 255) @Schema(description = "Nom de la pharmacie, enregistré en majuscules") String pharmacyName,
		@Size(max = 120) @Schema(description = "Zone de l'affiche") String zone,
		@Size(max = 255) String pharmacistName,
		@Size(max = 80) @Schema(description = "Téléphones extraits. Utilisé si phone est vide.") String phoneNumbers,
		@Size(max = 500) @Schema(description = "Indication textuelle de l'affiche, copiée dans la description si description est vide") String location,
		@Size(max = 100) @Schema(description = "Ville. À défaut, la zone est utilisée.") String city,
		@Size(max = 100) @Schema(description = "Commune. À défaut, la zone est utilisée.") String commune,
		@Size(max = 20) @Schema(description = "Téléphone réel. Obligatoire pour createPharmacy, jamais inventé.") String phone,
		@Size(max = 1000) String description,
		@Size(max = 80) @Schema(description = "Coordonnées GPS optionnelles, format latitude,longitude. Absentes : location nulle.") String coordinates,
		Long pharmacyId,
		boolean createPharmacy,
		boolean skip) {
}
