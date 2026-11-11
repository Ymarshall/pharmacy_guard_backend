package com.buddies.pharmaciegarde.common.api;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Comparaison de recherche : minuscules, sans accents.
 */
public final class TextSearch {

	private static final Pattern MARKS = Pattern.compile("\\p{M}+");

	private TextSearch() {
	}

	public static String fold(String value) {
		if (value == null) {
			return "";
		}
		String cleaned = value.trim()
				.replace('’', '\'')
				.replace('‘', '\'')
				.replace('´', '\'')
				.replace('`', '\'');
		String decomposed = Normalizer.normalize(cleaned.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		return MARKS.matcher(decomposed).replaceAll("").replaceAll("\\s+", " ");
	}

	public static boolean contains(String haystack, String needle) {
		if (needle == null || needle.isBlank()) {
			return true;
		}
		return fold(haystack).contains(fold(needle));
	}

	public static boolean containsAny(String needle, String... haystacks) {
		if (needle == null || needle.isBlank()) {
			return true;
		}
		for (String haystack : haystacks) {
			if (contains(haystack, needle)) {
				return true;
			}
		}
		return false;
	}
}
