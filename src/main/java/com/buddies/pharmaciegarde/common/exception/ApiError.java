package com.buddies.pharmaciegarde.common.exception;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
		Instant timestamp,
		int statusCode,
		String status,
		String error,
		String message,
		String path,
		Map<String, String> errors) {

	public static ApiError of(int statusCode, String status, String error, String message, String path) {
		return new ApiError(Instant.now(), statusCode, status, error, message, path, null);
	}

	public static ApiError of(int statusCode, String status, String error, String message, String path,
			Map<String, String> errors) {
		return new ApiError(Instant.now(), statusCode, status, error, message, path, errors);
	}
}
