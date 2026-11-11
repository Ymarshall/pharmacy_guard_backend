package com.buddies.pharmaciegarde.config.security;

import java.util.function.Supplier;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import com.buddies.pharmaciegarde.modules.user.repository.UserRepository;

/**
 * Autorise la création du premier utilisateur tant que la table est vide,
 * puis réserve POST /api/users aux administrateurs.
 */
@Component
public class FirstUserAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

	private final UserRepository userRepository;

	public FirstUserAuthorizationManager(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@SuppressWarnings("deprecation")
	public AuthorizationDecision check(Supplier<Authentication> authentication,
			RequestAuthorizationContext object) {
		if (userRepository.count() == 0) {
			return new AuthorizationDecision(true);
		}
		Authentication auth = authentication.get();
		boolean admin = auth != null && auth.getAuthorities() != null
				&& auth.getAuthorities().stream().anyMatch(granted -> "ROLE_ADMIN".equals(granted.getAuthority()));
		return new AuthorizationDecision(admin);
	}
}
