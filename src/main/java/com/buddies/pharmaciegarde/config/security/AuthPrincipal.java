package com.buddies.pharmaciegarde.config.security;

public record AuthPrincipal(Long id, String username, String role, String kind) {

	public static final String KIND_USER = "USER";
	public static final String KIND_CLIENT = "CLIENT";
	public static final String ROLE_ADMIN = "ADMIN";
	public static final String ROLE_OPERATOR = "OPERATOR";
	public static final String ROLE_CLIENT = "CLIENT";
}
