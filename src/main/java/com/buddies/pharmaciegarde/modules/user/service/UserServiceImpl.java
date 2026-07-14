package com.buddies.pharmaciegarde.modules.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;
import com.buddies.pharmaciegarde.modules.user.dto.LoginDto;
import com.buddies.pharmaciegarde.modules.user.dto.PasswordUpdateDto;
import com.buddies.pharmaciegarde.modules.user.dto.UserCreateDto;
import com.buddies.pharmaciegarde.modules.user.dto.UserResponseDto;
import com.buddies.pharmaciegarde.modules.user.model.User;
import com.buddies.pharmaciegarde.modules.user.repository.UserRepository;

@Service
@Transactional
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public UserResponseDto create(UserCreateDto userCreateDto) {
		// Vérifier si un utilisateur avec le même email existe déjà
		if (userRepository.existsByEmail(userCreateDto.getEmail())) {
			throw new IllegalArgumentException("Un utilisateur avec l'email '" + userCreateDto.getEmail() + "' existe déjà");
		}

		User user = new User();
		user.setEmail(userCreateDto.getEmail());
		user.setPassword(passwordEncoder.encode(userCreateDto.getPassword()));
		user.setFirstName(userCreateDto.getFirstName());
		user.setLastName(userCreateDto.getLastName());

		User savedUser = userRepository.save(user);
		return mapToResponseDto(savedUser);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponseDto getById(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
		return mapToResponseDto(user);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserResponseDto> getAll() {
		return userRepository.findAll().stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	public UserResponseDto update(Long id, UserCreateDto userCreateDto) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));

		// Vérifier si l'email est modifié et s'il existe déjà
		if (!user.getEmail().equals(userCreateDto.getEmail()) 
				&& userRepository.existsByEmail(userCreateDto.getEmail())) {
			throw new IllegalArgumentException("Un utilisateur avec l'email '" + userCreateDto.getEmail() + "' existe déjà");
		}

		user.setEmail(userCreateDto.getEmail());
		user.setFirstName(userCreateDto.getFirstName());
		user.setLastName(userCreateDto.getLastName());
		// Ne pas mettre à jour le mot de passe ici, utiliser updatePassword pour cela

		User updatedUser = userRepository.save(user);
		return mapToResponseDto(updatedUser);
	}

	@Override
	public void delete(Long id) {
		if (!userRepository.existsById(id)) {
			throw new ResourceNotFoundException("Utilisateur", id);
		}
		userRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponseDto login(LoginDto loginDto) {
		User user = userRepository.findByEmail(loginDto.getEmail())
				.orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe incorrect"));

		if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
			throw new IllegalArgumentException("Email ou mot de passe incorrect");
		}

		return mapToResponseDto(user);
	}

	@Override
	public UserResponseDto updatePassword(Long id, PasswordUpdateDto passwordUpdateDto) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));

		// Vérifier l'ancien mot de passe
		if (!passwordEncoder.matches(passwordUpdateDto.getOldPassword(), user.getPassword())) {
			throw new IllegalArgumentException("L'ancien mot de passe est incorrect");
		}

		// Mettre à jour avec le nouveau mot de passe hashé
		user.setPassword(passwordEncoder.encode(passwordUpdateDto.getNewPassword()));

		User updatedUser = userRepository.save(user);
		return mapToResponseDto(updatedUser);
	}

	private UserResponseDto mapToResponseDto(User user) {
		UserResponseDto dto = new UserResponseDto();
		dto.setId(user.getId());
		dto.setEmail(user.getEmail());
		dto.setFirstName(user.getFirstName());
		dto.setLastName(user.getLastName());
		return dto;
	}
}

