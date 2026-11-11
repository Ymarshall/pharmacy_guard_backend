package com.buddies.pharmaciegarde.modules.pharmacy.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UnknownFormatConversionException;

import org.junit.jupiter.api.Test;

class PharmacyTrigramSearchSqlTest {

	@Test
	void orderByIsAppendedWithoutTreatingLikeWildcardsAsFormatSpecifiers() {
		String sql = PharmacyTrigramSearch.buildSql("p.commune DESC");
		assertTrue(sql.contains("ORDER BY p.commune DESC"));
		assertTrue(sql.contains(":search"));
		assertFalse(sql.contains("%s"));
		assertFalse(sql.contains("'''"));
	}

	@Test
	void legacyFormattedTemplateIsTheBugSeenAsConversionQuote() {
		String legacy = "LIKE '%' || immutable_unaccent(lower(cast(:search as varchar))) || '%' ORDER BY %s";
		UnknownFormatConversionException ex = assertThrows(UnknownFormatConversionException.class,
				() -> legacy.formatted("p.name ASC"));
		assertTrue(ex.getMessage().contains("Conversion"));
	}
}
