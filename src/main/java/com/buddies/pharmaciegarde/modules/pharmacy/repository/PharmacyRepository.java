package com.buddies.pharmaciegarde.modules.pharmacy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy.PharmacyStatus;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

	Optional<Pharmacy> findByName(String name);

	List<Pharmacy> findByCity(String city);

	List<Pharmacy> findByCommune(String commune);

	List<Pharmacy> findByStatut(PharmacyStatus statut);

	Page<Pharmacy> findByCity(String city, Pageable pageable);

	Page<Pharmacy> findByCommune(String commune, Pageable pageable);

	Page<Pharmacy> findByStatut(PharmacyStatus statut, Pageable pageable);

	List<Pharmacy> findByCityAndStatut(String city, PharmacyStatus statut);

	boolean existsByName(String name);

	boolean existsByPhone1(String phone1);

	// Recherches pour les clients
	List<Pharmacy> findByNameStartingWithIgnoreCaseAndStatut(String name, PharmacyStatus statut);

	List<Pharmacy> findByCityContainingIgnoreCaseAndStatut(String city, PharmacyStatus statut);

	List<Pharmacy> findByCommuneContainingIgnoreCaseAndStatut(String commune, PharmacyStatus statut);

	List<Pharmacy> findByDistrictContainingIgnoreCaseAndStatut(String district, PharmacyStatus statut);

	@Query("SELECT p FROM Pharmacy p WHERE p.statut = :statut AND " +
			"(LOWER(p.name) LIKE LOWER(CONCAT(:name, '%')) OR " +
			"LOWER(p.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
			"LOWER(p.commune) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
			"LOWER(p.district) LIKE LOWER(CONCAT('%', :search, '%')))")
	List<Pharmacy> searchPharmacies(@Param("search") String search, @Param("name") String name, @Param("statut") PharmacyStatus statut);

	@Query(value = "SELECT * FROM pharmacies WHERE lower(name) = lower(cast(:name as varchar)) AND "
			+ "lower(commune) = lower(cast(:commune as varchar)) AND lower(city) = lower(cast(:city as varchar)) AND "
			+ "((cast(:district as varchar) IS NULL AND district IS NULL) OR "
			+ "(district IS NOT NULL AND lower(district) = lower(cast(:district as varchar))))",
			nativeQuery = true)
	Optional<Pharmacy> findByNameAndCommuneAndCityAndDistrict(
			@Param("name") String name,
			@Param("commune") String commune,
			@Param("city") String city,
			@Param("district") String district);
}

