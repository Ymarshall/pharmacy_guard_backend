package com.buddies.pharmaciegarde.modules.duty.matching;

import java.util.List;

public record PharmacyMatch(
		MatchStatus status,
		double confidence,
		String normalizedName,
		PharmacyCandidate suggested,
		List<ScoredCandidate> alternatives) {

	public record ScoredCandidate(PharmacyCandidate pharmacy, double score, double nameScore, double zoneScore,
			boolean phoneMatch) {
	}
}
