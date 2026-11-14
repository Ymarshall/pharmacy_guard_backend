package com.buddies.pharmaciegarde.modules.pharmacy.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.common.api.PageRequests;
import com.buddies.pharmaciegarde.common.api.PageResponse;
import com.buddies.pharmaciegarde.common.api.Slices;
import com.buddies.pharmaciegarde.common.api.Sorts;
import com.buddies.pharmaciegarde.common.api.TextSearch;
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
import com.buddies.pharmaciegarde.modules.duty.service.ActiveDutyPeriodResolver;

@Service
@Transactional
public class PharmacyServiceImpl implements PharmacyService {

	private final PharmacyRepository pharmacyRepository;
	private final AssuranceRepository assuranceRepository;
	private final DutyRepository dutyRepository;
	private final ActiveDutyPeriodResolver activeDutyPeriodResolver;
	private final PharmacyReviewRepository pharmacyReviewRepository;
	private final PharmacyTrigramSearch pharmacyTrigramSearch;

	public PharmacyServiceImpl(PharmacyRepository pharmacyRepository, AssuranceRepository assuranceRepository,
			DutyRepository dutyRepository, ActiveDutyPeriodResolver activeDutyPeriodResolver,
			PharmacyReviewRepository pharmacyReviewRepository, PharmacyTrigramSearch pharmacyTrigramSearch) {
		this.pharmacyRepository = pharmacyRepository;
		this.assuranceRepository = assuranceRepository;
		this.dutyRepository = dutyRepository;
		this.activeDutyPeriodResolver = activeDutyPeriodResolver;
		this.pharmacyReviewRepository = pharmacyReviewRepository;
		this.pharmacyTrigramSearch = pharmacyTrigramSearch;
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
		return mapAllToResponseDto(pharmacyRepository.findAll());
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<PharmacyResponseDto> getPage(int page, int size, String city, String commune, String statut,
			String search, String sort) {
		if (search == null || search.isBlank()) {
			org.springframework.data.domain.Sort parsed = Sorts.parse(sort,
					java.util.Set.of("id", "name", "city", "commune", "statut"), "name");
			org.springframework.data.domain.Pageable pageable = PageRequests.of(page, size, parsed);
			org.springframework.data.domain.Page<Pharmacy> result;
			if (city != null) {
				result = pharmacyRepository.findByCity(city, pageable);
			} else if (commune != null) {
				result = pharmacyRepository.findByCommune(commune, pageable);
			} else if (statut != null) {
				result = pharmacyRepository.findByStatut(PharmacyStatus.valueOf(statut.toUpperCase()), pageable);
			} else {
				result = pharmacyRepository.findAll(pageable);
			}
			EnrichmentContext context = buildContext(result.getContent());
			return PageResponse.from(result.map(pharmacy -> mapToResponseDto(pharmacy, context)));
		}
		Page<Pharmacy> slice = Slices.of(selectPharmacies(city, commune, statut, search, sort), page, size);
		EnrichmentContext context = buildContext(slice.getContent());
		return PageResponse.from(slice.map(pharmacy -> mapToResponseDto(pharmacy, context)));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PharmacyResponseDto> list(String city, String commune, String statut, String search, String sort) {
		return mapAllToResponseDto(selectPharmacies(city, commune, statut, search, sort));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacySearchHit> searchForReview(String q,
			String city, int page, int limit) {
		String query = q == null ? "" : q.trim();
		String foldedCity = TextSearch.fold(city);
		List<com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacySearchHit> hits = pharmacyRepository.findAll()
				.stream()
				.filter(pharmacy -> foldedCity.isEmpty()
						|| TextSearch.fold(pharmacy.getCity()).equals(foldedCity)
						|| TextSearch.fold(pharmacy.getCommune()).equals(foldedCity)
						|| TextSearch.contains(pharmacy.getCity(), city)
						|| TextSearch.contains(pharmacy.getCommune(), city))
				.map(pharmacy -> new com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacySearchHit(
						pharmacy.getId(), pharmacy.getName(), pharmacy.getCity(), pharmacy.getCommune(),
						pharmacy.getDistrict(), pharmacy.getPhone1(), reviewScore(query, pharmacy)))
				.filter(hit -> query.isEmpty() || hit.score() >= 0.45)
				.sorted(java.util.Comparator.comparingDouble(
						com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacySearchHit::score).reversed()
						.thenComparing(com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacySearchHit::name,
								java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
				.toList();
		int safeLimit = limit <= 0 ? 20 : Math.min(limit, 50);
		return PageResponse.from(Slices.of(hits, page, safeLimit));
	}

	private List<Pharmacy> selectPharmacies(String city, String commune, String statut, String search, String sort) {
		org.springframework.data.domain.Sort parsed = Sorts.parse(sort,
				java.util.Set.of("id", "name", "city", "commune", "statut"), "name");
		if (search != null && !search.isBlank()) {
			try {
				List<Long> ids = pharmacyTrigramSearch.searchIds(search, city, commune, statut, parsed);
				if (ids != null) {
					java.util.Map<Long, Pharmacy> byId = pharmacyRepository.findAllById(ids).stream()
							.collect(Collectors.toMap(Pharmacy::getId, pharmacy -> pharmacy));
					return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).collect(Collectors.toList());
				}
			} catch (RuntimeException ex) {
				org.slf4j.LoggerFactory.getLogger(PharmacyServiceImpl.class)
						.warn("Recherche trigram indisponible, repli Java : {}", ex.getMessage());
			}
		}
		PharmacyStatus status = statut == null || statut.isBlank() ? null : PharmacyStatus.valueOf(statut.toUpperCase());
		return pharmacyRepository.findAll(parsed).stream()
				.filter(pharmacy -> city == null || city.isBlank() || TextSearch.fold(pharmacy.getCity()).equals(TextSearch.fold(city)))
				.filter(pharmacy -> commune == null || commune.isBlank()
						|| TextSearch.fold(pharmacy.getCommune()).equals(TextSearch.fold(commune)))
				.filter(pharmacy -> status == null || pharmacy.getStatut() == status)
				.filter(pharmacy -> TextSearch.containsAny(search, pharmacy.getName(), pharmacy.getCity(),
						pharmacy.getCommune(), pharmacy.getDistrict(), pharmacy.getPhone1()))
				.collect(Collectors.toList());
	}

	private double reviewScore(String query, Pharmacy pharmacy) {
		if (query.isBlank()) {
			return 1;
		}
		String foldedName = com.buddies.pharmaciegarde.modules.duty.matching.PharmacyNameNormalizer.fold(pharmacy.getName());
		String foldedQuery = com.buddies.pharmaciegarde.modules.duty.matching.PharmacyNameNormalizer.fold(query);
		double score = com.buddies.pharmaciegarde.modules.duty.matching.PharmacyMatcher.jaroWinkler(foldedQuery, foldedName);
		if (!foldedQuery.isEmpty() && foldedName.contains(foldedQuery)) {
			score = Math.max(score, 0.9);
		}
		return Math.round(score * 1000.0) / 1000.0;
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
		return mapAllToResponseDto(pharmacyRepository.findByCity(city));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PharmacyResponseDto> getByCommune(String commune) {
		return mapAllToResponseDto(pharmacyRepository.findByCommune(commune));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PharmacyResponseDto> getByStatut(String statut) {
		try {
			PharmacyStatus status = PharmacyStatus.valueOf(statut.toUpperCase());
			return mapAllToResponseDto(pharmacyRepository.findByStatut(status));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Statut invalide. Les valeurs acceptées sont: ACTIF, INACTIF");
		}
	}

	private PharmacyResponseDto mapToResponseDto(Pharmacy pharmacy) {
		return mapToResponseDto(pharmacy, buildContext(List.of(pharmacy)));
	}

	private List<PharmacyResponseDto> mapAllToResponseDto(List<Pharmacy> pharmacies) {
		EnrichmentContext context = buildContext(pharmacies);
		List<PharmacyResponseDto> result = new ArrayList<>(pharmacies.size());
		for (Pharmacy pharmacy : pharmacies) {
			result.add(mapToResponseDto(pharmacy, context));
		}
		return result;
	}

	private PharmacyResponseDto mapToResponseDto(Pharmacy pharmacy, EnrichmentContext context) {
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
		
		// Notes, derniers avis et garde : lus dans le contexte préchargé
		context.apply(dto, pharmacy.getId());
		
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
		List<RankedPharmacy> candidates = new ArrayList<>();

		for (Pharmacy pharmacy : activePharmacies) {
			if (pharmacy.getLocation() != null && !pharmacy.getLocation().trim().isEmpty()) {
				double[] location = DistanceCalculator.parseLocation(pharmacy.getLocation());
				if (location != null) {
					double distance = DistanceCalculator.calculateDistance(
							clientLat, clientLon, location[0], location[1]);

					candidates.add(new RankedPharmacy(pharmacy, location[0], location[1],
							Math.round(distance * 100.0) / 100.0)); // Arrondir à 2 décimales
				}
			}
		}

		// Trier par distance, garder les N plus proches, puis seulement enrichir ceux-là
		return toDistanceDtos(candidates, limit);
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

		// Récupérer la période de garde réellement en cours (isCurrent + fenêtre de relève)
		java.util.Optional<DutyPeriod> activePeriodOpt = activeDutyPeriodResolver.findActive();

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
		List<RankedPharmacy> candidates = new ArrayList<>();

		for (Pharmacy pharmacy : dutyPharmacies) {
			if (pharmacy.getLocation() != null && !pharmacy.getLocation().trim().isEmpty()) {
				double[] location = DistanceCalculator.parseLocation(pharmacy.getLocation());
				if (location != null) {
					double distance = DistanceCalculator.calculateDistance(
							latitude, longitude, location[0], location[1]);

					candidates.add(new RankedPharmacy(pharmacy, location[0], location[1],
							Math.round(distance * 100.0) / 100.0)); // Arrondir à 2 décimales
				}
			}
		}

		// Trier par distance, garder les N plus proches, puis seulement enrichir ceux-là
		return toDistanceDtos(candidates, limit);
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

		return mapAllToResponseDto(pharmacies);
	}

	private List<PharmacyWithDistanceDto> toDistanceDtos(List<RankedPharmacy> candidates, int limit) {
		List<RankedPharmacy> nearest = candidates.stream()
				.sorted(Comparator.comparingDouble(RankedPharmacy::distanceInKm))
				.limit(limit)
				.collect(Collectors.toList());
		EnrichmentContext context = buildContext(nearest.stream().map(RankedPharmacy::pharmacy).toList());
		List<PharmacyWithDistanceDto> result = new ArrayList<>(nearest.size());
		for (RankedPharmacy ranked : nearest) {
			PharmacyWithDistanceDto dto = mapToPharmacyWithDistanceDto(ranked.pharmacy(), context);
			// Ajouter les coordonnées GPS de la pharmacie
			dto.setLatitude(ranked.latitude());
			dto.setLongitude(ranked.longitude());
			dto.setDistanceInKm(ranked.distanceInKm());
			result.add(dto);
		}
		return result;
	}

	private record RankedPharmacy(Pharmacy pharmacy, double latitude, double longitude, double distanceInKm) {
	}

	private PharmacyWithDistanceDto mapToPharmacyWithDistanceDto(Pharmacy pharmacy, EnrichmentContext context) {
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

		// Notes, derniers avis et garde : lus dans le contexte préchargé
		context.apply(dto, pharmacy.getId());

		return dto;
	}

	/** Au-delà, on agrège tous les avis plutôt que d'envoyer une longue liste d'identifiants. */
	private static final int IN_CLAUSE_LIMIT = 500;

	private static final List<DutyStatus> ON_DUTY_STATUSES = List.of(DutyStatus.ACTIVE, DutyStatus.TO_VERIFY);

	/**
	 * Données d'enrichissement chargées en quelques requêtes pour toute une liste,
	 * au lieu de 3 à 5 requêtes par pharmacie (N+1).
	 */
	private record EnrichmentContext(Set<Long> onDutyIds, java.util.Map<Long, Object[]> ratings,
			java.util.Map<Long, List<PharmacyReviewResponseDto>> recentReviews) {

		void apply(PharmacyResponseDto dto, Long pharmacyId) {
			Object[] rating = ratings.get(pharmacyId);
			Double average = rating == null || rating[1] == null ? null : ((Number) rating[1]).doubleValue();
			long total = rating == null || rating[2] == null ? 0L : ((Number) rating[2]).longValue();
			// Arrondir à 1 décimale
			dto.setAverageRating(average == null ? null : Math.round(average * 10.0) / 10.0);
			dto.setTotalReviews(total);
			List<PharmacyReviewResponseDto> recent = recentReviews.get(pharmacyId);
			dto.setRecentReviews(total > 0 && recent != null ? new ArrayList<>(recent) : new ArrayList<>());
			// De garde = une garde ACTIVE ou TO_VERIFY sur la période réellement en cours
			dto.setIsOnDuty(onDutyIds.contains(pharmacyId));
		}
	}

	private EnrichmentContext buildContext(java.util.Collection<Pharmacy> pharmacies) {
		Set<Long> ids = new HashSet<>();
		for (Pharmacy pharmacy : pharmacies) {
			if (pharmacy.getId() != null) {
				ids.add(pharmacy.getId());
			}
		}
		if (ids.isEmpty()) {
			return new EnrichmentContext(Set.of(), java.util.Map.of(), java.util.Map.of());
		}

		// Garde : période en cours lue une seule fois, puis les pharmacies de garde en une requête
		Set<Long> onDutyIds = new HashSet<>();
		activeDutyPeriodResolver.findActive().ifPresent(period -> onDutyIds.addAll(
				dutyRepository.findPharmacyIdsByDutyPeriodIdAndStatusIn(period.getId(), ON_DUTY_STATUSES)));

		// Notes : moyenne et nombre d'avis agrégés en une requête
		List<Object[]> rows = ids.size() > IN_CLAUSE_LIMIT
				? pharmacyReviewRepository.aggregateRatingsForAll()
				: pharmacyReviewRepository.aggregateRatingsFor(ids);
		java.util.Map<Long, Object[]> ratings = new java.util.HashMap<>();
		Set<Long> reviewed = new HashSet<>();
		for (Object[] row : rows) {
			Long pharmacyId = ((Number) row[0]).longValue();
			if (!ids.contains(pharmacyId)) {
				continue;
			}
			ratings.put(pharmacyId, row);
			if (row[2] != null && ((Number) row[2]).longValue() > 0) {
				reviewed.add(pharmacyId);
			}
		}

		// 3 derniers avis, seulement pour les pharmacies qui en ont, en une requête
		java.util.Map<Long, List<PharmacyReviewResponseDto>> recent = new java.util.HashMap<>();
		if (!reviewed.isEmpty()) {
			for (com.buddies.pharmaciegarde.modules.pharmacy.model.PharmacyReview review
					: pharmacyReviewRepository.findWithClientByPharmacyIdIn(reviewed)) {
				List<PharmacyReviewResponseDto> list = recent.computeIfAbsent(review.getPharmacy().getId(),
						key -> new ArrayList<>(3));
				if (list.size() < 3) {
					list.add(mapReviewToResponseDto(review));
				}
			}
		}
		return new EnrichmentContext(onDutyIds, ratings, recent);
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

