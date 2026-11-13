package com.buddies.pharmaciegarde.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.buddies.pharmaciegarde.modules.duty.model.DutyFile;
import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyFileRepository;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyPeriodRepository;
import com.buddies.pharmaciegarde.modules.duty.service.UploadStorageService;
import com.buddies.pharmaciegarde.modules.user.model.User;
import com.buddies.pharmaciegarde.modules.user.model.UserRole;
import com.buddies.pharmaciegarde.modules.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FrontFollowUpTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private DutyPeriodRepository dutyPeriodRepository;
	@Autowired
	private DutyFileRepository dutyFileRepository;
	@Autowired
	private UploadStorageService uploadStorageService;
	@Autowired
	private com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyRepository pharmacyRepository;

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
	void previewWithoutVisionReturns503AndUnreadableFileReturns422() throws Exception {
		String token = login();
		DutyPeriod period = period();
		DutyFile image = file(period, "affiche.jpg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 1, 2, 3 });
		mockMvc.perform(post("/api/duty-files/" + image.getId() + "/preview").header("Authorization", "Bearer " + token))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value("SERVICE_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("Google Vision API n'est pas activé"));

		DutyFile missing = file(period, "absent.pdf", null);
		mockMvc.perform(post("/api/duty-files/" + missing.getId() + "/preview").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.message").value("Fichier illisible"));
	}

	@Test
	void previewAsyncReturnsThePreviewWhenFinished() throws Exception {
		String token = login();
		DutyPeriod period = period();
		Path pdf = uploadStorageService.root().resolve("liste-texte.pdf");
		Files.createDirectories(uploadStorageService.root());
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				stream.beginText();
				stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				stream.newLineAtOffset(40, 700);
				stream.showText("PHARMACIE DU MARCHE ABENGOUROU TELEPHONE 27 35 91 30 09 ".repeat(4));
				stream.endText();
			}
			document.save(pdf.toFile());
		}
		DutyFile stored = new DutyFile("liste-texte.pdf");
		stored.setDutyPeriod(period);
		stored.setFilePath(pdf.toString());
		stored.setFileSize(Files.size(pdf));
		stored = dutyFileRepository.save(stored);

		String body = mockMvc.perform(post("/api/duty-files/" + stored.getId() + "/preview-async")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.jobType").value("PREVIEW"))
				.andExpect(jsonPath("$.jobId").exists())
				.andReturn().getResponse().getContentAsString();
		String jobId = body.split("\"jobId\":\"")[1].split("\"")[0];
		String status = "";
		String payload = "";
		for (int attempt = 0; attempt < 40; attempt++) {
			payload = mockMvc.perform(get("/api/duty-files/jobs/" + jobId).header("Authorization", "Bearer " + token))
					.andReturn().getResponse().getContentAsString();
			status = payload.contains("\"status\":\"COMPLETED\"") ? "COMPLETED"
					: payload.contains("\"status\":\"FAILED\"") ? "FAILED" : "PENDING";
			if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
				break;
			}
			Thread.sleep(100);
		}
		org.junit.jupiter.api.Assertions.assertEquals("COMPLETED", status, payload);
		org.junit.jupiter.api.Assertions.assertTrue(payload.contains("\"preview\""), payload);
	}

	@Test
	void pagedSearchIsAccentInsensitiveAndSortIsWhitelisted() throws Exception {
		// Le seed embarqué est désactivé : on crée la pharmacie que la recherche doit trouver.
		com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy portBouet = new com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy();
		portBouet.setName("PHCIE TEST PORT BOUET");
		portBouet.setPhone1("0700111555");
		portBouet.setCity("ABIDJAN");
		portBouet.setCommune("PORT-BOUET");
		portBouet.setStatut(com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy.PharmacyStatus.ACTIF);
		pharmacyRepository.save(portBouet);

		String token = login();
		mockMvc.perform(get("/api/pharmacies").param("page", "0").param("size", "50").param("search", "bouet"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].commune", containsString("BOUET")));

		mockMvc.perform(get("/api/pharmacies").param("page", "0").param("size", "5").param("sort", "name,desc")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").exists());

		mockMvc.perform(get("/api/users").param("page", "0").param("size", "10").param("sort", "password,asc")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("password")));

		mockMvc.perform(get("/api/users").param("page", "0").param("size", "10").param("search", "YANN")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements", greaterThan(0)));

		mockMvc.perform(get("/api/clients").param("page", "0").param("sort", "phone,asc")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());

		mockMvc.perform(get("/api/duties").param("page", "0").param("sort", "createdAt,desc")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/duty-periods").param("page", "0").param("sort", "startDate,asc")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void confirmCreatesPharmacyWithoutFakePhoneOrZeroLocation() throws Exception {
		String token = login();
		DutyPeriod period = period();
		DutyFile stored = new DutyFile("manuel.jpg");
		stored.setDutyPeriod(period);
		stored.setFilePath(uploadStorageService.root().resolve("manuel.jpg").toString());
		stored = dutyFileRepository.save(stored);
		String body = """
				{"lines":[{"pharmacyName":"pharmacie nvlle des lagunes test","zone":"ABIDJAN","city":"Abidjan","commune":"Cocody","phone":"0700111333","pharmacistName":"AWA KONE","description":"Près du marché","createPharmacy":true,"skip":false}]}
				""";
		mockMvc.perform(post("/api/duty-files/" + stored.getId() + "/confirm")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pharmaciesCreated").value(1))
				.andExpect(jsonPath("$.lines[0].status").value("CREATED"));

		mockMvc.perform(get("/api/pharmacies").param("search", "lagunes test").param("page", "0").param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("PHARMACIE NVLLE DES LAGUNES TEST"))
				.andExpect(jsonPath("$.content[0].phone1").value("0700111333"))
				.andExpect(jsonPath("$.content[0].location").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.content[0].city").value("ABIDJAN"));

		String missingPhone = """
				{"lines":[{"pharmacyName":"pharmacie sans tel","city":"ABIDJAN","commune":"COCODY","createPharmacy":true,"skip":false}]}
				""";
		mockMvc.perform(post("/api/duty-files/" + stored.getId() + "/confirm")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(missingPhone))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Erreurs de validation"));
	}

	@Test
	void reviewSearchRequiresOperatorAndScoresNormalizedNames() throws Exception {
		com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy pharmacy = new com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy();
		pharmacy.setName("PHARMACIE NVLLE DES LAGUNES REVUE");
		pharmacy.setPhone1("0700111444");
		pharmacy.setCity("ABIDJAN");
		pharmacy.setCommune("COCODY");
		pharmacy.setStatut(com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy.PharmacyStatus.ACTIF);
		pharmacyRepository.save(pharmacy);
		mockMvc.perform(get("/api/pharmacies/search").param("q", "atlantique"))
				.andExpect(status().isUnauthorized());
		String token = login();
		mockMvc.perform(get("/api/pharmacies/search").param("q", "NVLLE PHCIE DES LAGUNES REVUE").param("limit", "5")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name", containsString("LAGUNES REVUE")))
				.andExpect(jsonPath("$.content[0].score", greaterThan(0.8)))
				.andExpect(jsonPath("$.content[0].phone").value("0700111444"));
	}

	private DutyPeriod period() {
		DutyPeriod period = new DutyPeriod(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 8));
		period.setIsCurrent(false);
		return dutyPeriodRepository.save(period);
	}

	private DutyFile file(DutyPeriod period, String name, byte[] bytes) throws Exception {
		Files.createDirectories(uploadStorageService.root());
		Path path = uploadStorageService.root().resolve(name);
		if (bytes != null) {
			Files.write(path, bytes);
		} else if (Files.exists(path)) {
			Files.delete(path);
		}
		DutyFile file = new DutyFile(name);
		file.setDutyPeriod(period);
		file.setFilePath(path.toString());
		file.setFileSize(bytes == null ? 0L : (long) bytes.length);
		return dutyFileRepository.save(file);
	}

	private int loginHost = 20;

	private String login() throws Exception {
		loginHost++;
		String remote = "10.8.8." + loginHost;
		String body = mockMvc.perform(post("/api/auth/login")
				.with(request -> {
					request.setRemoteAddr(remote);
					return request;
				})
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"admin@test.ci\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return body.split("\"token\":\"")[1].split("\"")[0];
	}
}
