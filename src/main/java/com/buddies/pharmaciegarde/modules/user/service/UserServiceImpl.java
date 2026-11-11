package com.buddies.pharmaciegarde.modules.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.common.api.PageRequests;
import com.buddies.pharmaciegarde.common.api.PageResponse;
import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;
import com.buddies.pharmaciegarde.config.security.AuthPrincipal;
import com.buddies.pharmaciegarde.config.security.JwtService;
import com.buddies.pharmaciegarde.config.security.SecurityUtils;
import com.buddies.pharmaciegarde.modules.user.dto.LoginDto;
import com.buddies.pharmaciegarde.modules.user.dto.PasswordUpdateDto;
import com.buddies.pharmaciegarde.modules.user.dto.UserCreateDto;
import com.buddies.pharmaciegarde.modules.user.dto.UserResponseDto;
import com.buddies.pharmaciegarde.modules.user.model.User;
import com.buddies.pharmaciegarde.modules.user.model.UserRole;
import com.buddies.pharmaciegarde.modules.user.repository.UserRepository;

@Service
@Transactional
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
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
		user.setRole(parseRole(userCreateDto.getRole(), UserRole.OPERATOR));

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
	@Transactional(readOnly = true)
	public PageResponse<UserResponseDto> getPage(int page, int size, String search, String sort) {
		org.springframework.data.domain.Sort parsed = com.buddies.pharmaciegarde.common.api.Sorts.parseOr(sort,
				java.util.Set.of("id", "email", "firstName", "lastName", "role"),
				org.springframework.data.domain.Sort.by("email"));
		if (search == null || search.isBlank()) {
			return PageResponse.from(userRepository.findAll(PageRequests.of(page, size, parsed)).map(this::mapToResponseDto));
		}
		return PageResponse.from(com.buddies.pharmaciegarde.common.api.Slices.of(filterUsers(search, parsed), page, size)
				.map(this::mapToResponseDto));
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserResponseDto> list(String search, String sort) {
		org.springframework.data.domain.Sort parsed = com.buddies.pharmaciegarde.common.api.Sorts.parseOr(sort,
				java.util.Set.of("id", "email", "firstName", "lastName", "role"),
				org.springframework.data.domain.Sort.by("email"));
		return filterUsers(search, parsed).stream().map(this::mapToResponseDto).collect(Collectors.toList());
	}

	private List<User> filterUsers(String search, org.springframework.data.domain.Sort parsed) {
		String field = com.buddies.pharmaciegarde.common.api.Sorts.field(parsed, "email");
		boolean desc = com.buddies.pharmaciegarde.common.api.Sorts.isDescending(parsed);
		java.util.Comparator<User> comparator = switch (field) {
			case "id" -> java.util.Comparator.comparing(User::getId);
			case "firstName" -> java.util.Comparator.comparing(User::getFirstName, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
			case "lastName" -> java.util.Comparator.comparing(User::getLastName, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
			case "role" -> java.util.Comparator.comparing(user -> user.getRole() == null ? "" : user.getRole().name());
			default -> java.util.Comparator.comparing(User::getEmail, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
		};
		if (desc) {
			comparator = comparator.reversed();
		}
		return userRepository.findAll().stream()
				.filter(user -> com.buddies.pharmaciegarde.common.api.TextSearch.containsAny(search, user.getEmail(),
						user.getFirstName(), user.getLastName(), user.getRole() == null ? null : user.getRole().name()))
				.sorted(comparator)
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
		if (userCreateDto.getRole() != null && !userCreateDto.getRole().isBlank()) {
			user.setRole(parseRole(userCreateDto.getRole(), user.getRole()));
		}
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

		UserResponseDto dto = mapToResponseDto(user);
		UserRole role = user.getRole() == null ? UserRole.ADMIN : user.getRole();
		dto.setToken(jwtService.generate(new AuthPrincipal(user.getId(), user.getEmail(), role.name(), AuthPrincipal.KIND_USER)));
		dto.setTokenType("Bearer");
		dto.setExpiresInSeconds(jwtService.getExpirationSeconds());
		return dto;
	}

	@Override
	public UserResponseDto updatePassword(Long id, PasswordUpdateDto passwordUpdateDto) {
		SecurityUtils.assertSelfOrAdmin(id);
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
		dto.setRole(user.getRole() == null ? UserRole.ADMIN.name() : user.getRole().name());
		return dto;
	}

	private UserRole parseRole(String role, UserRole fallback) {
		if (role == null || role.isBlank()) {
			return fallback;
		}
		try {
			return UserRole.valueOf(role.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Rôle invalide. Valeurs acceptées : ADMIN, OPERATOR");
		}
	}
}

