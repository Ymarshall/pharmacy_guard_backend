package com.buddies.pharmaciegarde.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class TextSearchAndSortTest {

	@Test
	void foldsAccentsAndCase() {
		assertTrue(TextSearch.contains("Port-Bouët", "bouet"));
		assertTrue(TextSearch.contains("PHCIE Atlantique", "atlantique"));
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
