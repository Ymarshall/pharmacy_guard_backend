package com.buddies.pharmaciegarde.modules.user.service;

import java.util.List;

import com.buddies.pharmaciegarde.common.api.PageResponse;
import com.buddies.pharmaciegarde.modules.user.dto.LoginDto;
import com.buddies.pharmaciegarde.modules.user.dto.PasswordUpdateDto;
import com.buddies.pharmaciegarde.modules.user.dto.UserCreateDto;
import com.buddies.pharmaciegarde.modules.user.dto.UserResponseDto;

public interface UserService {

	UserResponseDto create(UserCreateDto userCreateDto);

	UserResponseDto getById(Long id);

	List<UserResponseDto> getAll();

	PageResponse<UserResponseDto> getPage(int page, int size);

	UserResponseDto update(Long id, UserCreateDto userCreateDto);

	void delete(Long id);

	UserResponseDto login(LoginDto loginDto);

	UserResponseDto updatePassword(Long id, PasswordUpdateDto passwordUpdateDto);
}

