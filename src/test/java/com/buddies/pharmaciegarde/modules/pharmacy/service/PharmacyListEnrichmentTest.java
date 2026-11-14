package com.buddies.pharmaciegarde.modules.pharmacy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.buddies.pharmaciegarde.modules.duty.model.Duty;
import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;
import com.buddies.pharmaciegarde.modules.duty.model.DutyStatus;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyPeriodRepository;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyRepository;
import com.buddies.pharmaciegarde.modules.duty.service.ActiveDutyPeriodResolver;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Assurance;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy.PharmacyStatus;
import com.buddies.pharmaciegarde.modules.pharmacy.model.PharmacyReview;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.AssuranceRepository;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyRepository;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyReviewRepository;
import com.buddies.pharmaciegarde.modules.user.model.Client;
import com.buddies.pharmaciegarde.modules.user.repository.ClientRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManagerFactory;

/**
 * La liste des pharmacies garde le même contenu (notes, derniers avis, garde, assurances)
 * sans faire 3 à 5 requêtes SQL par pharmacie.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PharmacyListEnrichmentTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private PharmacyRepository pharmacyRepository;
	@Autowired private PharmacyReviewRepository reviewRepository;
	@Autowired private ClientRepository clientRepository;
	@Autowired private AssuranceRepository assuranceRepository;
	@Autowired private DutyPeriodRepository dutyPeriodRepository;
	@Autowired private DutyRepository dutyRepository;
	@Autowired private ActiveDutyPeriodResolver activeDutyPeriodResolver;
	@Autowired private EntityManagerFactory entityManagerFactory;

	@Test
	void listKeepsRatingsReviewsDutyAndAssurancesWithFewQueries() throws Exception {
		String tag = UUID.randomUUID().toString().substring(0, 8);

		Assurance mugef = new Assurance();
		mugef.setName("MUGEF-" + tag);
		mugef = assuranceRepository.save(mugef);

		Pharmacy onDuty = pharmacy("PERF GARDE " + tag);
		onDuty.getAssurances().add(mugef);
		onDuty = pharmacyRepository.save(onDuty);
		Pharmacy disabledDuty = pharmacyRepository.save(pharmacy("PERF DESACTIVEE " + tag));
		Pharmacy plain = pharmacyRepository.save(pharmacy("PERF SIMPLE " + tag));
		for (int i = 0; i < 40; i++) {
			pharmacyRepository.save(pharmacy("PERF FILLER " + tag + " " + i));
		}

		// Période en cours (commencée hier) : garde ACTIVE pour l'une, DISABLED pour l'autre.
		DutyPeriod period = new DutyPeriod();
		period.setStartDate(LocalDate.now().minusDays(1));
		period.setEndDate(LocalDate.now().plusDays(6));
		period.setIsCurrent(true);
		dutyPeriodRepository.save(period);
		DutyPeriod active = activeDutyPeriodResolver.findActive().orElseThrow();
		dutyRepository.save(new Duty(onDuty, active));
		Duty disabled = new Duty(disabledDuty, active);
		disabled.setStatus(DutyStatus.DISABLED);
		dutyRepository.save(disabled);

		// 4 avis : 3, 4, 5, 5 → moyenne 4,25 → 4.3 ; les 3 plus récents remontent.
		int[] ratings = { 3, 4, 5, 5 };
		for (int i = 0; i < ratings.length; i++) {
			Client client = new Client();
			client.setPhone("+22507" + tag.hashCode() % 1000 + i + "0000".substring(0, 3));
			client.setFirstName("Client" + i);
			client.setLastName(i == 0 ? null : "Test");
			client.setProviderType(Client.ProviderType.PHONE);
			client.setCreatedAt(LocalDateTime.now());
			client = clientRepository.save(client);
			PharmacyReview review = new PharmacyReview();
			review.setPharmacy(onDuty);
			review.setClient(client);
			review.setRating(ratings[i]);
			review.setComment("avis " + i);
			review.setCreatedAt(LocalDateTime.now().minusHours(10 - i));
			reviewRepository.save(review);
		}

		Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		stats.clear();
		String body = mockMvc.perform(get("/api/pharmacies"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		long statements = stats.getPrepareStatementCount();

		List<Map<String, Object>> list = objectMapper.readValue(body, new TypeReference<>() {
		});
		Map<String, Object> garde = byName(list, onDuty.getName());
		Map<String, Object> off = byName(list, disabledDuty.getName());
		Map<String, Object> simple = byName(list, plain.getName());

		assertThat(garde.get("isOnDuty")).isEqualTo(true);
		assertThat(((Number) garde.get("averageRating")).doubleValue()).isEqualTo(4.3);
		assertThat(((Number) garde.get("totalReviews")).longValue()).isEqualTo(4L);
		List<Map<String, Object>> recent = castList(garde.get("recentReviews"));
		assertThat(recent).extracting(r -> r.get("comment")).containsExactly("avis 3", "avis 2", "avis 1");
		assertThat(recent.get(0).get("clientName")).isEqualTo("Client3 Test");
		assertThat(recent.get(0).get("pharmacyName")).isEqualTo(onDuty.getName());
		assertThat(castList(garde.get("assurances"))).extracting(a -> a.get("name")).containsExactly(mugef.getName());

		assertThat(off.get("isOnDuty")).isEqualTo(false);
		for (Map<String, Object> p : List.of(off, simple)) {
			assertThat(p.get("averageRating")).isNull();
			assertThat(((Number) p.get("totalReviews")).longValue()).isZero();
			assertThat(castList(p.get("recentReviews"))).isEmpty();
			assertThat(p.get("assurances")).isNull();
		}

		// Avant : ~3 requêtes par pharmacie (plus de 130 ici). Maintenant : quelques requêtes en tout.
		assertThat(list.size()).isGreaterThanOrEqualTo(43);
		assertThat(statements).isLessThan(15);

		// La fiche détaillée donne le même enrichissement.
		String one = mockMvc.perform(get("/api/pharmacies/" + onDuty.getId()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		Map<String, Object> detail = objectMapper.readValue(one, new TypeReference<>() {
		});
		assertThat(detail).isEqualTo(garde);
	}

	private static Pharmacy pharmacy(String name) {
		return new Pharmacy(name, "+2252722000000", "ABIDJAN", "COCODY", PharmacyStatus.ACTIF, "5.36,-3.96");
	}

	private static Map<String, Object> byName(List<Map<String, Object>> list, String name) {
		return list.stream().filter(p -> name.equals(p.get("name"))).findFirst().orElseThrow();
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> castList(Object value) {
		return value == null ? new ArrayList<>() : (List<Map<String, Object>>) value;
	}
}
