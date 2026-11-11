package com.buddies.pharmaciegarde.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class TextSearchAndSortTest {

	@Test
	void foldsAccentsApostrophesAndCase() {
		assertTrue(TextSearch.contains("Port-Bouët", "bouet"));
		assertTrue(TextSearch.contains("Pharmacie de l'Étoile", "pharmacie de l'etoile"));
		assertTrue(TextSearch.contains("Côte d’Ivoire", "cote d'ivoire"));
		assertTrue(TextSearch.contains("ABOBO", "abobo"));
		assertEquals("marche", TextSearch.fold("Marché"));
	}

	@Test
	void rejectsUnknownSortField() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> Sorts.parse("password,asc", Set.of("email", "id"), "id"));
		assertTrue(ex.getMessage().contains("password"));
	}

	@Test
	void keepsDefaultSortWhenAbsent() {
		Sort sort = Sorts.parseOr(null, Set.of("id"), Sort.by(Sort.Direction.DESC, "id"));
		assertEquals("id", Sorts.field(sort, "name"));
		assertTrue(Sorts.isDescending(sort));
	}
}
