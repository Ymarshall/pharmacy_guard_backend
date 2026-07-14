package com.buddies.pharmaciegarde.modules.pharmacy.controller;

import com.buddies.pharmaciegarde.modules.pharmacy.service.PharmacyGeocodingUpdateService;
import com.buddies.pharmaciegarde.modules.pharmacy.service.PharmacyGeocodingUpdateService.GeocodingUpdateStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pharmacies")
@Tag(name = "Géocodage des pharmacies", description = "API pour mettre à jour les coordonnées GPS des pharmacies")
public class PharmacyGeocodingController {

    private final PharmacyGeocodingUpdateService geocodingUpdateService;

    public PharmacyGeocodingController(PharmacyGeocodingUpdateService geocodingUpdateService) {
        this.geocodingUpdateService = geocodingUpdateService;
    }

    @Operation(
            summary = "Mettre à jour les coordonnées GPS de toutes les pharmacies",
            description = "Géocode toutes les pharmacies en utilisant l'API Google Maps et met à jour leurs coordonnées GPS. " +
                    "Cette opération peut prendre du temps et consomme des quotas de l'API Google Maps."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Mise à jour terminée",
                    content = @Content(schema = @Schema(implementation = GeocodingUpdateStats.class))
            ),
            @ApiResponse(responseCode = "400", description = "Erreur lors de la mise à jour")
    })
    @PostMapping("/update-geocoding")
    public ResponseEntity<GeocodingUpdateStats> updateAllPharmacies(
            @Parameter(description = "Forcer la mise à jour même si les coordonnées existent déjà", example = "false")
            @RequestParam(defaultValue = "false") boolean forceUpdate) {
        
        GeocodingUpdateStats stats = geocodingUpdateService.updateAllPharmacies(forceUpdate);
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Mettre à jour les coordonnées GPS d'une pharmacie spécifique",
            description = "Géocode une pharmacie spécifique en utilisant l'API Google Maps et met à jour ses coordonnées GPS."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mise à jour réussie"),
            @ApiResponse(responseCode = "400", description = "Pharmacie non trouvée ou erreur de géocodage"),
            @ApiResponse(responseCode = "404", description = "Pharmacie non trouvée")
    })
    @PostMapping("/{id}/update-geocoding")
    public ResponseEntity<?> updatePharmacy(
            @Parameter(description = "ID de la pharmacie", required = true) @PathVariable Long id) {
        
        boolean success = geocodingUpdateService.updatePharmacy(id);
        if (success) {
            return ResponseEntity.ok().body("Coordonnées GPS mises à jour avec succès");
        } else {
            return ResponseEntity.badRequest().body("Échec de la mise à jour des coordonnées GPS");
        }
    }
}

