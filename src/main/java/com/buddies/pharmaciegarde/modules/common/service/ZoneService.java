package com.buddies.pharmaciegarde.modules.common.service;

import java.util.List;

import com.buddies.pharmaciegarde.modules.common.dto.ZoneCreateDto;
import com.buddies.pharmaciegarde.modules.common.dto.ZoneResponseDto;
import com.buddies.pharmaciegarde.modules.common.dto.ZoneUpdateDto;

public interface ZoneService {

	List<ZoneResponseDto> getAllZones();

	ZoneResponseDto getZoneById(Long id);

	ZoneResponseDto createZone(ZoneCreateDto createDto);

	ZoneResponseDto updateZone(Long id, ZoneUpdateDto updateDto);

	void deleteZone(Long id);

	List<ZoneResponseDto> getActiveZones();

	List<ZoneResponseDto> getZonesByType(String type);

	List<ZoneResponseDto> getZonesByRegion(String region);
}

