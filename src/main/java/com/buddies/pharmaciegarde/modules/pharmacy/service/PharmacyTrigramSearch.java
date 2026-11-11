package com.buddies.pharmaciegarde.modules.pharmacy.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

/**
 * Recherche PostgreSQL sur l'index trigram (extension pg_trgm + unaccent immutable).
 * Retourne null si la base n'est pas PostgreSQL ou si la fonction n'est pas installée :
 * l'appelant retombe alors sur une comparaison Java insensible aux accents.
 */
@Component
public class PharmacyTrigramSearch {

	private static final Map<String, String> COLUMNS = Map.of(
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

	@SuppressWarnings("unchecked")
	public List<com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy> search(String search, String city,
			String commune, String statut, Sort sort) {
		if (!supportsTrigram() || search == null || search.isBlank()) {
			return null;
		}
		String order = orderBy(sort);
		String sql = """
				SELECT p.* FROM pharmacies p
				WHERE (
				  immutable_unaccent(lower(p.name)) LIKE '%' || immutable_unaccent(lower(CAST(:search AS varchar))) || '%'
				  OR similarity(immutable_unaccent(lower(p.name)), immutable_unaccent(lower(CAST(:search AS varchar)))) > 0.2
				  OR immutable_unaccent(lower(coalesce(p.city, ''))) LIKE '%' || immutable_unaccent(lower(CAST(:search AS varchar))) || '%'
				  OR immutable_unaccent(lower(coalesce(p.commune, ''))) LIKE '%' || immutable_unaccent(lower(CAST(:search AS varchar))) || '%'
				)
				AND (CAST(:city AS varchar) IS NULL OR lower(p.city) = lower(CAST(:city AS varchar)))
				AND (CAST(:commune AS varchar) IS NULL OR lower(p.commune) = lower(CAST(:commune AS varchar)))
				AND (CAST(:statut AS varchar) IS NULL OR p.statut = CAST(:statut AS varchar))
				ORDER BY %s
				""".formatted(order);
		try {
			Query query = entityManager.createNativeQuery(sql,
					com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy.class);
			query.setParameter("search", search.trim());
			query.setParameter("city", blankToNull(city));
			query.setParameter("commune", blankToNull(commune));
			query.setParameter("statut", blankToNull(statut));
			return query.getResultList();
		} catch (PersistenceException ex) {
			return null;
		}
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

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
