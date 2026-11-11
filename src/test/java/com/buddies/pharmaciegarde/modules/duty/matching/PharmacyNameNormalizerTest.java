package com.buddies.pharmaciegarde.modules.duty.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PharmacyNameNormalizerTest {

	@Test
	void foldsAccentsCaseAndPrefixes() {
		assertEquals("MARCHE", PharmacyNameNormalizer.fold("Pharmacie du Marché"));
		assertEquals("MARCHE", PharmacyNameNormalizer.fold("PHCIE DU MARCHE"));
		assertEquals("MARCHE", PharmacyNameNormalizer.fold("NVLLE PHARMACIE DU MARCHÉ"));
		assertEquals("ETOILE", PharmacyNameNormalizer.fold("PHCIE DE L'ÉTOILE"));
		assertEquals("SAINT MICHEL", PharmacyNameNormalizer.fold("PHARMACIE ST MICHEL"));
	}

	@Test
	void comparesIvorianPhonesOnTheLocalNumber() {
		assertTrue(PharmacyNameNormalizer.phonesMatch("27 35 91 30 09", "+225 27 35 91 30 09"));
		assertEquals("2735913009", PharmacyNameNormalizer.phoneKey("+2252735913009"));
	}
}
