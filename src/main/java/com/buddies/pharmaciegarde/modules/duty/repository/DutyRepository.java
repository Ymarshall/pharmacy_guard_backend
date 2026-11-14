package com.buddies.pharmaciegarde.modules.duty.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.buddies.pharmaciegarde.modules.duty.model.Duty;
import com.buddies.pharmaciegarde.modules.duty.model.DutyStatus;

@Repository
public interface DutyRepository extends JpaRepository<Duty, Long> {

	@Override
	@EntityGraph(attributePaths = {"pharmacy", "dutyPeriod", "dutyFile"})
	List<Duty> findAll();

	@Override
	@EntityGraph(attributePaths = {"pharmacy", "dutyPeriod", "dutyFile"})
	org.springframework.data.domain.Page<Duty> findAll(org.springframework.data.domain.Pageable pageable);

	@EntityGraph(attributePaths = {"pharmacy", "dutyPeriod", "dutyFile"})
	List<Duty> findByPharmacyId(Long pharmacyId);

	@EntityGraph(attributePaths = {"pharmacy", "dutyPeriod", "dutyFile"})
	List<Duty> findByDutyPeriodId(Long dutyPeriodId);

	@EntityGraph(attributePaths = {"pharmacy", "dutyPeriod", "dutyFile"})
	@Query("SELECT d FROM Duty d WHERE d.pharmacy.id = :pharmacyId AND d.dutyPeriod.id = :dutyPeriodId")
	List<Duty> findByPharmacyIdAndDutyPeriodId(@Param("pharmacyId") Long pharmacyId,
			@Param("dutyPeriodId") Long dutyPeriodId);

	List<Duty> findByDutyFileId(Long dutyFileId);

	List<Duty> findByDutyPeriodIdIn(List<Long> dutyPeriodIds);

	// Méthodes de recherche par statut
	List<Duty> findByStatus(DutyStatus status);

	List<Duty> findByDutyPeriodIdAndStatus(Long dutyPeriodId, DutyStatus status);

	@Query("SELECT d FROM Duty d WHERE d.dutyPeriod.id = :dutyPeriodId AND d.status IN :statuses")
	List<Duty> findByDutyPeriodIdAndStatusIn(@Param("dutyPeriodId") Long dutyPeriodId,
			@Param("statuses") List<DutyStatus> statuses);

	/** Identifiants des pharmacies de garde sur une période, en une requête. */
	@Query("SELECT DISTINCT d.pharmacy.id FROM Duty d WHERE d.dutyPeriod.id = :dutyPeriodId AND d.status IN :statuses")
	List<Long> findPharmacyIdsByDutyPeriodIdAndStatusIn(@Param("dutyPeriodId") Long dutyPeriodId,
			@Param("statuses") List<DutyStatus> statuses);
}

