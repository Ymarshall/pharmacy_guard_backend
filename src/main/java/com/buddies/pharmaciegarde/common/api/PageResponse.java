package com.buddies.pharmaciegarde.common.api;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Page de résultats. Présente uniquement si page ou size est fourni.")
public record PageResponse<T>(
		@Schema(description = "Éléments de la page") List<T> content,
		@Schema(description = "Index de page, à partir de 0") int page,
		@Schema(description = "Taille de page") int size,
		@Schema(description = "Nombre total d'éléments") long totalElements,
		@Schema(description = "Nombre total de pages") int totalPages) {

	public static <T> PageResponse<T> from(Page<T> source) {
		return new PageResponse<>(source.getContent(), source.getNumber(), source.getSize(),
				source.getTotalElements(), source.getTotalPages());
	}
}
