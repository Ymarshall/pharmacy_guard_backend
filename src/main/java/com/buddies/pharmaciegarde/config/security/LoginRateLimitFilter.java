package com.buddies.pharmaciegarde.config.security;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limite les tentatives de connexion par adresse IP (fenêtre glissante en mémoire).
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

	private final int maxAttempts;
	private final long windowMillis;
	private final boolean trustForwardedFor;
	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	public LoginRateLimitFilter(int maxAttempts, long windowSeconds, boolean trustForwardedFor) {
		this.maxAttempts = maxAttempts;
		this.windowMillis = windowSeconds * 1000L;
		this.trustForwardedFor = trustForwardedFor;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!isLogin(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		String key = clientKey(request);
		long now = System.currentTimeMillis();
		Window window = windows.compute(key, (k, existing) -> {
			if (existing == null || now - existing.startedAt > windowMillis) {
				return new Window(now, 1);
			}
			existing.count++;
			return existing;
		});
		if (window.count > maxAttempts) {
			SecurityJson.write(response, 429,
					"{\"status\":\"TOO_MANY_REQUESTS\",\"statusCode\":429,\"message\":\"Trop de tentatives de connexion. Réessayez dans une minute.\"}");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean isLogin(HttpServletRequest request) {
		if (!HttpMethod.POST.matches(request.getMethod())) {
			return false;
		}
		String path = request.getRequestURI();
		return "/api/auth/login".equals(path)
				|| "/api/auth/client/login".equals(path)
				|| "/api/auth/client/register".equals(path);
	}

	private String clientKey(HttpServletRequest request) {
		if (trustForwardedFor) {
			String forwarded = request.getHeader("X-Forwarded-For");
			if (forwarded != null && !forwarded.isBlank()) {
				return forwarded.split(",")[0].trim();
			}
		}
		return request.getRemoteAddr();
	}

	private static final class Window {
		private final long startedAt;
		private int count;

		private Window(long startedAt, int count) {
			this.startedAt = startedAt;
			this.count = count;
		}
	}
}
