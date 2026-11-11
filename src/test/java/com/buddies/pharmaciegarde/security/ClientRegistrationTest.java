package com.buddies.pharmaciegarde.security;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.buddies.pharmaciegarde.modules.user.model.Client;
import com.buddies.pharmaciegarde.modules.user.model.Client.ProviderType;
import com.buddies.pharmaciegarde.modules.user.model.User;
import com.buddies.pharmaciegarde.modules.user.model.UserRole;
import com.buddies.pharmaciegarde.modules.user.repository.ClientRepository;
import com.buddies.pharmaciegarde.modules.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientRegistrationTest {

	private static final String JSON_UTF8 = "application/json;charset=UTF-8";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ClientRepository clientRepository;

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
	void registerReturnsClientAndBearerToken() throws Exception {
		String body = mockMvc.perform(post("/api/auth/client/register")
				.with(remote("10.70.0.1"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"+225 07 48 00 11 33\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInSeconds").value(86400))
				.andExpect(jsonPath("$.client.id").isNumber())
				.andExpect(jsonPath("$.client.phone").value("+225 07 48 00 11 33"))
				.andExpect(jsonPath("$.client.isActive").value(true))
				.andExpect(jsonPath("$.client.providerType").value("PHONE"))
				.andExpect(jsonPath("$.client.password").doesNotExist())
				.andReturn().getResponse().getContentAsString();

		String token = body.split("\"token\":\"")[1].split("\"")[0];
		String id = body.split("\"id\":")[1].split("[,}]")[0];
		assertTrue(token.chars().filter(ch -> ch == '.').count() == 2, token);

		mockMvc.perform(post("/api/auth/client/login")
				.with(remote("10.70.0.1"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"0748001133\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.client.id").value(Integer.parseInt(id)))
				.andExpect(jsonPath("$.tokenType").value("Bearer"));

		mockMvc.perform(put("/api/clients/" + id + "/location")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"latitude\":5.35,\"longitude\":-4.01}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.latitude").value(5.35))
				.andExpect(jsonPath("$.longitude").value(-4.01));
	}

	@Test
	void duplicatePhoneReturns409() throws Exception {
		mockMvc.perform(post("/api/auth/client/register")
				.with(remote("10.70.0.2"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"+225 05 48 00 22 44\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/client/register")
				.with(remote("10.70.0.2"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"0548002244\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value("CONFLICT"))
				.andExpect(jsonPath("$.statusCode").value(409))
				.andExpect(jsonPath("$.error").value("Conflit"))
				.andExpect(jsonPath("$.message").value("Un client avec ce numéro de téléphone existe déjà"))
				.andExpect(jsonPath("$.path").value("/api/auth/client/register"));
	}

	@Test
	void invalidPhoneReturns400() throws Exception {
		mockMvc.perform(post("/api/auth/client/register")
				.with(remote("10.70.0.3"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"+33 6 12 34 56 78\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.statusCode").value(400))
				.andExpect(jsonPath("$.errors.phone").value("Le numéro de téléphone doit être un numéro ivoirien valide"));
	}

	@Test
	void sensitiveFieldsAreIgnored() throws Exception {
		String phone = "+225 01 48 00 33 55";
		mockMvc.perform(post("/api/auth/client/register")
				.with(remote("10.70.0.4"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"phone":"%s","isActive":false,"role":"ADMIN","password":"secret123","email":"pirate@example.com","providerType":"GOOGLE","providerId":"abc","firstName":"Pirate","lastName":"Admin","latitude":0,"longitude":0}
						""".formatted(phone)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.client.isActive").value(true))
				.andExpect(jsonPath("$.client.providerType").value("PHONE"))
				.andExpect(jsonPath("$.client.email").value(nullValue()))
				.andExpect(jsonPath("$.client.firstName").value(nullValue()))
				.andExpect(jsonPath("$.client.latitude").value(nullValue()))
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.role").doesNotExist());

		Client stored = clientRepository.findByPhone(phone).orElseThrow();
		assertEquals(ProviderType.PHONE, stored.getProviderType());
		assertEquals(Boolean.TRUE, stored.getIsActive());
		assertNull(stored.getPassword());
		assertNull(stored.getEmail());
		assertNull(stored.getFirstName());
		assertNull(stored.getProviderId());
		assertNull(stored.getLatitude());
	}

	@Test
	void registerRateLimitIsUtf8Json() throws Exception {
		RequestPostProcessor remote = remote("10.70.0.9");
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(post("/api/auth/client/register")
					.with(remote)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"phone\":\"123\"}"));
		}
		MvcResult result = mockMvc.perform(post("/api/auth/client/register")
				.with(remote)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"123\"}"))
				.andExpect(status().is(429))
				.andExpect(header().string("Content-Type", JSON_UTF8))
				.andExpect(jsonPath("$.status").value("TOO_MANY_REQUESTS"))
				.andExpect(jsonPath("$.message").value(
						"Trop de tentatives de connexion. Réessayez dans une minute."))
				.andReturn();

		assertUtf8(result, "Réessayez");
	}

	@Test
	void securityErrorsOutsideMvcAreUtf8() throws Exception {
		MvcResult unauthorized = mockMvc.perform(post("/api/clients")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"0748001166\",\"latitude\":5.3,\"longitude\":-4.0}"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("Content-Type", JSON_UTF8))
				.andExpect(jsonPath("$.message").value("Authentification requise"))
				.andReturn();
		assertUtf8(unauthorized, "Authentification requise");

		MvcResult invalidToken = mockMvc.perform(post("/api/clients")
				.header("Authorization", "Bearer pas-un-jwt")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"0748001166\",\"latitude\":5.3,\"longitude\":-4.0}"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("Content-Type", JSON_UTF8))
				.andExpect(jsonPath("$.message").value("Jeton d'authentification invalide ou expiré"))
				.andReturn();
		assertUtf8(invalidToken, "expiré");

		String registered = mockMvc.perform(post("/api/auth/client/register")
				.with(remote("10.70.0.6"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"+225 07 48 00 44 66\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String clientToken = registered.split("\"token\":\"")[1].split("\"")[0];

		MvcResult forbidden = mockMvc.perform(post("/api/clients")
				.header("Authorization", "Bearer " + clientToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"0748001177\",\"latitude\":5.3,\"longitude\":-4.0}"))
				.andExpect(status().isForbidden())
				.andExpect(header().string("Content-Type", JSON_UTF8))
				.andExpect(jsonPath("$.status").value("FORBIDDEN"))
				.andExpect(jsonPath("$.message").value("Accès refusé"))
				.andReturn();
		assertUtf8(forbidden, "Accès refusé");
	}

	@Test
	void adminClientCreationStaysProtectedAndStillWorksForAdmin() throws Exception {
		mockMvc.perform(get("/api/clients"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/clients")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"0748001188\",\"latitude\":5.3,\"longitude\":-4.0}"))
				.andExpect(status().isUnauthorized());

		String adminToken = loginAdmin("10.70.0.7");
		mockMvc.perform(post("/api/clients")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"0748001188\",\"latitude\":5.3,\"longitude\":-4.0,\"isActive\":true}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.phone").value("0748001188"))
				.andExpect(jsonPath("$.token").doesNotExist())
				.andExpect(jsonPath("$.id").isNumber());

		mockMvc.perform(get("/api/clients").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
	}

	private String loginAdmin(String remoteAddress) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
				.with(remote(remoteAddress))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"admin@test.ci\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return body.split("\"token\":\"")[1].split("\"")[0];
	}

	private RequestPostProcessor remote(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}

	private static void assertUtf8(MvcResult result, String fragment) {
		byte[] raw = result.getResponse().getContentAsByteArray();
		String decoded = new String(raw, StandardCharsets.UTF_8);
		assertTrue(decoded.contains(fragment), decoded);
		assertTrue(contains(raw, fragment.getBytes(StandardCharsets.UTF_8)), decoded);
		byte[] asLatin1 = fragment.getBytes(StandardCharsets.ISO_8859_1);
		if (!fragment.equals(new String(asLatin1, StandardCharsets.UTF_8))) {
			assertTrue(!contains(raw, asLatin1), decoded);
		}
		assertTrue(result.getResponse().getContentType().toLowerCase().contains("charset=utf-8"),
				result.getResponse().getContentType());
	}

	private static boolean contains(byte[] raw, byte[] needle) {
		for (int i = 0; i <= raw.length - needle.length; i++) {
			boolean match = true;
			for (int j = 0; j < needle.length; j++) {
				if (raw[i + j] != needle[j]) {
					match = false;
					break;
				}
			}
			if (match) {
				return true;
			}
		}
		return false;
	}
}
