package com.buddies.pharmaciegarde.modules.pharmacy.service;

import java.util.List;

import com.buddies.pharmaciegarde.modules.pharmacy.dto.AssuranceCreateDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.AssuranceResponseDto;

public interface AssuranceService {

	AssuranceResponseDto create(AssuranceCreateDto assuranceCreateDto);

	AssuranceResponseDto getById(Long id);

	List<AssuranceResponseDto> getAll();

	AssuranceResponseDto update(Long id, AssuranceCreateDto assuranceCreateDto);

	void delete(Long id);
}

