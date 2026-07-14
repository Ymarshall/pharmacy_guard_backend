package com.buddies.pharmaciegarde.modules.pharmacy.service;

import java.util.List;

import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyRatingSummaryDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyReviewCreateDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyReviewResponseDto;

public interface PharmacyReviewService {

	/**
	 * Crée ou met à jour un commentaire et une note pour une pharmacie
	 * Si le client a déjà commenté cette pharmacie, le commentaire existant sera mis à jour
	 * 
	 * @param dto Données du commentaire
	 * @return Commentaire créé ou mis à jour
	 */
	PharmacyReviewResponseDto createOrUpdate(PharmacyReviewCreateDto dto);

	/**
	 * Met à jour un commentaire existant
	 * 
	 * @param reviewId ID du commentaire
	 * @param clientId ID du client (pour vérifier que c'est son commentaire)
	 * @param dto Nouvelles données
	 * @return Commentaire mis à jour
	 */
	PharmacyReviewResponseDto update(Long reviewId, Long clientId, PharmacyReviewCreateDto dto);

	/**
	 * Supprime un commentaire
	 * 
	 * @param reviewId ID du commentaire
	 * @param clientId ID du client (pour vérifier que c'est son commentaire)
	 */
	void delete(Long reviewId, Long clientId);

	/**
	 * Supprime un commentaire par son ID uniquement (sans vérification du client)
	 * Utile pour le back office ou la suppression administrative
	 * 
	 * @param reviewId ID du commentaire
	 */
	void deleteById(Long reviewId);

	/**
	 * Récupère tous les commentaires d'une pharmacie avec pagination
	 * 
	 * @param pharmacyId ID de la pharmacie
	 * @param page Numéro de page (0-indexed)
	 * @param size Taille de la page
	 * @return Liste des commentaires
	 */
	List<PharmacyReviewResponseDto> getByPharmacyId(Long pharmacyId, int page, int size);

	/**
	 * Récupère les statistiques de notation d'une pharmacie
	 * 
	 * @param pharmacyId ID de la pharmacie
	 * @return Statistiques de notation
	 */
	PharmacyRatingSummaryDto getRatingSummary(Long pharmacyId);

	/**
	 * Récupère un commentaire par son ID
	 * 
	 * @param reviewId ID du commentaire
	 * @return Commentaire
	 */
	PharmacyReviewResponseDto getById(Long reviewId);
}
