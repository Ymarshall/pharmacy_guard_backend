package com.buddies.pharmaciegarde.config.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;
	private final long expirationMs;

	public JwtService(
			@Value("${app.jwt.secret:}") String secret,
			@Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
		if (secret == null || secret.trim().length() < 32) {
			throw new IllegalStateException(
					"JWT_SECRET est obligatoire et doit contenir au moins 32 caractères. "
							+ "Définissez la variable d'environnement JWT_SECRET.");
		}
		this.key = Keys.hmacShaKeyFor(secret.trim().getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String generate(AuthPrincipal principal) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMs);
		return Jwts.builder()
				.subject(String.valueOf(principal.id()))
				.claim("role", principal.role())
				.claim("kind", principal.kind())
				.claim("name", principal.username())
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key)
				.compact();
	}

	public AuthPrincipal parse(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		Long id = Long.valueOf(claims.getSubject());
		String role = claims.get("role", String.class);
		String kind = claims.get("kind", String.class);
		String name = claims.get("name", String.class);
		if (id == null || role == null || kind == null) {
			throw new JwtException("Jeton incomplet");
		}
		return new AuthPrincipal(id, name, role, kind);
	}

	public long getExpirationSeconds() {
		return expirationMs / 1000;
	}
}
