package com.buddies.pharmaciegarde.modules.pharmacy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.buddies.pharmaciegarde.modules.pharmacy.model.Assurance;

@Repository
public interface AssuranceRepository extends JpaRepository<Assurance, Long> {

	Optional<Assurance> findByName(String name);

	boolean existsByName(String name);
}

