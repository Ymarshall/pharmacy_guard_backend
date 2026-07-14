package com.buddies.pharmaciegarde.modules.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.buddies.pharmaciegarde.modules.common.model.Zone;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

	Optional<Zone> findByName(String name);

	List<Zone> findByType(String type);

	List<Zone> findByRegion(String region);

	List<Zone> findByIsActive(Boolean isActive);

	List<Zone> findByTypeAndIsActive(String type, Boolean isActive);

	// Recherche par nom (insensible à la casse)
	Optional<Zone> findByNameIgnoreCase(String name);

	// Recherche par nom contenant (insensible à la casse)
	List<Zone> findByNameContainingIgnoreCaseAndIsActive(String name, Boolean isActive);

	// Recherche pour le parser : trouver une zone par nom exact ou partiel
	@Query("SELECT z FROM Zone z WHERE LOWER(z.name) = LOWER(:name) AND z.isActive = true")
	Optional<Zone> findActiveZoneByNameIgnoreCase(@Param("name") String name);

	@Query("SELECT z FROM Zone z WHERE LOWER(z.name) LIKE LOWER(CONCAT(:name, '%')) AND z.isActive = true")
	List<Zone> findActiveZonesByNameStartingWithIgnoreCase(@Param("name") String name);
}

