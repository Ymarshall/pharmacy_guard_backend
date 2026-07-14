package com.buddies.pharmaciegarde.modules.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buddies.pharmaciegarde.modules.user.dto.LoginDto;
import com.buddies.pharmaciegarde.modules.user.dto.UserResponseDto;
import com.buddies.pharmaciegarde.modules.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API d'authentification")
public class AuthController {

	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	@Operation(summary = "Connexion", description = "Authentifie un utilisateur avec son email et mot de passe")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Connexion réussie",
					content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Email ou mot de passe incorrect")
	})
	@PostMapping("/login")
	public ResponseEntity<UserResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
		UserResponseDto user = userService.login(loginDto);
		return ResponseEntity.ok(user);
	}
}

