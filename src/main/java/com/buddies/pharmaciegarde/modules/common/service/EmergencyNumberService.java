package com.buddies.pharmaciegarde.modules.common.service;

import java.util.List;

import com.buddies.pharmaciegarde.modules.common.dto.EmergencyNumberCreateDto;
import com.buddies.pharmaciegarde.modules.common.dto.EmergencyNumberResponseDto;

public interface EmergencyNumberService {

	EmergencyNumberResponseDto create(EmergencyNumberCreateDto emergencyNumberCreateDto);

	EmergencyNumberResponseDto getById(Long id);

	List<EmergencyNumberResponseDto> getAll();

	com.buddies.pharmaciegarde.common.api.PageResponse<EmergencyNumberResponseDto> getPage(int page, int size);

	EmergencyNumberResponseDto update(Long id, EmergencyNumberCreateDto emergencyNumberCreateDto);

	void delete(Long id);
}

