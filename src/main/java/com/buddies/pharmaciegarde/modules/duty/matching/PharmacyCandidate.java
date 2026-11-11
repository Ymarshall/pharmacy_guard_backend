package com.buddies.pharmaciegarde.modules.duty.matching;

public record PharmacyCandidate(
		Long id,
		String name,
		String city,
		String commune,
		String district,
		String phone1,
		String phone2) {
}
