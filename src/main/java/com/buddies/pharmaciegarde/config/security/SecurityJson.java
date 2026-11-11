package com.buddies.pharmaciegarde.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Réponses JSON écrites hors MVC (filtres et point d'entrée de sécurité).
 * Le charset est explicite : sans lui, le client HTTP suppose du latin-1.
 */
final class SecurityJson {

	static final String CONTENT_TYPE = "application/json;charset=UTF-8";

	private SecurityJson() {
	}

	static void write(HttpServletResponse response, int status, String json) throws IOException {
		byte[] payload = json.getBytes(StandardCharsets.UTF_8);
		response.setStatus(status);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(CONTENT_TYPE);
		response.setContentLength(payload.length);
		response.getOutputStream().write(payload);
	}
}
