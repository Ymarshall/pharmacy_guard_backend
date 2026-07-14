package com.buddies.pharmaciegarde.modules.report.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyPeriodRepository;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyRepository;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy.PharmacyStatus;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyRepository;
import com.buddies.pharmaciegarde.modules.report.dto.ChartDataDto;
import com.buddies.pharmaciegarde.modules.report.dto.ChartDataDto.ChartSeriesDto;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

	private final PharmacyRepository pharmacyRepository;
	private final DutyPeriodRepository dutyPeriodRepository;
	private final DutyRepository dutyRepository;

	private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

	public ReportServiceImpl(
			PharmacyRepository pharmacyRepository,
			DutyPeriodRepository dutyPeriodRepository,
			DutyRepository dutyRepository) {
		this.pharmacyRepository = pharmacyRepository;
		this.dutyPeriodRepository = dutyPeriodRepository;
		this.dutyRepository = dutyRepository;
	}

	@Override
	public ChartDataDto getPharmacyStatusDistribution() {
		ChartDataDto chart = new ChartDataDto();
		chart.setTitle("Répartition des pharmacies par statut");
		chart.setChartType("pie");

		List<String> labels = new ArrayList<>();
		List<Long> values = new ArrayList<>();

		// Compter les pharmacies actives
		long activeCount = pharmacyRepository.findByStatut(PharmacyStatus.ACTIF).size();
		labels.add("Actif");
		values.add(activeCount);

		// Compter les pharmacies inactives
		long inactiveCount = pharmacyRepository.findByStatut(PharmacyStatus.INACTIF).size();
		labels.add("Inactif");
		values.add(inactiveCount);

		chart.setLabels(labels);
		chart.setValues(values);

		// Ajouter des métadonnées (pourcentages, couleurs)
		Map<String, Object> metadata = new HashMap<>();
		long total = activeCount + inactiveCount;
		if (total > 0) {
			metadata.put("activePercentage", Math.round((double) activeCount / total * 100));
			metadata.put("inactivePercentage", Math.round((double) inactiveCount / total * 100));
		}
		metadata.put("total", total);
		metadata.put("colors", List.of("#10B981", "#EF4444")); // Vert pour actif, rouge pour inactif
		chart.setMetadata(metadata);

		return chart;
	}

	@Override
	public ChartDataDto getPharmacyCreationTrend(LocalDate startDate, LocalDate endDate) {
		ChartDataDto chart = new ChartDataDto();
		chart.setTitle("Évolution du nombre de gardes par période");
		chart.setChartType("line");

		// Utiliser les périodes de garde comme proxy pour l'évolution temporelle
		// Par défaut: 12 derniers mois
		final LocalDate finalStartDate = (startDate != null) ? startDate : LocalDate.now().minusMonths(12);
		final LocalDate finalEndDate = (endDate != null) ? endDate : LocalDate.now();

		// Récupérer toutes les périodes dans la plage de dates
		List<DutyPeriod> periods = dutyPeriodRepository.findAll().stream()
				.filter(p -> !p.getStartDate().isBefore(finalStartDate) && !p.getStartDate().isAfter(finalEndDate))
				.sorted((p1, p2) -> p1.getStartDate().compareTo(p2.getStartDate()))
				.collect(Collectors.toList());

		// Grouper par mois (basé sur startDate)
		Map<String, Long> periodsByMonth = new HashMap<>();
		for (DutyPeriod period : periods) {
			String monthKey = period.getStartDate().format(MONTH_FORMATTER);
			periodsByMonth.put(monthKey, periodsByMonth.getOrDefault(monthKey, 0L) + 1);
		}

		// Créer une liste ordonnée des mois
		List<String> labels = new ArrayList<>();
		List<Long> values = new ArrayList<>();
		LocalDate currentMonth = finalStartDate.withDayOfMonth(1);
		while (!currentMonth.isAfter(finalEndDate)) {
			String monthKey = currentMonth.format(MONTH_FORMATTER);
			labels.add(monthKey);
			values.add(periodsByMonth.getOrDefault(monthKey, 0L));
			currentMonth = currentMonth.plusMonths(1);
		}

		chart.setLabels(labels);
		chart.setValues(values);

		// Ajouter des métadonnées
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("totalPeriods", periods.size());
		metadata.put("color", "#3B82F6"); // Bleu pour la ligne
		chart.setMetadata(metadata);

		return chart;
	}

	@Override
	public ChartDataDto getTopZonesByPharmacyCount() {
		ChartDataDto chart = new ChartDataDto();
		chart.setTitle("Top 10 des zones avec le plus de pharmacies");
		chart.setChartType("bar");

		// Grouper les pharmacies par zone (city)
		Map<String, Long> pharmaciesByZone = pharmacyRepository.findAll().stream()
				.collect(Collectors.groupingBy(
						Pharmacy::getCity,
						Collectors.counting()));

		// Trier par nombre décroissant et prendre le top 10
		List<Map.Entry<String, Long>> topZones = pharmaciesByZone.entrySet().stream()
				.sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
				.limit(10)
				.collect(Collectors.toList());

		List<String> labels = topZones.stream()
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());

		List<Long> values = topZones.stream()
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());

		chart.setLabels(labels);
		chart.setValues(values);

		// Ajouter des métadonnées
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("totalZones", pharmaciesByZone.size());
		metadata.put("color", "#8B5CF6"); // Violet pour les barres
		chart.setMetadata(metadata);

		return chart;
	}

	@Override
	public ChartDataDto getCumulativePharmacyAndDutyTrend(LocalDate startDate, LocalDate endDate) {
		ChartDataDto chart = new ChartDataDto();
		chart.setTitle("Évolution cumulée des périodes de garde et gardes assignées");
		chart.setChartType("area");

		// Par défaut: 12 derniers mois
		final LocalDate finalStartDate = (startDate != null) ? startDate : LocalDate.now().minusMonths(12);
		final LocalDate finalEndDate = (endDate != null) ? endDate : LocalDate.now();

		// Récupérer toutes les périodes dans la plage de dates
		List<DutyPeriod> periods = dutyPeriodRepository.findAll().stream()
				.filter(p -> !p.getStartDate().isBefore(finalStartDate) && !p.getStartDate().isAfter(finalEndDate))
				.sorted((p1, p2) -> p1.getStartDate().compareTo(p2.getStartDate()))
				.collect(Collectors.toList());

		// Grouper par mois
		Map<String, List<DutyPeriod>> periodsByMonth = new HashMap<>();
		for (DutyPeriod period : periods) {
			String monthKey = period.getStartDate().format(MONTH_FORMATTER);
			periodsByMonth.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(period);
		}

		// Créer les séries de données
		List<String> labels = new ArrayList<>();
		List<Long> cumulativePeriods = new ArrayList<>();
		List<Long> cumulativeDuties = new ArrayList<>();

		long totalPeriods = 0;
		long totalDuties = 0;

		LocalDate currentMonth = finalStartDate.withDayOfMonth(1);
		while (!currentMonth.isAfter(finalEndDate)) {
			String monthKey = currentMonth.format(MONTH_FORMATTER);
			labels.add(monthKey);

			// Ajouter les périodes de ce mois
			List<DutyPeriod> monthPeriods = periodsByMonth.getOrDefault(monthKey, new ArrayList<>());
			totalPeriods += monthPeriods.size();

			// Compter les gardes pour ces périodes
			for (DutyPeriod period : monthPeriods) {
				long dutiesCount = dutyRepository.findByDutyPeriodId(period.getId()).size();
				totalDuties += dutiesCount;
			}

			cumulativePeriods.add(totalPeriods);
			cumulativeDuties.add(totalDuties);

			currentMonth = currentMonth.plusMonths(1);
		}

		chart.setLabels(labels);

		// Créer les séries multiples
		List<ChartSeriesDto> chartSeries = new ArrayList<>();
		ChartSeriesDto periodsSeries = new ChartSeriesDto("Périodes de garde", cumulativePeriods);
		periodsSeries.setColor("#3B82F6"); // Bleu
		chartSeries.add(periodsSeries);

		ChartSeriesDto dutiesSeries = new ChartSeriesDto("Gardes assignées", cumulativeDuties);
		dutiesSeries.setColor("#10B981"); // Vert
		chartSeries.add(dutiesSeries);

		chart.setSeries(chartSeries);

		// Ajouter des métadonnées
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("totalPeriods", totalPeriods);
		metadata.put("totalDuties", totalDuties);
		chart.setMetadata(metadata);

		return chart;
	}
}

