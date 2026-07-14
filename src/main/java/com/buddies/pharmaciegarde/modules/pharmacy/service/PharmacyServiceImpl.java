package com.buddies.pharmaciegarde.modules.pharmacy.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;
import com.buddies.pharmaciegarde.common.utils.DistanceCalculator;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.AssuranceResponseDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyCreateDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyResponseDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyReviewResponseDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacySearchDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyWithDistanceDto;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Assurance;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy.PharmacyStatus;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.AssuranceRepository;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyRepository;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyReviewRepository;
import com.buddies.pharmaciegarde.modules.duty.model.Duty;
import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;
import com.buddies.pharmaciegarde.modules.duty.model.DutyStatus;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyRepository;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyPeriodRepository;

@Service
@Transactional
public class PharmacyServiceImpl implements PharmacyService {

	private final PharmacyRepository pharmacyRepository;
	private final AssuranceRepository assuranceRepository;
	private final DutyRepository dutyRepository;
	private final DutyPeriodRepository dutyPeriodRepository;
	private final PharmacyReviewRepository pharmacyReviewRepository;

	public PharmacyServiceImpl(PharmacyRepository pharmacyRepository, AssuranceRepository assuranceRepository,
			DutyRepository dutyRepository, DutyPeriodRepository dutyPeriodRepository,
			PharmacyReviewRepository pharmacyReviewRepository) {
		this.pharmacyRepository = pharmacyRepository;
		this.assuranceRepository = assuranceRepository;
		this.dutyRepository = dutyRepository;
		this.dutyPeriodRepository = dutyPeriodRepository;
		this.pharmacyReviewRepository = pharmacyReviewRepository;
	}

	@Override
	public PharmacyResponseDto create(PharmacyCreateDto pharmacyCreateDto) {
		// Vérifier si une pharmacie avec le même nom existe déjà
		if (pharmacyRepository.existsByName(pharmacyCreateDto.getName())) {
			throw new IllegalArgumentException("Une pharmacie avec le nom '" + pharmacyCreateDto.getName() + "' existe déjà");
		}

		// Vérifier si le numéro de téléphone principal existe déjà
		if (pharmacyRepository.existsByPhone1(pharmacyCreateDto.getPhone1())) {
			throw new IllegalArgumentException("Une pharmacie avec le numéro de téléphone '" + pharmacyCreateDto.getPhone1() + "' existe déjà");
		}

		Pharmacy pharmacy = new Pharmacy();
		pharmacy.setName(pharmacyCreateDto.getName());
		pharmacy.setPhone1(pharmacyCreateDto.getPhone1());
		pharmacy.setPhone2(pharmacyCreateDto.getPhone2());
		pharmacy.setEmail(pharmacyCreateDto.getEmail());
		pharmacy.setHoraires(pharmacyCreateDto.getHoraires());
		pharmacy.setCity(pharmacyCreateDto.getCity());
		pharmacy.setCommune(pharmacyCreateDto.getCommune());
		pharmacy.setDistrict(pharmacyCreateDto.getDistrict());
		pharmacy.setDescription(pharmacyCreateDto.getDescription());
		pharmacy.setPharmacistName(pharmacyCreateDto.getPharmacistName());
		pharmacy.setPhoto(pharmacyCreateDto.getPhoto());
		pharmacy.setStatut(pharmacyCreateDto.getStatut());
		pharmacy.setLocation(pharmacyCreateDto.getLocation());

		// Gérer les assurances
		if (pharmacyCreateDto.getAssuranceIds() != null && !pharmacyCreateDto.getAssuranceIds().isEmpty()) {
			Set<Assurance> assurances = new HashSet<>();
			for (Long assuranceId : pharmacyCreateDto.getAssuranceIds()) {
				Assurance assurance = assuranceRepository.findById(assuranceId)
						.orElseThrow(() -> new ResourceNotFoundException("Assurance", assuranceId));
				assurances.add(assurance);
			}
			pharmacy.setAssurances(assurances);
		}

		Pharmacy savedPharmacy = pharmacyRepository.save(pharmacy);
		return mapToResponseDto(savedPharmacy);
	}

