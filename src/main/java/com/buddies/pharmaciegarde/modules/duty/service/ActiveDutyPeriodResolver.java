package com.buddies.pharmaciegarde.modules.duty.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyPeriodRepository;

/**
 * Une période n'est « en cours » que si {@code isCurrent} est vrai et que
 * l'instant actuel est dans sa fenêtre de relève.
 */
@Component
public class ActiveDutyPeriodResolver {

	private final DutyPeriodRepository dutyPeriodRepository;
	private final DutyPeriodWindow dutyPeriodWindow;

	public ActiveDutyPeriodResolver(DutyPeriodRepository dutyPeriodRepository, DutyPeriodWindow dutyPeriodWindow) {
		this.dutyPeriodRepository = dutyPeriodRepository;
		this.dutyPeriodWindow = dutyPeriodWindow;
	}

	public Optional<DutyPeriod> findActive() {
		return findActiveAll().stream().findFirst();
	}

	public List<DutyPeriod> findActiveAll() {
		return dutyPeriodRepository.findByIsCurrentTrueOrderByStartDateDesc().stream()
				.filter(dutyPeriodWindow::isActive)
				.collect(Collectors.toList());
	}
}
