package com.buddies.pharmaciegarde.config.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static AuthPrincipal current() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
			return null;
		}
		return principal;
	}

	public static AuthPrincipal requirePrincipal() {
		AuthPrincipal principal = current();
		if (principal == null) {
			throw new AccessDeniedException("Authentification requise");
		}
		return principal;
	}

	/**
	 * Un client ne peut agir que sur son propre identifiant.
	 * Un compte back-office (admin ou opérateur) peut agir sur un client identifié.
	 */
	public static void assertClientSelfOrBackoffice(Long clientId) {
		AuthPrincipal principal = requirePrincipal();
		if (AuthPrincipal.KIND_CLIENT.equals(principal.kind())) {
			if (clientId == null || !principal.id().equals(clientId)) {
				throw new AccessDeniedException("Vous ne pouvez agir que sur votre propre compte");
			}
			return;
		}
		if (AuthPrincipal.ROLE_ADMIN.equals(principal.role())
				|| AuthPrincipal.ROLE_OPERATOR.equals(principal.role())) {
			return;
		}
		throw new AccessDeniedException("Accès refusé");
	}

	public static void assertSelfOrAdmin(Long userId) {
		AuthPrincipal principal = requirePrincipal();
		if (principal.id().equals(userId) || AuthPrincipal.ROLE_ADMIN.equals(principal.role())) {
			return;
		}
		throw new AccessDeniedException("Vous ne pouvez modifier que votre propre mot de passe");
	}
}
