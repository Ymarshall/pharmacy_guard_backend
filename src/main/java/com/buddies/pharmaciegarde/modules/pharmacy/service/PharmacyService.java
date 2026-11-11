package com.buddies.pharmaciegarde.modules.pharmacy.service;

import java.util.List;

import com.buddies.pharmaciegarde.common.api.PageResponse;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyCreateDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyResponseDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacySearchDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyWithDistanceDto;

public interface PharmacyService {

	PharmacyResponseDto create(PharmacyCreateDto pharmacyCreateDto);

	PharmacyResponseDto getById(Long id);

	List<PharmacyResponseDto> getAll();

	PageResponse<PharmacyResponseDto> getPage(int page, int size, String city, String commune, String statut);

	PharmacyResponseDto update(Long id, PharmacyCreateDto pharmacyCreateDto);

	void delete(Long id);

	List<PharmacyResponseDto> getByCity(String city);

	List<PharmacyResponseDto> getByCommune(String commune);

	List<PharmacyResponseDto> getByStatut(String statut);

	// Méthodes pour les clients
	List<PharmacyWithDistanceDto> getNearestPharmacies(Double latitude, Double longitude, int limit);

	/**
	 * Récupère les pharmacies de garde les plus proches pour la période active
	 * @param latitude Latitude de l'utilisateur
	 * @param longitude Longitude de l'utilisateur
	 * @param limit Nombre maximum de pharmacies à retourner
	 * @return Liste des pharmacies de garde les plus proches avec leur distance
	 */
	List<PharmacyWithDistanceDto> getNearestDutyPharmacies(Double latitude, Double longitude, int limit);

	List<PharmacyResponseDto> searchPharmacies(PharmacySearchDto searchDto);
}

