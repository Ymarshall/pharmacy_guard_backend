package com.buddies.pharmaciegarde.common.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public final class Slices {

	private Slices() {
	}

	public static <T> Page<T> of(List<T> all, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? PageRequests.DEFAULT_SIZE : Math.min(size, PageRequests.MAX_SIZE);
		int from = Math.min(safePage * safeSize, all.size());
		int to = Math.min(from + safeSize, all.size());
		return new PageImpl<>(all.subList(from, to), PageRequest.of(safePage, safeSize), all.size());
	}
}
