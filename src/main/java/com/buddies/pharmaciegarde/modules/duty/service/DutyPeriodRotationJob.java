package com.buddies.pharmaciegarde.modules.duty.service;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyPeriodRepository;

/**
 * Chaque heure : clôture les périodes dont la fenêtre est terminée, puis
 * active la prochaine période dont la fenêtre contient l'instant actuel.
 */
@Component
public class DutyPeriodRotationJob {

	private static final Logger logger = LoggerFactory.getLogger(DutyPeriodRotationJob.class);

	private final DutyPeriodRepository dutyPeriodRepository;
	private final DutyPeriodWindow dutyPeriodWindow;
	private final DutyPeriodService dutyPeriodService;
	private final ActiveDutyPeriodResolver activeDutyPeriodResolver;

	public DutyPeriodRotationJob(DutyPeriodRepository dutyPeriodRepository, DutyPeriodWindow dutyPeriodWindow,
			DutyPeriodService dutyPeriodService, ActiveDutyPeriodResolver activeDutyPeriodResolver) {
		this.dutyPeriodRepository = dutyPeriodRepository;
		this.dutyPeriodWindow = dutyPeriodWindow;
		this.dutyPeriodService = dutyPeriodService;
		this.activeDutyPeriodResolver = activeDutyPeriodResolver;
	}

	@Scheduled(cron = "0 0 * * * *", zone = "${app.duty.zone:Africa/Abidjan}")
	@Transactional
	public void rotateExpiredPeriods() {
		rotateExpiredPeriods(dutyPeriodWindow.now());
	}

	void rotateExpiredPeriods(ZonedDateTime now) {
		List<DutyPeriod> currentPeriods = dutyPeriodRepository.findByIsCurrentTrueOrderByStartDateDesc();
		for (DutyPeriod period : currentPeriods) {
			if (!dutyPeriodWindow.isWithinWindow(period, now)) {
				period.setIsCurrent(false);
				dutyPeriodRepository.save(period);
				logger.info("Période de garde {} clôturée : fenêtre terminée le {}", period.getId(),
						dutyPeriodWindow.windowEnd(period));
			}
		}

		if (activeDutyPeriodResolver.findActive().isPresent()) {
			return;
		}

		dutyPeriodRepository.findAll().stream()
				.filter(period -> !Boolean.TRUE.equals(period.getIsCurrent()))
				.filter(period -> dutyPeriodWindow.isWithinWindow(period, now))
				.min(Comparator.comparing(DutyPeriod::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(DutyPeriod::getId, Comparator.nullsLast(Comparator.naturalOrder())))
				.ifPresent(next -> {
					dutyPeriodService.activatePeriod(next.getId());
					logger.info("Période de garde {} activée automatiquement (fenêtre en cours)", next.getId());
				});
	}
}
