package com.buddies.pharmaciegarde.modules.common.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.buddies.pharmaciegarde.modules.common.model.EmergencyNumber;

@Repository
public interface EmergencyNumberRepository extends JpaRepository<EmergencyNumber, Long> {

	Optional<EmergencyNumber> findByName(String name);

	Optional<EmergencyNumber> findByNumber(String number);

	boolean existsByName(String name);

	boolean existsByNumber(String number);
}

