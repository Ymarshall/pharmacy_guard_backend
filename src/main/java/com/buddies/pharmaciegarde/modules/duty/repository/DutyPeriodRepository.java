package com.buddies.pharmaciegarde.modules.duty.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;

@Repository
public interface DutyPeriodRepository extends JpaRepository<DutyPeriod, Long> {

	// Trouver la garde courante (une seule normalement)
	Optional<DutyPeriod> findByIsCurrentTrue();

	// Trouver les gardes par période
	List<DutyPeriod> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate date1, LocalDate date2);

	// Trouver les gardes qui chevauchent une période
	@Query("SELECT dp FROM DutyPeriod dp WHERE " +
			"(dp.startDate <= :endDate AND dp.endDate >= :startDate)")
	List<DutyPeriod> findOverlappingPeriods(@Param("startDate") LocalDate startDate, 
											@Param("endDate") LocalDate endDate);

	// Trouver les gardes actives (courantes)
	List<DutyPeriod> findByIsCurrentTrueOrderByStartDateDesc();
}

