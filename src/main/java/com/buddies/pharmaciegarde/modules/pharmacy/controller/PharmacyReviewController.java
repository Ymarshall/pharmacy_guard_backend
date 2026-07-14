package com.buddies.pharmaciegarde.modules.pharmacy.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyRatingSummaryDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyReviewCreateDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyReviewResponseDto;
import com.buddies.pharmaciegarde.modules.pharmacy.service.PharmacyReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/client/pharmacies")
@Tag(name = "Pharmacy Reviews", description = "API de gestion des commentaires et notes sur les pharmacies (app mobile)")
public class PharmacyReviewController {

	private final PharmacyReviewService pharmacyReviewService;

	public PharmacyReviewController(PharmacyReviewService pharmacyReviewService) {
		this.pharmacyReviewService = pharmacyReviewService;
	}

	@Operation(summary = "Créer ou mettre à jour un commentaire et une note", 
			description = "Crée un nouveau commentaire et note pour une pharmacie. " +
					"Si le client a déjà commenté cette pharmacie, le commentaire existant sera mis à jour.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Commentaire créé ou mis à jour avec succès",
					content = @Content(schema = @Schema(implementation = PharmacyReviewResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides"),
			@ApiResponse(responseCode = "404", description = "Pharmacie ou client non trouvé")
	})
	@PostMapping("/{pharmacyId}/reviews")
	public ResponseEntity<PharmacyReviewResponseDto> createOrUpdateReview(
			@Parameter(description = "ID de la pharmacie", required = true) @PathVariable Long pharmacyId,
			@Valid @RequestBody PharmacyReviewCreateDto dto) {
		// S'assurer que le pharmacyId dans le DTO correspond à celui dans l'URL
		dto.setPharmacyId(pharmacyId);
		PharmacyReviewResponseDto review = pharmacyReviewService.createOrUpdate(dto);
		return ResponseEntity.ok(review);
	}

	@Operation(summary = "Modifier un commentaire", 
			description = "Modifie un commentaire existant. Le client ne peut modifier que ses propres commentaires.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Commentaire modifié avec succès",
					content = @Content(schema = @Schema(implementation = PharmacyReviewResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Vous ne pouvez modifier que vos propres commentaires"),
			@ApiResponse(responseCode = "404", description = "Commentaire non trouvé")
	})
	@PutMapping("/reviews/{reviewId}")
	public ResponseEntity<PharmacyReviewResponseDto> updateReview(
			@Parameter(description = "ID du commentaire", required = true) @PathVariable Long reviewId,
			@Parameter(description = "ID du client", required = true) @RequestParam Long clientId,
			@Valid @RequestBody PharmacyReviewCreateDto dto) {
		PharmacyReviewResponseDto review = pharmacyReviewService.update(reviewId, clientId, dto);
		return ResponseEntity.ok(review);
	}

	@Operation(summary = "Supprimer un commentaire par son ID", 
			description = "Supprime un commentaire uniquement par son ID, sans vérification du client. " +
					"Utile pour le back office ou la suppression administrative.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Commentaire supprimé avec succès"),
			@ApiResponse(responseCode = "404", description = "Commentaire non trouvé")
	})
	@DeleteMapping("/admin/reviews/{reviewId}")
	public ResponseEntity<Void> deleteReviewById(
			@Parameter(description = "ID du commentaire", required = true) @PathVariable Long reviewId) {
		pharmacyReviewService.deleteById(reviewId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Supprimer un commentaire", 
			description = "Supprime un commentaire. Le client ne peut supprimer que ses propres commentaires.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Commentaire supprimé avec succès"),
			@ApiResponse(responseCode = "400", description = "Vous ne pouvez supprimer que vos propres commentaires"),
			@ApiResponse(responseCode = "404", description = "Commentaire non trouvé")
	})
	@DeleteMapping("/reviews/{reviewId}")
	public ResponseEntity<Void> deleteReview(
			@Parameter(description = "ID du commentaire", required = true) @PathVariable Long reviewId,
			@Parameter(description = "ID du client", required = true) @RequestParam Long clientId) {
		pharmacyReviewService.delete(reviewId, clientId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Récupérer les commentaires d'une pharmacie", 
			description = "Retourne la liste paginée des commentaires d'une pharmacie, triés par date (plus récents en premier)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des commentaires récupérée avec succès"),
			@ApiResponse(responseCode = "404", description = "Pharmacie non trouvée")
	})
	@GetMapping("/{pharmacyId}/reviews")
	public ResponseEntity<List<PharmacyReviewResponseDto>> getReviews(
			@Parameter(description = "ID de la pharmacie", required = true) @PathVariable Long pharmacyId,
			@Parameter(description = "Numéro de page (0-indexed, par défaut: 0)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Taille de la page (par défaut: 10)") @RequestParam(defaultValue = "10") int size) {
		List<PharmacyReviewResponseDto> reviews = pharmacyReviewService.getByPharmacyId(pharmacyId, page, size);
		return ResponseEntity.ok(reviews);
	}

	@Operation(summary = "Récupérer les statistiques de notation d'une pharmacie", 
			description = "Retourne la note moyenne, le nombre total de commentaires et la distribution des notes")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Statistiques récupérées avec succès",
					content = @Content(schema = @Schema(implementation = PharmacyRatingSummaryDto.class))),
			@ApiResponse(responseCode = "404", description = "Pharmacie non trouvée")
	})
	@GetMapping("/{pharmacyId}/rating-summary")
	public ResponseEntity<PharmacyRatingSummaryDto> getRatingSummary(
			@Parameter(description = "ID de la pharmacie", required = true) @PathVariable Long pharmacyId) {
		PharmacyRatingSummaryDto summary = pharmacyReviewService.getRatingSummary(pharmacyId);
		return ResponseEntity.ok(summary);
	}
}
