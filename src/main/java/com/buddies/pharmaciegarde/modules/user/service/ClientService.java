package com.buddies.pharmaciegarde.modules.user.service;

import java.util.List;

import com.buddies.pharmaciegarde.modules.user.dto.ClientCreateDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientLoginDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientLoginResponseDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientResponseDto;
import com.buddies.pharmaciegarde.modules.user.dto.LocationUpdateDto;

public interface ClientService {

	ClientResponseDto create(ClientCreateDto clientCreateDto);

	ClientResponseDto getById(Long id);

	List<ClientResponseDto> getAll();

	ClientResponseDto update(Long id, ClientCreateDto clientCreateDto);

	void delete(Long id);

	ClientLoginResponseDto login(ClientLoginDto clientLoginDto);

	ClientResponseDto updateLocation(Long id, LocationUpdateDto locationUpdateDto);

	ClientResponseDto updateStatus(Long id, Boolean isActive);
}

