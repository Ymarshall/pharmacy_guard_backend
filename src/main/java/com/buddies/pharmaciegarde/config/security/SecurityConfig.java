package com.buddies.pharmaciegarde.config.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtService jwtService;
	private final FirstUserAuthorizationManager firstUserAuthorizationManager;

	@Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:3000,http://127.0.0.1:4200}")
	private String allowedOrigins;

	@Value("${app.swagger.enabled:true}")
	private boolean swaggerEnabled;

	@Value("${app.security.login-max-attempts:10}")
	private int loginMaxAttempts;

	@Value("${app.security.login-window-seconds:60}")
	private long loginWindowSeconds;

	@Value("${app.security.trust-forwarded-for:false}")
	private boolean trustForwardedFor;

	public SecurityConfig(JwtService jwtService, FirstUserAuthorizationManager firstUserAuthorizationManager) {
		this.jwtService = jwtService;
		this.firstUserAuthorizationManager = firstUserAuthorizationManager;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		List<String> origins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toList();
		configuration.setAllowedOrigins(origins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
		configuration.setAllowCredentials(true);
		configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			// API stateless : le jeton est envoyé dans Authorization, pas dans un cookie.
			// Le CSRF des formulaires ne s'applique pas à ce modèle.
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.headers(headers -> headers
					.contentSecurityPolicy(csp -> csp.policyDirectives(
							"default-src 'self'; frame-ancestors 'none'; object-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"))
					.frameOptions(frame -> frame.deny())
					.contentTypeOptions(Customizer.withDefaults())
					.referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
					.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
					.addHeaderWriter((request, response) -> response.setHeader(
							"Permissions-Policy", "geolocation=(), camera=(), microphone=()")))
			.exceptionHandling(ex -> ex
					.authenticationEntryPoint((request, response, authException) -> {
						response.setStatus(401);
						response.setContentType("application/json");
						response.getWriter().write(
								"{\"status\":\"UNAUTHORIZED\",\"statusCode\":401,\"message\":\"Authentification requise\"}");
					})
					.accessDeniedHandler((request, response, accessDeniedException) -> {
						response.setStatus(403);
						response.setContentType("application/json");
						response.getWriter().write(
								"{\"status\":\"FORBIDDEN\",\"statusCode\":403,\"message\":\"Accès refusé\"}");
					}))
			.authorizeHttpRequests(auth -> {
				if (swaggerEnabled) {
					auth.requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();
				} else {
					auth.requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").denyAll();
				}
				auth.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/client/login").permitAll()
					.requestMatchers(HttpMethod.GET, "/api").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/emergency-numbers", "/api/emergency-numbers/**").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/zones", "/api/zones/**").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/assurances", "/api/assurances/**").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/pharmacies/search").hasAnyRole("ADMIN", "OPERATOR")
					.requestMatchers(HttpMethod.GET, "/api/pharmacies", "/api/pharmacies/**").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/duty-periods/current", "/api/duty-periods/current/all").permitAll()
					.requestMatchers(HttpMethod.GET,
							"/api/client/pharmacies/nearest",
							"/api/client/pharmacies/nearest-duty",
							"/api/client/pharmacies/search",
							"/api/client/pharmacies/*/reviews",
							"/api/client/pharmacies/*/rating-summary").permitAll()
					.requestMatchers(HttpMethod.POST, "/api/client/pharmacies/*/reviews").hasAnyRole("CLIENT", "ADMIN")
					.requestMatchers(HttpMethod.PUT, "/api/client/pharmacies/reviews/**").hasAnyRole("CLIENT", "ADMIN")
					.requestMatchers(HttpMethod.DELETE, "/api/client/pharmacies/admin/reviews/**").hasAnyRole("ADMIN", "OPERATOR")
					.requestMatchers(HttpMethod.DELETE, "/api/client/pharmacies/reviews/**").hasAnyRole("CLIENT", "ADMIN")
					.requestMatchers(HttpMethod.POST, "/api/client/pharmacies/duty/report").hasAnyRole("CLIENT", "ADMIN", "OPERATOR")
					.requestMatchers(HttpMethod.POST, "/api/users").access(firstUserAuthorizationManager)
					.requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")
					.requestMatchers(HttpMethod.PUT, "/api/clients/*/location").hasAnyRole("CLIENT", "ADMIN", "OPERATOR")
					.requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasRole("ADMIN")
					.requestMatchers("/api/clients", "/api/clients/**").hasAnyRole("ADMIN", "OPERATOR")
					.requestMatchers("/api/dashboard/**", "/api/reports/**").hasAnyRole("ADMIN", "OPERATOR")
					.requestMatchers("/api/**").hasAnyRole("ADMIN", "OPERATOR")
					.anyRequest().denyAll();
			})
			.addFilterBefore(new LoginRateLimitFilter(loginMaxAttempts, loginWindowSeconds, trustForwardedFor),
					UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
