package com.buddies.pharmaciegarde.config.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Pose les index après Hibernate, pour une base neuve où Flyway a tourné avant les tables.
 * Chaque ordre est idempotent et isolé : un échec (droits, extension) n'arrête pas les suivants.
 */
@Component
@Order(100)
public class SchemaHardeningRunner implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(SchemaHardeningRunner.class);

	private static final List<String> STATEMENTS = List.of(
			"CREATE INDEX IF NOT EXISTS idx_pharmacies_name ON pharmacies (name)",
			"CREATE INDEX IF NOT EXISTS idx_pharmacies_city ON pharmacies (city)",
			"CREATE INDEX IF NOT EXISTS idx_pharmacies_commune ON pharmacies (commune)",
			"CREATE INDEX IF NOT EXISTS idx_pharmacies_statut ON pharmacies (statut)",
			"CREATE INDEX IF NOT EXISTS idx_duties_pharmacy ON duties (pharmacy_id)",
			"CREATE INDEX IF NOT EXISTS idx_duties_period ON duties (duty_period_id)",
			"CREATE INDEX IF NOT EXISTS idx_duties_status ON duties (status)",
			"CREATE UNIQUE INDEX IF NOT EXISTS uq_duties_pharmacy_period ON duties (pharmacy_id, duty_period_id)",
			"CREATE INDEX IF NOT EXISTS idx_duty_periods_dates ON duty_periods (start_date, end_date)",
			"CREATE INDEX IF NOT EXISTS idx_duty_periods_current ON duty_periods (is_current)",
			"CREATE INDEX IF NOT EXISTS idx_duty_files_period ON duty_files (duty_period_id)",
			"CREATE INDEX IF NOT EXISTS idx_duty_files_hash ON duty_files (content_hash)",
			"CREATE INDEX IF NOT EXISTS idx_pharmacies_name_trgm ON pharmacies USING gin (name gin_trgm_ops)");

	private final DataSource dataSource;

	public SchemaHardeningRunner(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void run(ApplicationArguments args) {
		try (Connection connection = dataSource.getConnection()) {
			String url = connection.getMetaData().getURL();
			if (url == null || !url.startsWith("jdbc:postgresql:")) {
				return;
			}
			for (String sql : STATEMENTS) {
				try (Statement statement = connection.createStatement()) {
					statement.execute(sql);
				} catch (SQLException ex) {
					logger.debug("Index ignoré ({}): {}", sql, ex.getMessage());
				}
			}
			logger.info("Vérification des index PostgreSQL terminée");
		} catch (SQLException ex) {
			logger.warn("Impossible de vérifier les index: {}", ex.getMessage());
		}
	}
}
