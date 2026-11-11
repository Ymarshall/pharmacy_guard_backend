package com.buddies.pharmaciegarde.modules.duty.service;

import java.util.List;

import com.buddies.pharmaciegarde.modules.duty.dto.DutyPeriodCreateDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyPeriodResponseDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyPeriodUpdateDto;

public interface DutyPeriodService {

	List<DutyPeriodResponseDto> getAllPeriods();

	com.buddies.pharmaciegarde.common.api.PageResponse<DutyPeriodResponseDto> getPage(int page, int size);

	DutyPeriodResponseDto getPeriodById(Long id);

	DutyPeriodResponseDto createPeriod(DutyPeriodCreateDto createDto);

	DutyPeriodResponseDto updatePeriod(Long id, DutyPeriodUpdateDto updateDto);

	void deletePeriod(Long id);

	DutyPeriodResponseDto getCurrentPeriod();

	List<DutyPeriodResponseDto> getCurrentPeriods();

	DutyPeriodResponseDto activatePeriod(Long id);
}

