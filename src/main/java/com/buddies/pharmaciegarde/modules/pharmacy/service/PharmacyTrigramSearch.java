package com.buddies.pharmaciegarde.modules.pharmacy.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Recherche PostgreSQL sur l'index trigram (pg_trgm + unaccent).
 * L'entrée utilisateur reste un paramètre. Le ORDER BY vient d'une liste blanche.
 * Ne pas passer le SQL à String.format / formatted : les % du LIKE seraient lus comme des spécificateurs
 * (UnknownFormatConversionException « Conversion = ' »), et le repli Java ne serait jamais atteint.
 */
@Component
public class PharmacyTrigramSearch {

	static final Map<String, String> COLUMNS = Map.of(
			"id", "p.id",
			"name", "p.name",
			"city", "p.city",
			"commune", "p.commune",
			"statut", "p.statut");

	private final EntityManager entityManager;

	public PharmacyTrigramSearch(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public boolean supportsTrigram() {
		Object dialect = entityManager.getEntityManagerFactory().getProperties().get("hibernate.dialect");
		String value = dialect == null ? "" : dialect.toString();
		return value.toLowerCase().contains("postgres");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	@SuppressWarnings("unchecked")
	public List<Long> searchIds(String search, String city, String commune, String statut, Sort sort) {
		if (!supportsTrigram() || search == null || search.isBlank()) {
			return null;
		}
		String sql = buildSql(orderBy(sort)).replace("SELECT p.*", "SELECT p.id");
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter("search", search.trim());
		query.setParameter("city", blank(city));
		query.setParameter("commune", blank(commune));
		query.setParameter("statut", blank(statut));
		List<Number> ids = query.getResultList();
		return ids.stream().map(Number::longValue).toList();
	}

	/**
	 * SQL paramétré. {@code orderBy} est déjà une colonne de {@link #COLUMNS}, jamais la saisie utilisateur.
	 */
	static String buildSql(String orderBy) {
		return """
				SELECT p.* FROM pharmacies p
				WHERE (
				  immutable_unaccent(lower(p.name)) LIKE concat('%', immutable_unaccent(lower(cast(:search as varchar))), '%')
				  OR similarity(immutable_unaccent(lower(p.name)), immutable_unaccent(lower(cast(:search as varchar)))) > 0.45
				  OR immutable_unaccent(lower(coalesce(p.city, ''))) LIKE concat('%', immutable_unaccent(lower(cast(:search as varchar))), '%')
				  OR immutable_unaccent(lower(coalesce(p.commune, ''))) LIKE concat('%', immutable_unaccent(lower(cast(:search as varchar))), '%')
				)
				AND (:city = '' OR lower(p.city) = lower(cast(:city as varchar)))
				AND (:commune = '' OR lower(p.commune) = lower(cast(:commune as varchar)))
				AND (:statut = '' OR p.statut = cast(:statut as varchar))
				""" + " ORDER BY " + orderBy;
	}

	private String orderBy(Sort sort) {
		if (sort == null || sort.isUnsorted()) {
			return "p.name ASC";
		}
		Sort.Order order = sort.stream().findFirst().orElse(null);
		if (order == null || !COLUMNS.containsKey(order.getProperty())) {
			return "p.name ASC";
		}
		return COLUMNS.get(order.getProperty()) + (order.isDescending() ? " DESC" : " ASC");
	}

	private String blank(String value) {
		return value == null ? "" : value.trim();
	}
}
