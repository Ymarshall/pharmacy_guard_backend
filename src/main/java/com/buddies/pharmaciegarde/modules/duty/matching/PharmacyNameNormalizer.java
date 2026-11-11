package com.buddies.pharmaciegarde.modules.duty.matching;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalise les noms d'affiches de garde (Côte d'Ivoire) pour le rapprochement.
 * Les préfixes PHARMACIE / PHCIE / NVLLE et les particules DE/DU/LA sont retirés du noyau comparé.
 */
public final class PharmacyNameNormalizer {

	private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
	private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]+");
	private static final Pattern STOP_WORDS = Pattern.compile(
			"\\b(PHARMACIE|PHCIE|PHIE|NOUVELLE|NVLLE|DE|DU|DES|LA|LE|LES|L|D|ET)\\b");

	private PharmacyNameNormalizer() {
	}

	public static String fold(String value) {
		if (value == null) {
			return "";
		}
		String upper = value.trim().toUpperCase(Locale.ROOT);
		upper = upper.replace('’', ' ').replace('\'', ' ').replace('`', ' ').replace('´', ' ');
		String decomposed = Normalizer.normalize(upper, Normalizer.Form.NFD);
		String withoutMarks = COMBINING_MARKS.matcher(decomposed).replaceAll("");
		withoutMarks = withoutMarks.replace("NVLLE", "NOUVELLE");
		String spaced = NON_ALNUM.matcher(withoutMarks).replaceAll(" ").trim();
		spaced = spaced.replaceAll("\\bSTE\\b", "SAINTE").replaceAll("\\bST\\b", "SAINT");
		String stripped = STOP_WORDS.matcher(spaced).replaceAll(" ");
		return stripped.replaceAll("\\s+", " ").trim();
	}

	public static String displayName(String value) {
		String core = fold(value);
		if (core.isEmpty()) {
			return "PHCIE";
		}
		return "PHCIE " + core;
	}

	/**
	 * Garde les chiffres significatifs. Les numéros ivoiriens sont comparés sur les 8 derniers chiffres.
	 */
	public static String phoneKey(String phones) {
		if (phones == null) {
			return "";
		}
		String digits = phones.replaceAll("\\D", "");
		if (digits.length() > 10 && digits.startsWith("225")) {
			digits = digits.substring(3);
		}
		if (digits.length() > 10) {
			return digits.substring(digits.length() - 10);
		}
		return digits;
	}

	public static boolean phonesMatch(String left, String right) {
		String a = phoneKey(left);
		String b = phoneKey(right);
		return a.length() >= 8 && a.equals(b);
	}

	public static String zoneKey(String zone) {
		return fold(zone);
	}
}