	@Override
	@Transactional(readOnly = true)
	public PharmacyResponseDto getById(Long id) {
		Pharmacy pharmacy = pharmacyRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Pharmacie", id));
		return mapToResponseDto(pharmacy);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PharmacyResponseDto> getAll() {
		return pharmacyRepository.findAll().stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	public PharmacyResponseDto update(Long id, PharmacyCreateDto pharmacyCreateDto) {
		Pharmacy pharmacy = pharmacyRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Pharmacie", id));

		// Vérifier si le nom est modifié et s'il existe déjà
		if (!pharmacy.getName().equals(pharmacyCreateDto.getName()) 
				&& pharmacyRepository.existsByName(pharmacyCreateDto.getName())) {
			throw new IllegalArgumentException("Une pharmacie avec le nom '" + pharmacyCreateDto.getName() + "' existe déjà");
		}

		// Vérifier si le numéro de téléphone principal est modifié et s'il existe déjà
		if (!pharmacy.getPhone1().equals(pharmacyCreateDto.getPhone1()) 
				&& pharmacyRepository.existsByPhone1(pharmacyCreateDto.getPhone1())) {
			throw new IllegalArgumentException("Une pharmacie avec le numéro de téléphone '" + pharmacyCreateDto.getPhone1() + "' existe déjà");
		}

		pharmacy.setName(pharmacyCreateDto.getName());
		pharmacy.setPhone1(pharmacyCreateDto.getPhone1());
		pharmacy.setPhone2(pharmacyCreateDto.getPhone2());
		pharmacy.setEmail(pharmacyCreateDto.getEmail());
		pharmacy.setHoraires(pharmacyCreateDto.getHoraires());
		pharmacy.setCity(pharmacyCreateDto.getCity());
		pharmacy.setCommune(pharmacyCreateDto.getCommune());
		pharmacy.setDistrict(pharmacyCreateDto.getDistrict());
		pharmacy.setDescription(pharmacyCreateDto.getDescription());
		pharmacy.setPharmacistName(pharmacyCreateDto.getPharmacistName());
		pharmacy.setPhoto(pharmacyCreateDto.getPhoto());
		pharmacy.setStatut(pharmacyCreateDto.getStatut());
		pharmacy.setLocation(pharmacyCreateDto.getLocation());

		// Gérer les assurances
		Set<Assurance> assurances = new HashSet<>();
		if (pharmacyCreateDto.getAssuranceIds() != null && !pharmacyCreateDto.getAssuranceIds().isEmpty()) {
			for (Long assuranceId : pharmacyCreateDto.getAssuranceIds()) {
				Assurance assurance = assuranceRepository.findById(assuranceId)
						.orElseThrow(() -> new ResourceNotFoundException("Assurance", assuranceId));
				assurances.add(assurance);
			}
		}
		pharmacy.setAssurances(assurances);

		Pharmacy updatedPharmacy = pharmacyRepository.save(pharmacy);
		return mapToResponseDto(updatedPharmacy);
	}

	@Override
	public void delete(Long id) {
		if (!pharmacyRepository.existsById(id)) {
			throw new ResourceNotFoundException("Pharmacie", id);
		}
		pharmacyRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PharmacyResponseDto> getByCity(String city) {
		return pharmacyRepository.findByCity(city).stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<PharmacyResponseDto> getByCommune(String commune) {
		return pharmacyRepository.findByCommune(commune).stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<PharmacyResponseDto> getByStatut(String statut) {
		try {
			PharmacyStatus status = PharmacyStatus.valueOf(statut.toUpperCase());
			return pharmacyRepository.findByStatut(status).stream()
					.map(this::mapToResponseDto)
					.collect(Collectors.toList());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Statut invalide. Les valeurs acceptées sont: ACTIF, INACTIF");
		}
	}

	private PharmacyResponseDto mapToResponseDto(Pharmacy pharmacy) {
		PharmacyResponseDto dto = new PharmacyResponseDto();
		dto.setId(pharmacy.getId());
		dto.setName(pharmacy.getName());
		dto.setPhone1(pharmacy.getPhone1());
		dto.setPhone2(pharmacy.getPhone2());
		dto.setEmail(pharmacy.getEmail());
		dto.setHoraires(pharmacy.getHoraires());
		dto.setCity(pharmacy.getCity());
		dto.setCommune(pharmacy.getCommune());
		dto.setDistrict(pharmacy.getDistrict());
		dto.setDescription(pharmacy.getDescription());
		dto.setPharmacistName(pharmacy.getPharmacistName());
		dto.setPhoto(pharmacy.getPhoto());
		dto.setStatut(pharmacy.getStatut());
		dto.setLocation(pharmacy.getLocation());
		
		// Parser et extraire les coordonnées GPS
		if (pharmacy.getLocation() != null && !pharmacy.getLocation().trim().isEmpty()) {
			double[] location = DistanceCalculator.parseLocation(pharmacy.getLocation());
			if (location != null) {
				dto.setLatitude(location[0]);
				dto.setLongitude(location[1]);
			}
		}
		
		// Mapper les assurances
		if (pharmacy.getAssurances() != null && !pharmacy.getAssurances().isEmpty()) {
			List<AssuranceResponseDto> assurances = pharmacy.getAssurances().stream()
					.map(this::mapAssuranceToDto)
					.collect(Collectors.toList());
			dto.setAssurances(assurances);
		}
		
		// Enrichir avec les notes et commentaires
		enrichPharmacyWithRatings(dto, pharmacy.getId());
		
		// Vérifier si la pharmacie est de garde pour la période active
		dto.setIsOnDuty(isPharmacyOnDuty(pharmacy.getId()));
		
		return dto;
	}

	private AssuranceResponseDto mapAssuranceToDto(Assurance assurance) {
		AssuranceResponseDto dto = new AssuranceResponseDto();
		dto.setId(assurance.getId());
		dto.setName(assurance.getName());
		dto.setDescription(assurance.getDescription());
		return dto;
	}

	@Override
	@Transactional(readOnly = true)
	public List<PharmacyWithDistanceDto> getNearestPharmacies(Double latitude, Double longitude, int limit) {
		// Valider les coordonnées
		if (latitude == null || longitude == null) {
			throw new IllegalArgumentException("La latitude et la longitude sont obligatoires");
		}

		if (latitude < -90 || latitude > 90) {
			throw new IllegalArgumentException("La latitude doit être entre -90 et 90");
		}

		if (longitude < -180 || longitude > 180) {
			throw new IllegalArgumentException("La longitude doit être entre -180 et 180");
		}

		double clientLat = latitude;
		double clientLon = longitude;

		// Récupérer toutes les pharmacies actives (garde ou pas)
		List<Pharmacy> activePharmacies = pharmacyRepository.findByStatut(PharmacyStatus.ACTIF);

		// Calculer la distance pour chaque pharmacie et filtrer celles avec localisation
		List<PharmacyWithDistanceDto> pharmaciesWithDistance = new ArrayList<>();

		for (Pharmacy pharmacy : activePharmacies) {
			if (pharmacy.getLocation() != null && !pharmacy.getLocation().trim().isEmpty()) {
				double[] location = DistanceCalculator.parseLocation(pharmacy.getLocation());
				if (location != null) {
					double distance = DistanceCalculator.calculateDistance(
							clientLat, clientLon, location[0], location[1]);

					PharmacyWithDistanceDto dto = mapToPharmacyWithDistanceDto(pharmacy);
					// Ajouter les coordonnées GPS de la pharmacie
					dto.setLatitude(location[0]);
					dto.setLongitude(location[1]);
					dto.setDistanceInKm(Math.round(distance * 100.0) / 100.0); // Arrondir à 2 décimales
					pharmaciesWithDistance.add(dto);
				}
			}
		}

		// Trier par distance et prendre les N plus proches
		return pharmaciesWithDistance.stream()
				.sorted(Comparator.comparing(PharmacyWithDistanceDto::getDistanceInKm))
				.limit(limit)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<PharmacyWithDistanceDto> getNearestDutyPharmacies(Double latitude, Double longitude, int limit) {
		// Valider les coordonnées
		if (latitude == null || longitude == null) {
			throw new IllegalArgumentException("La latitude et la longitude sont obligatoires");
		}

		if (latitude < -90 || latitude > 90) {
			throw new IllegalArgumentException("La latitude doit être entre -90 et 90");
		}

		if (longitude < -180 || longitude > 180) {
			throw new IllegalArgumentException("La longitude doit être entre -180 et 180");
		}

		// Récupérer la période de garde active
		java.util.Optional<DutyPeriod> activePeriodOpt = dutyPeriodRepository.findByIsCurrentTrue();

		if (activePeriodOpt.isEmpty()) {
			// Si aucune période active, retourner une liste vide
			return new ArrayList<>();
		}

		DutyPeriod activePeriod = activePeriodOpt.get();

		// Récupérer les gardes de la période active avec statut ACTIVE ou TO_VERIFY uniquement
		List<DutyStatus> allowedStatuses = List.of(DutyStatus.ACTIVE, DutyStatus.TO_VERIFY);
		List<Duty> duties = dutyRepository.findByDutyPeriodIdAndStatusIn(activePeriod.getId(), allowedStatuses);

		// Extraire les pharmacies en garde (uniquement les pharmacies actives)
		List<Pharmacy> dutyPharmacies = duties.stream()
				.map(Duty::getPharmacy)
				.filter(p -> p.getStatut() == PharmacyStatus.ACTIF)
				.distinct()
				.collect(Collectors.toList());

		// Calculer la distance pour chaque pharmacie et filtrer celles avec localisation
		List<PharmacyWithDistanceDto> pharmaciesWithDistance = new ArrayList<>();

		for (Pharmacy pharmacy : dutyPharmacies) {
			if (pharmacy.getLocation() != null && !pharmacy.getLocation().trim().isEmpty()) {
				double[] location = DistanceCalculator.parseLocation(pharmacy.getLocation());
				if (location != null) {
					double distance = DistanceCalculator.calculateDistance(
							latitude, longitude, location[0], location[1]);

					PharmacyWithDistanceDto dto = mapToPharmacyWithDistanceDto(pharmacy);
					// Ajouter les coordonnées GPS de la pharmacie
					dto.setLatitude(location[0]);
					dto.setLongitude(location[1]);
					dto.setDistanceInKm(Math.round(distance * 100.0) / 100.0); // Arrondir à 2 décimales
					pharmaciesWithDistance.add(dto);
				}
			}
		}

		// Trier par distance et prendre les N plus proches
		return pharmaciesWithDistance.stream()
				.sorted(Comparator.comparing(PharmacyWithDistanceDto::getDistanceInKm))
				.limit(limit)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<PharmacyResponseDto> searchPharmacies(PharmacySearchDto searchDto) {
		List<Pharmacy> pharmacies = new ArrayList<>();

		// Si aucun critère n'est fourni, retourner toutes les pharmacies actives
		if (searchDto.getName() == null && searchDto.getCity() == null
				&& searchDto.getCommune() == null && searchDto.getDistrict() == null) {
			pharmacies = pharmacyRepository.findByStatut(PharmacyStatus.ACTIF);
		} else {
			// Recherche par nom (début de nom)
			if (searchDto.getName() != null && !searchDto.getName().trim().isEmpty()) {
				pharmacies.addAll(pharmacyRepository.findByNameStartingWithIgnoreCaseAndStatut(
						searchDto.getName().trim(), PharmacyStatus.ACTIF));
			}

			// Recherche par ville
			if (searchDto.getCity() != null && !searchDto.getCity().trim().isEmpty()) {
				pharmacies.addAll(pharmacyRepository.findByCityContainingIgnoreCaseAndStatut(
						searchDto.getCity().trim(), PharmacyStatus.ACTIF));
			}

			// Recherche par commune
			if (searchDto.getCommune() != null && !searchDto.getCommune().trim().isEmpty()) {
				pharmacies.addAll(pharmacyRepository.findByCommuneContainingIgnoreCaseAndStatut(
						searchDto.getCommune().trim(), PharmacyStatus.ACTIF));
			}

			// Recherche par quartier/district
			if (searchDto.getDistrict() != null && !searchDto.getDistrict().trim().isEmpty()) {
				pharmacies.addAll(pharmacyRepository.findByDistrictContainingIgnoreCaseAndStatut(
						searchDto.getDistrict().trim(), PharmacyStatus.ACTIF));
			}

			// Supprimer les doublons
			pharmacies = pharmacies.stream()
					.distinct()
					.collect(Collectors.toList());
		}

		return pharmacies.stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	private PharmacyWithDistanceDto mapToPharmacyWithDistanceDto(Pharmacy pharmacy) {
		PharmacyWithDistanceDto dto = new PharmacyWithDistanceDto();
		dto.setId(pharmacy.getId());
		dto.setName(pharmacy.getName());
		dto.setPhone1(pharmacy.getPhone1());
		dto.setPhone2(pharmacy.getPhone2());
		dto.setEmail(pharmacy.getEmail());
		dto.setHoraires(pharmacy.getHoraires());
		dto.setCity(pharmacy.getCity());
		dto.setCommune(pharmacy.getCommune());
		dto.setDistrict(pharmacy.getDistrict());
		dto.setDescription(pharmacy.getDescription());
		dto.setPharmacistName(pharmacy.getPharmacistName());
		dto.setPhoto(pharmacy.getPhoto());
		dto.setStatut(pharmacy.getStatut());
		dto.setLocation(pharmacy.getLocation());

		// Parser et extraire les coordonnées GPS (latitude et longitude séparées)
		if (pharmacy.getLocation() != null && !pharmacy.getLocation().trim().isEmpty()) {
			double[] location = DistanceCalculator.parseLocation(pharmacy.getLocation());
			if (location != null) {
				dto.setLatitude(location[0]);
				dto.setLongitude(location[1]);
			}
		}

		// Mapper les assurances
		if (pharmacy.getAssurances() != null && !pharmacy.getAssurances().isEmpty()) {
			List<AssuranceResponseDto> assurances = pharmacy.getAssurances().stream()
					.map(this::mapAssuranceToDto)
					.collect(Collectors.toList());
			dto.setAssurances(assurances);
		}

		// Enrichir avec les notes et commentaires
		enrichPharmacyWithRatings(dto, pharmacy.getId());

		// Vérifier si la pharmacie est de garde pour la période active
		dto.setIsOnDuty(isPharmacyOnDuty(pharmacy.getId()));

		return dto;
	}

	/**
	 * Vérifie si une pharmacie est de garde pour la période active
	 * 
	 * @param pharmacyId ID de la pharmacie
	 * @return true si la pharmacie est de garde pour la période active avec statut ACTIVE ou TO_VERIFY, false sinon
	 */
	private Boolean isPharmacyOnDuty(Long pharmacyId) {
		// Récupérer la période de garde active
		java.util.Optional<DutyPeriod> activePeriodOpt = dutyPeriodRepository.findByIsCurrentTrue();
		
		if (activePeriodOpt.isEmpty()) {
			return false;
		}

		DutyPeriod activePeriod = activePeriodOpt.get();

		// Vérifier si la pharmacie a une garde pour la période active avec statut ACTIVE ou TO_VERIFY
		List<DutyStatus> allowedStatuses = List.of(DutyStatus.ACTIVE, DutyStatus.TO_VERIFY);
		List<Duty> duties = dutyRepository.findByPharmacyIdAndDutyPeriodId(pharmacyId, activePeriod.getId());
		
		return duties.stream()
				.anyMatch(d -> allowedStatuses.contains(d.getStatus()));
	}

	/**
	 * Enrichit un DTO de pharmacie avec les informations de notation et commentaires
	 */
	private void enrichPharmacyWithRatings(PharmacyResponseDto dto, Long pharmacyId) {
		// Calculer la note moyenne
		Double averageRating = pharmacyReviewRepository.findAverageRatingByPharmacyId(pharmacyId);
		if (averageRating != null) {
			dto.setAverageRating(Math.round(averageRating * 10.0) / 10.0); // Arrondir à 1 décimale
		} else {
			dto.setAverageRating(null);
		}

		// Compter le nombre total de commentaires
		Long totalReviews = pharmacyReviewRepository.countByPharmacyId(pharmacyId);
		dto.setTotalReviews(totalReviews != null ? totalReviews : 0L);

		// Récupérer les 3 derniers commentaires
		if (totalReviews != null && totalReviews > 0) {
			org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 3);
			List<com.buddies.pharmaciegarde.modules.pharmacy.model.PharmacyReview> recentReviews = 
					pharmacyReviewRepository.findByPharmacyIdOrderByCreatedAtDesc(pharmacyId, pageable);
			
			List<PharmacyReviewResponseDto> recentReviewsDto = recentReviews.stream()
					.map(this::mapReviewToResponseDto)
					.collect(Collectors.toList());
			dto.setRecentReviews(recentReviewsDto);
		} else {
			dto.setRecentReviews(new ArrayList<>());
		}
	}

	/**
	 * Mappe une entité PharmacyReview vers un DTO
	 */
	private PharmacyReviewResponseDto mapReviewToResponseDto(
			com.buddies.pharmaciegarde.modules.pharmacy.model.PharmacyReview review) {
		PharmacyReviewResponseDto dto = new PharmacyReviewResponseDto();
		dto.setId(review.getId());
		dto.setPharmacyId(review.getPharmacy().getId());
		dto.setPharmacyName(review.getPharmacy().getName());
		dto.setClientId(review.getClient().getId());

		// Construire le nom du client
		String clientName = "Anonyme";
		if (review.getClient().getFirstName() != null || review.getClient().getLastName() != null) {
			StringBuilder nameBuilder = new StringBuilder();
			if (review.getClient().getFirstName() != null) {
				nameBuilder.append(review.getClient().getFirstName());
			}
			if (review.getClient().getLastName() != null) {
				if (nameBuilder.length() > 0) {
					nameBuilder.append(" ");
				}
				nameBuilder.append(review.getClient().getLastName());
			}
			clientName = nameBuilder.toString();
		}
		dto.setClientName(clientName);

		dto.setRating(review.getRating());
		dto.setComment(review.getComment());
		dto.setCreatedAt(review.getCreatedAt());
		dto.setUpdatedAt(review.getUpdatedAt());

		return dto;
	}
}

