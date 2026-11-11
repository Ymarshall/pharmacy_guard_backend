package com.buddies.pharmaciegarde.modules.duty.matching;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Rapprochement flou d'une ligne d'affiche avec le référentiel.
 * Le score mélange Jaro-Winkler (nom), la zone et le téléphone.
 * Deux noms très proches dans des villes différentes restent ambigus : la validation humaine est requise.
 */
@Component
public class PharmacyMatcher {

	public static final double MATCH_THRESHOLD = 0.90;
	public static final double AMBIGUOUS_THRESHOLD = 0.72;
	public static final double ALTERNATIVE_THRESHOLD = 0.55;
	private static final double AMBIGUITY_GAP = 0.03;

	public PharmacyMatch match(String extractedName, String extractedZone, String extractedPhones,
			List<PharmacyCandidate> catalog) {
		String normalized = PharmacyNameNormalizer.fold(extractedName);
		String zone = PharmacyNameNormalizer.zoneKey(extractedZone);
		boolean zonePresent = !zone.isEmpty();
		List<PharmacyMatch.ScoredCandidate> scored = new ArrayList<>();
		if (catalog != null) {
			for (PharmacyCandidate candidate : catalog) {
				scored.add(score(normalized, zone, zonePresent, extractedPhones, candidate));
			}
		}
		scored.sort(Comparator.comparingDouble(PharmacyMatch.ScoredCandidate::score).reversed());
		List<PharmacyMatch.ScoredCandidate> alternatives = scored.stream()
				.filter(item -> item.score() >= ALTERNATIVE_THRESHOLD)
				.limit(3)
				.toList();
		if (scored.isEmpty() || scored.get(0).score() < AMBIGUOUS_THRESHOLD) {
			return new PharmacyMatch(MatchStatus.NOT_FOUND, scored.isEmpty() ? 0 : round(scored.get(0).score()),
					normalized, null, alternatives);
		}
		PharmacyMatch.ScoredCandidate best = scored.get(0);
		double second = scored.size() > 1 ? scored.get(1).score() : 0;
		boolean tight = second >= AMBIGUOUS_THRESHOLD && (best.score() - second) < AMBIGUITY_GAP;
		boolean zoneConflict = zonePresent && best.zoneScore() == 0 && !best.phoneMatch();
		boolean confident = best.score() + 1e-6 >= MATCH_THRESHOLD && !tight && !zoneConflict;
		if (!zonePresent && !best.phoneMatch() && best.nameScore() < 0.98) {
			confident = false;
		}
		MatchStatus status = confident ? MatchStatus.MATCHED : MatchStatus.AMBIGUOUS;
		return new PharmacyMatch(status, round(best.score()), normalized, best.pharmacy(), alternatives);
	}

	private PharmacyMatch.ScoredCandidate score(String normalizedName, String zone, boolean zonePresent,
			String phones, PharmacyCandidate candidate) {
		String candidateName = PharmacyNameNormalizer.fold(candidate.name());
		double nameScore = jaroWinkler(normalizedName, candidateName);
		double zoneScore = zoneScore(zone, zonePresent, candidate);
		boolean phoneMatch = PharmacyNameNormalizer.phonesMatch(phones, candidate.phone1())
				|| PharmacyNameNormalizer.phonesMatch(phones, candidate.phone2());
		double phoneScore = phoneMatch ? 1.0 : 0.0;
		double score = 0.70 * nameScore + 0.20 * zoneScore + 0.10 * phoneScore;
		if (phoneMatch && nameScore >= 0.60) {
			score = Math.max(score, 0.93);
		}
		if (zonePresent && zoneScore == 0 && !phoneMatch) {
			score = Math.min(score, 0.84);
			if (nameScore >= 0.92) {
				score = Math.max(score, 0.80);
			}
		}
		if (!zonePresent && !phoneMatch && nameScore < 0.98) {
			score = Math.min(score, 0.86);
		}
		return new PharmacyMatch.ScoredCandidate(candidate, score, nameScore, zoneScore, phoneMatch);
	}

	private double zoneScore(String zone, boolean zonePresent, PharmacyCandidate candidate) {
		if (!zonePresent) {
			return 0.4;
		}
		String city = PharmacyNameNormalizer.zoneKey(candidate.city());
		String commune = PharmacyNameNormalizer.zoneKey(candidate.commune());
		String district = PharmacyNameNormalizer.zoneKey(candidate.district());
		if (zone.equals(city) || zone.equals(commune) || zone.equals(district)) {
			return 1.0;
		}
		if ((!city.isEmpty() && (city.contains(zone) || zone.contains(city)))
				|| (!commune.isEmpty() && (commune.contains(zone) || zone.contains(commune)))
				|| (!district.isEmpty() && (district.contains(zone) || zone.contains(district)))) {
			return 0.6;
		}
		return 0.0;
	}

	static double jaroWinkler(String left, String right) {
		if (left == null || right == null) {
			return 0;
		}
		if (left.equals(right)) {
			return left.isEmpty() ? 0 : 1;
		}
		int len1 = left.length();
		int len2 = right.length();
		if (len1 == 0 || len2 == 0) {
			return 0;
		}
		int matchDistance = Math.max(0, Math.max(len1, len2) / 2 - 1);
		boolean[] leftMatches = new boolean[len1];
		boolean[] rightMatches = new boolean[len2];
		int matches = 0;
		for (int i = 0; i < len1; i++) {
			int start = Math.max(0, i - matchDistance);
			int end = Math.min(i + matchDistance + 1, len2);
			for (int j = start; j < end; j++) {
				if (rightMatches[j] || left.charAt(i) != right.charAt(j)) {
					continue;
				}
				leftMatches[i] = true;
				rightMatches[j] = true;
				matches++;
				break;
			}
		}
		if (matches == 0) {
			return 0;
		}
		double transpositions = 0;
		int k = 0;
		for (int i = 0; i < len1; i++) {
			if (!leftMatches[i]) {
				continue;
			}
			while (!rightMatches[k]) {
				k++;
			}
			if (left.charAt(i) != right.charAt(k)) {
				transpositions++;
			}
			k++;
		}
		transpositions /= 2.0;
		double jaro = (matches / (double) len1 + matches / (double) len2 + (matches - transpositions) / matches) / 3.0;
		int prefix = 0;
		int maxPrefix = Math.min(4, Math.min(len1, len2));
		for (int i = 0; i < maxPrefix; i++) {
			if (left.charAt(i) == right.charAt(i)) {
				prefix++;
			} else {
				break;
			}
		}
		return jaro + prefix * 0.1 * (1 - jaro);
	}

	private double round(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}
}
