package com.buddies.pharmaciegarde.modules.user.service;

import java.util.List;

import com.buddies.pharmaciegarde.modules.user.dto.ClientCreateDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientLoginDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientLoginResponseDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientRegisterDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientResponseDto;
import com.buddies.pharmaciegarde.modules.user.dto.LocationUpdateDto;

public interface ClientService {

	ClientResponseDto create(ClientCreateDto clientCreateDto);

	ClientLoginResponseDto register(ClientRegisterDto clientRegisterDto);

	ClientResponseDto getById(Long id);

	List<ClientResponseDto> getAll();

	com.buddies.pharmaciegarde.common.api.PageResponse<ClientResponseDto> getPage(int page, int size, String search,
			String sort);

	List<ClientResponseDto> list(String search, String sort);

	ClientResponseDto update(Long id, ClientCreateDto clientCreateDto);

	void delete(Long id);

	ClientLoginResponseDto login(ClientLoginDto clientLoginDto);

	ClientResponseDto updateLocation(Long id, LocationUpdateDto locationUpdateDto);

	ClientResponseDto updateStatus(Long id, Boolean isActive);
}

