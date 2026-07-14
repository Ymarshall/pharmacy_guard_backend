package com.buddies.pharmaciegarde.modules.pharmacy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.buddies.pharmaciegarde.modules.pharmacy.model.PharmacyReview;

@Repository
public interface PharmacyReviewRepository extends JpaRepository<PharmacyReview, Long> {

	List<PharmacyReview> findByPharmacyId(Long pharmacyId);

	Optional<PharmacyReview> findByPharmacyIdAndClientId(Long pharmacyId, Long clientId);

	@Query("SELECT AVG(r.rating) FROM PharmacyReview r WHERE r.pharmacy.id = :pharmacyId")
	Double findAverageRatingByPharmacyId(@Param("pharmacyId") Long pharmacyId);

	Long countByPharmacyId(Long pharmacyId);

	List<PharmacyReview> findByPharmacyIdOrderByCreatedAtDesc(Long pharmacyId, Pageable pageable);

	boolean existsByPharmacyIdAndClientId(Long pharmacyId, Long clientId);
}
