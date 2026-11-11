package com.buddies.pharmaciegarde.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.buddies.pharmaciegarde.modules.duty.model.Duty;
import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyPeriodRepository;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyRepository;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyRepository;
import com.buddies.pharmaciegarde.modules.pharmacy.service.PharmacyTrigramSearch;
import com.buddies.pharmaciegarde.modules.user.model.Client;
import com.buddies.pharmaciegarde.modules.user.model.User;
import com.buddies.pharmaciegarde.modules.user.model.UserRole;
import com.buddies.pharmaciegarde.modules.user.repository.ClientRepository;
import com.buddies.pharmaciegarde.modules.user.repository.UserRepository;

/**
 * PostgreSQL réel : le bug {@code search=plateau} → 400 « Conversion = ' » n'apparaît pas sur H2,
 * parce que la requête trigram n'y est pas exécutée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf("com.buddies.pharmaciegarde.security.PostgresSearchIntegrationTest#postgresReachable")
class PostgresSearchIntegrationTest {

	private static final String URL = "jdbc:postgresql://localhost:5432/pharmacy_search_it";
	private static final String USER = "postgres";
	private static final String PASSWORD = "pgtest";

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> URL);
		registry.add("spring.datasource.username", () -> USER);
		registry.add("spring.datasource.password", () -> PASSWORD);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
		registry.add("spring.jpa.properties.hibernate.hbm2ddl.auto", () -> "update");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("app.jwt.secret", () -> "test-jwt-secret-must-be-at-least-32-characters");
		registry.add("google.vision.enabled", () -> "false");
		registry.add("app.security.login-max-attempts", () -> "1000");
		registry.add("app.admin.email", () -> "");
		registry.add("app.admin.password", () -> "");
	}

	static boolean postgresReachable() {
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
			return connection.isValid(2);
		} catch (Exception ex) {
			return false;
		}
	}

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private PharmacyTrigramSearch pharmacyTrigramSearch;
	@Autowired
	private PharmacyRepository pharmacyRepository;
	@Autowired
	private DutyRepository dutyRepository;
	@Autowired
	private DutyPeriodRepository dutyPeriodRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ClientRepository clientRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void fixtures() {
		if (userRepository.findByEmail("pg-search@test.ci").isEmpty()) {
			User user = new User();
			user.setEmail("pg-search@test.ci");
			user.setPassword(passwordEncoder.encode("secret123"));
			user.setFirstName("Yann");
			user.setLastName("Côte d'Ivoire");
			user.setRole(UserRole.ADMIN);
			userRepository.save(user);
		}
		Pharmacy pharmacy = pharmacyRepository.findByName("PHARMACIE DE L'ÉTOILE ZZ").orElseGet(() -> {
			Pharmacy created = new Pharmacy();
			created.setName("PHARMACIE DE L'ÉTOILE ZZ");
			created.setPhone1("0700555666");
			created.setCity("Côte d'Ivoire");
			created.setCommune("Abobo");
			created.setStatut(Pharmacy.PharmacyStatus.ACTIF);
			created.setLocation("5.3,-4.0");
			return pharmacyRepository.save(created);
		});
		DutyPeriod period = dutyPeriodRepository.findAll().stream()
				.filter(item -> "Côte d'Ivoire ZZ".equals(item.getDescription()))
				.findFirst()
				.orElseGet(() -> {
					DutyPeriod created = new DutyPeriod(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 8));
					created.setDescription("Côte d'Ivoire ZZ");
					created.setIsCurrent(false);
					return dutyPeriodRepository.save(created);
				});
		if (dutyRepository.findByPharmacyIdAndDutyPeriodId(pharmacy.getId(), period.getId()).isEmpty()) {
			Duty duty = new Duty(pharmacy, period);
			dutyRepository.save(duty);
		}
		if (clientRepository.findByPhone("0799001122").isEmpty()) {
			Client client = new Client();
			client.setPhone("0799001122");
			client.setFirstName("Abobo");
			client.setLastName("Étoile");
			client.setEmail("abobo-zz@test.ci");
			client.setProviderType(Client.ProviderType.PHONE);
			client.setIsActive(true);
			clientRepository.save(client);
		}
	}

	@Test
	void pharmacySearchPlateauDoesNotFailOnTrigramSql() throws Exception {
		assertNotNull(pharmacyTrigramSearch.searchIds("plateau", null, null, null, Sort.by("name")),
				"la requête trigram doit s'exécuter, pas lever Conversion = '");
		mockMvc.perform(get("/api/pharmacies").param("search", "plateau").param("page", "0").param("size", "5")
				.param("sort", "name,asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	void searchHandlesAccentsApostrophesSortAndPagingOnEveryList() throws Exception {
		String token = login();
		mockMvc.perform(get("/api/pharmacies").param("search", "cote d'ivoire").param("sort", "name,asc")
				.param("page", "0").param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].city", hasItem(containsString("Ivoire"))));
		mockMvc.perform(get("/api/pharmacies").param("search", "Pharmacie de l'Étoile").param("sort", "commune,desc")
				.param("page", "0").param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].name", hasItem(containsString("ÉTOILE"))));
		mockMvc.perform(get("/api/pharmacies").param("search", "abobo").param("page", "0").param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements", greaterThan(0)));

		mockMvc.perform(get("/api/duties").param("search", "l'etoile zz").param("sort", "createdAt,desc")
				.param("page", "0").param("size", "5").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].pharmacyName", containsString("ÉTOILE")));

		mockMvc.perform(get("/api/duty-periods").param("search", "cote d'ivoire zz").param("sort", "startDate,asc")
				.param("page", "0").param("size", "5").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].description", containsString("Ivoire")));

		mockMvc.perform(get("/api/users").param("search", "cote d'ivoire").param("sort", "lastName,asc")
				.param("page", "0").param("size", "5").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].lastName").value("Côte d'Ivoire"));

		mockMvc.perform(get("/api/clients").param("search", "abobo").param("sort", "firstName,asc")
				.param("page", "0").param("size", "5").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].firstName").value("Abobo"))
				.andExpect(jsonPath("$.content[0].phone").value("0799001122"));
	}

	private String login() throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
				.with(request -> {
					request.setRemoteAddr("10.4.4.4");
					return request;
				})
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"pg-search@test.ci\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return body.split("\"token\":\"")[1].split("\"")[0];
	}
}
