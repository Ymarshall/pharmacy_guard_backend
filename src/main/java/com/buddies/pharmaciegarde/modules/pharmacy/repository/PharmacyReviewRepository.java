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

	/** Moyenne et nombre d'avis par pharmacie, en une requête : {pharmacyId, avg, count}. */
	@Query("SELECT r.pharmacy.id, AVG(r.rating), COUNT(r) FROM PharmacyReview r GROUP BY r.pharmacy.id")
	List<Object[]> aggregateRatingsForAll();

	/** Idem, limité à quelques pharmacies (fiche, page, petite liste). */
	@Query("SELECT r.pharmacy.id, AVG(r.rating), COUNT(r) FROM PharmacyReview r "
			+ "WHERE r.pharmacy.id IN :pharmacyIds GROUP BY r.pharmacy.id")
	List<Object[]> aggregateRatingsFor(@Param("pharmacyIds") java.util.Collection<Long> pharmacyIds);

	/** Avis des pharmacies données, client et pharmacie chargés, du plus récent au plus ancien. */
	@Query("SELECT r FROM PharmacyReview r JOIN FETCH r.pharmacy JOIN FETCH r.client "
			+ "WHERE r.pharmacy.id IN :pharmacyIds ORDER BY r.createdAt DESC")
	List<PharmacyReview> findWithClientByPharmacyIdIn(@Param("pharmacyIds") java.util.Collection<Long> pharmacyIds);
}
