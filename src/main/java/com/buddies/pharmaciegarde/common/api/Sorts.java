package com.buddies.pharmaciegarde.common.api;

import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Sort;

public final class Sorts {

	private Sorts() {
	}

	public static Sort parse(String raw, Set<String> allowed, String defaultField) {
		return parseOr(raw, allowed, Sort.by(Sort.Direction.ASC, defaultField));
	}

	public static Sort parseOr(String raw, Set<String> allowed, Sort fallback) {
		if (raw == null || raw.isBlank()) {
			return fallback;
		}
		String[] parts = raw.split(",", -1);
		if (parts.length < 1 || parts.length > 2 || parts[0].isBlank()) {
			throw new IllegalArgumentException("Tri invalide. Format attendu : champ,asc ou champ,desc");
		}
		String field = parts[0].trim();
		if (!allowed.contains(field)) {
			throw new IllegalArgumentException("Tri non autorisé : " + field);
		}
		String direction = parts.length == 2 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
		if (!direction.equals("asc") && !direction.equals("desc")) {
			throw new IllegalArgumentException("Sens de tri invalide : " + parts[1].trim());
		}
		return Sort.by("desc".equals(direction) ? Sort.Direction.DESC : Sort.Direction.ASC, field);
	}

	public static boolean isDescending(Sort sort) {
		return sort.stream().findFirst().map(order -> order.getDirection() == Sort.Direction.DESC).orElse(false);
	}

	public static String field(Sort sort, String fallback) {
		return sort.stream().findFirst().map(Sort.Order::getProperty).orElse(fallback);
	}
}
