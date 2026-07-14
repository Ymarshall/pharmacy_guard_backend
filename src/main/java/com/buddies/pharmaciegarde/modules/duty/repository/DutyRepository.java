package com.buddies.pharmaciegarde.modules.duty.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.buddies.pharmaciegarde.modules.duty.model.Duty;
import com.buddies.pharmaciegarde.modules.duty.model.DutyStatus;

@Repository
public interface DutyRepository extends JpaRepository<Duty, Long> {

	List<Duty> findByPharmacyId(Long pharmacyId);

	List<Duty> findByDutyPeriodId(Long dutyPeriodId);

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
}

