package com.buddies.pharmaciegarde.security;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.buddies.pharmaciegarde.modules.user.model.User;
import com.buddies.pharmaciegarde.modules.user.model.UserRole;
import com.buddies.pharmaciegarde.modules.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void createAdmin() {
		if (userRepository.findByEmail("admin@test.ci").isEmpty()) {
			User user = new User();
			user.setEmail("admin@test.ci");
			user.setPassword(passwordEncoder.encode("secret123"));
			user.setFirstName("Yann");
			user.setLastName("Test");
			user.setRole(UserRole.ADMIN);
			userRepository.save(user);
		}
	}

	@Test
	void publicReadStaysOpenAndWritesRequireJwt() throws Exception {
		mockMvc.perform(get("/api/emergency-numbers"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"));

		mockMvc.perform(get("/api/pharmacies"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());

		mockMvc.perform(post("/api/pharmacies")
				.contentType(MediaType.APPLICATION_JSON)
				.content(pharmacyJson("PHCIE SECURITE")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentification requise"))
				.andExpect(jsonPath("$.message", not(containsString("java."))));

		String token = login("127.0.0.8");
		mockMvc.perform(post("/api/pharmacies")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(pharmacyJson("PHCIE SECURITE")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("PHCIE SECURITE"));
	}

	@Test
	void paginationIsOptIn() throws Exception {
		mockMvc.perform(get("/api/pharmacies").param("page", "0").param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.totalElements").exists());
	}

	@Test
	void searchParameterIsNotConcatenatedSql() throws Exception {
		mockMvc.perform(get("/api/client/pharmacies/search").param("name", "' OR 1=1 --"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	void loginIsRateLimited() throws Exception {
		RequestPostProcessor remote = request -> {
			request.setRemoteAddr("10.9.9.9");
			return request;
		};
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(post("/api/auth/login")
					.with(remote)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"admin@test.ci\",\"password\":\"mauvais\"}"));
		}
		mockMvc.perform(post("/api/auth/login")
				.with(remote)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"admin@test.ci\",\"password\":\"mauvais\"}"))
				.andExpect(status().is(429));
	}

	@Test
	void unknownResourceDoesNotLeakStackTrace() throws Exception {
		mockMvc.perform(get("/api/pharmacies/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message", not(containsString("Exception"))));
	}

	@Test
	void uploadsAreNotServedFromTheWebRoot() throws Exception {
		mockMvc.perform(get("/uploads/duty-files/secret.pdf"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void invalidTokenIsRejected() throws Exception {
		mockMvc.perform(get("/api/users").header("Authorization", "Bearer pas-un-jwt"))
				.andExpect(status().isUnauthorized());
	}

	private String login(String remoteAddress) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
				.with(request -> {
					request.setRemoteAddr(remoteAddress);
					return request;
				})
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"admin@test.ci\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").exists())
				.andExpect(jsonPath("$.role").value("ADMIN"))
				.andReturn().getResponse().getContentAsString();
		int start = body.indexOf("\"token\":\"") + 9;
		int end = body.indexOf('"', start);
		return body.substring(start, end);
	}

	private String pharmacyJson(String name) {
		return """
				{"name":"%s","phone1":"0700000099","city":"ABIDJAN","commune":"COCODY","statut":"ACTIF","location":"5.3,-4.0"}
				""".formatted(name);
	}
}
