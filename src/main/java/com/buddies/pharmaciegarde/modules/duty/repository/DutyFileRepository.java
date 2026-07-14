package com.buddies.pharmaciegarde.modules.duty.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.buddies.pharmaciegarde.modules.duty.model.DutyFile;
import com.buddies.pharmaciegarde.modules.duty.model.DutyFile.ProcessingStatus;

@Repository
public interface DutyFileRepository extends JpaRepository<DutyFile, Long> {

	List<DutyFile> findByStatus(ProcessingStatus status);

	List<DutyFile> findByStatusOrderByUploadDateDesc(ProcessingStatus status);

	List<DutyFile> findByDutyPeriodId(Long dutyPeriodId);

	List<DutyFile> findByDutyPeriodIdOrderByUploadDateDesc(Long dutyPeriodId);
}

