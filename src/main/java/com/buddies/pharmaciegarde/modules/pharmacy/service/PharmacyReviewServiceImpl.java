package com.buddies.pharmaciegarde.modules.pharmacy.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyRatingSummaryDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyReviewCreateDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.PharmacyReviewResponseDto;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy;
import com.buddies.pharmaciegarde.modules.pharmacy.model.PharmacyReview;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyRepository;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyReviewRepository;
import com.buddies.pharmaciegarde.modules.user.model.Client;
import com.buddies.pharmaciegarde.modules.user.repository.ClientRepository;

@Service
@Transactional
public class PharmacyReviewServiceImpl implements PharmacyReviewService {

	private final PharmacyReviewRepository pharmacyReviewRepository;
	private final PharmacyRepository pharmacyRepository;
	private final ClientRepository clientRepository;

	public PharmacyReviewServiceImpl(PharmacyReviewRepository pharmacyReviewRepository,
			PharmacyRepository pharmacyRepository, ClientRepository clientRepository) {
		this.pharmacyReviewRepository = pharmacyReviewRepository;
		this.pharmacyRepository = pharmacyRepository;
		this.clientRepository = clientRepository;
	}

	@Override
	public PharmacyReviewResponseDto createOrUpdate(PharmacyReviewCreateDto dto) {
		com.buddies.pharmaciegarde.config.security.SecurityUtils.assertClientSelfOrBackoffice(dto.getClientId());
		// Vérifier que la pharmacie existe
		Pharmacy pharmacy = pharmacyRepository.findById(dto.getPharmacyId())
				.orElseThrow(() -> new ResourceNotFoundException("Pharmacie", dto.getPharmacyId()));

		// Vérifier que le client existe
		Client client = clientRepository.findById(dto.getClientId())
				.orElseThrow(() -> new ResourceNotFoundException("Client", dto.getClientId()));

		// Vérifier si le client a déjà commenté cette pharmacie
		java.util.Optional<PharmacyReview> existingReview = pharmacyReviewRepository
				.findByPharmacyIdAndClientId(dto.getPharmacyId(), dto.getClientId());

		PharmacyReview review;
		if (existingReview.isPresent()) {
			// Mettre à jour le commentaire existant
			review = existingReview.get();
			review.setRating(dto.getRating());
			review.setComment(dto.getComment());
		} else {
			// Créer un nouveau commentaire
			review = new PharmacyReview();
			review.setPharmacy(pharmacy);
			review.setClient(client);
			review.setRating(dto.getRating());
			review.setComment(dto.getComment());
		}

		PharmacyReview savedReview = pharmacyReviewRepository.save(review);
		return mapToResponseDto(savedReview);
	}

	@Override
	public PharmacyReviewResponseDto update(Long reviewId, Long clientId, PharmacyReviewCreateDto dto) {
		com.buddies.pharmaciegarde.config.security.SecurityUtils.assertClientSelfOrBackoffice(clientId);
		// Vérifier que le commentaire existe
		PharmacyReview review = pharmacyReviewRepository.findById(reviewId)
				.orElseThrow(() -> new ResourceNotFoundException("Commentaire", reviewId));

		// Vérifier que c'est le commentaire du client
		if (!review.getClient().getId().equals(clientId)) {
			throw new IllegalArgumentException("Vous ne pouvez modifier que vos propres commentaires");
		}

		// Vérifier que la pharmacie existe
		Pharmacy pharmacy = pharmacyRepository.findById(dto.getPharmacyId())
				.orElseThrow(() -> new ResourceNotFoundException("Pharmacie", dto.getPharmacyId()));

		// Mettre à jour
		review.setPharmacy(pharmacy);
		review.setRating(dto.getRating());
		review.setComment(dto.getComment());

		PharmacyReview updatedReview = pharmacyReviewRepository.save(review);
		return mapToResponseDto(updatedReview);
	}

	@Override
	public void delete(Long reviewId, Long clientId) {
		com.buddies.pharmaciegarde.config.security.SecurityUtils.assertClientSelfOrBackoffice(clientId);
		// Vérifier que le commentaire existe
		PharmacyReview review = pharmacyReviewRepository.findById(reviewId)
				.orElseThrow(() -> new ResourceNotFoundException("Commentaire", reviewId));

		// Vérifier que c'est le commentaire du client
		if (!review.getClient().getId().equals(clientId)) {
			throw new IllegalArgumentException("Vous ne pouvez supprimer que vos propres commentaires");
		}

		pharmacyReviewRepository.delete(review);
	}

	@Override
	public void deleteById(Long reviewId) {
		// Vérifier que le commentaire existe
		if (!pharmacyReviewRepository.existsById(reviewId)) {
			throw new ResourceNotFoundException("Commentaire", reviewId);
		}

		pharmacyReviewRepository.deleteById(reviewId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PharmacyReviewResponseDto> getByPharmacyId(Long pharmacyId, int page, int size) {
		// Vérifier que la pharmacie existe
		if (!pharmacyRepository.existsById(pharmacyId)) {
			throw new ResourceNotFoundException("Pharmacie", pharmacyId);
		}

		Pageable pageable = PageRequest.of(page, size);
		List<PharmacyReview> reviews = pharmacyReviewRepository.findByPharmacyIdOrderByCreatedAtDesc(pharmacyId,
				pageable);

		return reviews.stream().map(this::mapToResponseDto).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public PharmacyRatingSummaryDto getRatingSummary(Long pharmacyId) {
		// Vérifier que la pharmacie existe
		if (!pharmacyRepository.existsById(pharmacyId)) {
			throw new ResourceNotFoundException("Pharmacie", pharmacyId);
		}

		// Calculer la note moyenne
		Double averageRating = pharmacyReviewRepository.findAverageRatingByPharmacyId(pharmacyId);
		if (averageRating == null) {
			averageRating = 0.0;
		}

		// Compter le nombre total de commentaires
		Long totalReviews = pharmacyReviewRepository.countByPharmacyId(pharmacyId);

		// Calculer la distribution des notes
		List<PharmacyReview> allReviews = pharmacyReviewRepository.findByPharmacyId(pharmacyId);
		Map<Integer, Long> ratingDistribution = new HashMap<>();
		for (int i = 1; i <= 5; i++) {
			ratingDistribution.put(i, 0L);
		}

		for (PharmacyReview review : allReviews) {
			Integer rating = review.getRating();
			ratingDistribution.put(rating, ratingDistribution.get(rating) + 1);
		}

		PharmacyRatingSummaryDto summary = new PharmacyRatingSummaryDto();
		summary.setAverageRating(Math.round(averageRating * 10.0) / 10.0); // Arrondir à 1 décimale
		summary.setTotalReviews(totalReviews);
		summary.setRatingDistribution(ratingDistribution);

		return summary;
	}

	@Override
	@Transactional(readOnly = true)
	public PharmacyReviewResponseDto getById(Long reviewId) {
		PharmacyReview review = pharmacyReviewRepository.findById(reviewId)
				.orElseThrow(() -> new ResourceNotFoundException("Commentaire", reviewId));
		return mapToResponseDto(review);
	}

	private PharmacyReviewResponseDto mapToResponseDto(PharmacyReview review) {
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
