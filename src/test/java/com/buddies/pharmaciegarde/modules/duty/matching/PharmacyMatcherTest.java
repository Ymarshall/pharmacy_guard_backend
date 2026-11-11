package com.buddies.pharmaciegarde.modules.duty.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PharmacyMatcherTest {

	private final PharmacyMatcher matcher = new PharmacyMatcher();

	private final List<PharmacyCandidate> catalog = List.of(
			new PharmacyCandidate(1L, "PHCIE DU MARCHE", "ABENGOUROU", "ABENGOUROU", null, "2735913009", null),
			new PharmacyCandidate(2L, "PHCIE DU MARCHE", "BOUAKE", "BOUAKE", null, "2720312211", null),
			new PharmacyCandidate(3L, "NOUVELLE PHARMACIE DE L'ETOILE", "ABIDJAN", "COCODY", "ANGRE", "0708091011", null),
			new PharmacyCandidate(4L, "PHCIE SAINT MICHEL", "YAMOUSSOUKRO", "YAMOUSSOUKRO", null, "2720304050", null));

	@Test
	void matchesSameNameInTheGivenZone() {
		PharmacyMatch match = matcher.match("Pharmacie du Marché", "Abengourou", null, catalog);
		assertEquals(MatchStatus.MATCHED, match.status());
		assertEquals(1L, match.suggested().id());
		assertTrue(match.confidence() >= 0.90);
	}

	@Test
	void keepsCloseNamesInDifferentCitiesAmbiguous() {
		PharmacyMatch match = matcher.match("PHCIE DU MARCHE", "DALOA", null, catalog);
		assertEquals(MatchStatus.AMBIGUOUS, match.status());
		assertTrue(match.alternatives().size() >= 2);
		assertTrue(match.confidence() < 0.90);
	}

	@Test
	void linksRenamedNouvellePharmacy() {
		PharmacyMatch match = matcher.match("NVLLE PHCIE DE L'ETOILE", "Cocody", null, catalog);
		assertEquals(MatchStatus.MATCHED, match.status());
		assertEquals(3L, match.suggested().id());
	}

	@Test
	void usesPhoneWhenZoneIsWrong() {
		PharmacyMatch match = matcher.match("Pharmacie du marché", "YAMOUSSOUKRO", "27 20 31 22 11", catalog);
		assertEquals(MatchStatus.MATCHED, match.status());
		assertEquals(2L, match.suggested().id());
	}

	@Test
	void doesNotInventAMatchForAnUnknownName() {
		PharmacyMatch match = matcher.match("PHARMACIE DES LAGUNES", "SAN PEDRO", "0102030405", catalog);
		assertEquals(MatchStatus.NOT_FOUND, match.status());
		assertNull(match.suggested());
	}

	@Test
	void foldsSaintAbbreviation() {
		PharmacyMatch match = matcher.match("PHARMACIE ST MICHEL", "Yamoussoukro", null, catalog);
		assertEquals(MatchStatus.MATCHED, match.status());
		assertEquals(4L, match.suggested().id());
	}
}
