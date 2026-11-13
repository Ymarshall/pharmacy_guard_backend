package com.buddies.pharmaciegarde.modules.duty.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyPeriodRepository;

@ExtendWith(MockitoExtension.class)
class DutyPeriodRotationJobTest {

	private static final ZoneId ZONE = ZoneId.of("Africa/Abidjan");

	@Mock
	private DutyPeriodRepository dutyPeriodRepository;
	@Mock
	private DutyPeriodService dutyPeriodService;

	@Test
	void closesExpiredPeriodAndActivatesTheNextWindow() {
		DutyPeriod ending = period(11L, LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 13), true);
		DutyPeriod next = period(12L, LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 20), false);
		DutyPeriodWindow window = windowAt(LocalDateTime.of(2026, 11, 14, 8, 0));
		ActiveDutyPeriodResolver resolver = new ActiveDutyPeriodResolver(dutyPeriodRepository, window);
		DutyPeriodRotationJob job = new DutyPeriodRotationJob(dutyPeriodRepository, window, dutyPeriodService, resolver);

		when(dutyPeriodRepository.findByIsCurrentTrueOrderByStartDateDesc())
				.thenReturn(List.of(ending))
				.thenReturn(List.of());
		when(dutyPeriodRepository.findAll()).thenReturn(List.of(ending, next));
		when(dutyPeriodRepository.save(ending)).thenReturn(ending);

		job.rotateExpiredPeriods(window.now());

		assertFalse(ending.getIsCurrent());
		verify(dutyPeriodRepository).save(ending);
		verify(dutyPeriodService).activatePeriod(12L);
	}

	@Test
	void leavesAStillActivePeriodAloneOnFridayNight() {
		DutyPeriod current = period(11L, LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 13), true);
		DutyPeriod next = period(12L, LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 20), false);
		DutyPeriodWindow window = windowAt(LocalDateTime.of(2026, 11, 13, 23, 0));
		ActiveDutyPeriodResolver resolver = new ActiveDutyPeriodResolver(dutyPeriodRepository, window);
		DutyPeriodRotationJob job = new DutyPeriodRotationJob(dutyPeriodRepository, window, dutyPeriodService, resolver);

		when(dutyPeriodRepository.findByIsCurrentTrueOrderByStartDateDesc()).thenReturn(List.of(current));

		job.rotateExpiredPeriods(window.now());

		assertTrue(current.getIsCurrent());
		verify(dutyPeriodRepository, never()).save(any());
		verify(dutyPeriodService, never()).activatePeriod(any());
	}

	@Test
	void saturdayJustBeforeHandoverDoesNotRotate() {
		DutyPeriod current = period(11L, LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 13), true);
		DutyPeriodWindow window = windowAt(LocalDateTime.of(2026, 11, 14, 7, 59));
		ActiveDutyPeriodResolver resolver = new ActiveDutyPeriodResolver(dutyPeriodRepository, window);
		DutyPeriodRotationJob job = new DutyPeriodRotationJob(dutyPeriodRepository, window, dutyPeriodService, resolver);

		when(dutyPeriodRepository.findByIsCurrentTrueOrderByStartDateDesc()).thenReturn(List.of(current));

		job.rotateExpiredPeriods(window.now());

		assertTrue(current.getIsCurrent());
		verify(dutyPeriodService, never()).activatePeriod(any());
	}

	private static DutyPeriod period(Long id, LocalDate start, LocalDate end, boolean current) {
		DutyPeriod period = new DutyPeriod(start, end);
		period.setId(id);
		period.setIsCurrent(current);
		return period;
	}

	private static DutyPeriodWindow windowAt(LocalDateTime localDateTime) {
		ZonedDateTime instant = ZonedDateTime.of(localDateTime, ZONE);
		return DutyPeriodWindow.defaults(Clock.fixed(instant.toInstant(), ZONE));
	}
}
