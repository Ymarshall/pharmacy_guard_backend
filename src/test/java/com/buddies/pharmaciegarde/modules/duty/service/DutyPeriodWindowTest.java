package com.buddies.pharmaciegarde.modules.duty.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;

class DutyPeriodWindowTest {

	private static final ZoneId ZONE = ZoneId.of("Africa/Abidjan");
	private static final LocalDate START = LocalDate.of(2026, 11, 7);
	private static final LocalDate END = LocalDate.of(2026, 11, 13);

	@Test
	void fridayNightStaysActive() {
		DutyPeriodWindow window = windowAt(LocalDateTime.of(2026, 11, 13, 23, 0));
		DutyPeriod period = currentPeriod();
		assertTrue(window.isWithinWindow(period));
		assertTrue(window.isActive(period));
		assertEquals(1, window.daysRemaining(period));
	}

	@Test
	void saturdayJustBeforeHandoverStaysActive() {
		DutyPeriodWindow window = windowAt(LocalDateTime.of(2026, 11, 14, 7, 59));
		DutyPeriod period = currentPeriod();
		assertTrue(window.isWithinWindow(period));
		assertTrue(window.isActive(period));
		assertEquals(1, window.daysRemaining(period));
	}

	@Test
	void saturdayAtHandoverBecomesInactive() {
		DutyPeriodWindow window = windowAt(LocalDateTime.of(2026, 11, 14, 8, 0));
		DutyPeriod period = currentPeriod();
		assertFalse(window.isWithinWindow(period));
		assertFalse(window.isActive(period));
		assertEquals(0, window.daysRemaining(period));
	}

	@Test
	void windowStartsAtHandoverOnTheFirstDay() {
		DutyPeriod period = currentPeriod();
		DutyPeriodWindow before = windowAt(LocalDateTime.of(2026, 11, 7, 7, 59));
		DutyPeriodWindow atStart = windowAt(LocalDateTime.of(2026, 11, 7, 8, 0));
		assertFalse(before.isWithinWindow(period));
		assertFalse(before.isActive(period));
		assertTrue(atStart.isWithinWindow(period));
		assertTrue(atStart.isActive(period));
		assertEquals(7, atStart.daysRemaining(period));
	}

	@Test
	void isCurrentFlagIsRequired() {
		DutyPeriodWindow window = windowAt(LocalDateTime.of(2026, 11, 13, 23, 0));
		DutyPeriod draft = period(false);
		assertTrue(window.isWithinWindow(draft));
		assertFalse(window.isActive(draft));
	}

	@Test
	void daysRemainingIsInclusiveOfTheEndDate() {
		DutyPeriod period = currentPeriod();
		assertEquals(2, windowAt(LocalDateTime.of(2026, 11, 12, 10, 0)).daysRemaining(period));
		assertEquals(1, windowAt(LocalDateTime.of(2026, 11, 13, 0, 0)).daysRemaining(period));
		assertEquals(0, windowAt(LocalDateTime.of(2026, 11, 14, 8, 0)).daysRemaining(period));
	}

	private static DutyPeriod currentPeriod() {
		return period(true);
	}

	private static DutyPeriod period(boolean current) {
		DutyPeriod period = new DutyPeriod(START, END);
		period.setId(1L);
		period.setIsCurrent(current);
		return period;
	}

	private static DutyPeriodWindow windowAt(LocalDateTime localDateTime) {
		ZonedDateTime instant = ZonedDateTime.of(localDateTime, ZONE);
		return DutyPeriodWindow.defaults(Clock.fixed(instant.toInstant(), ZONE));
	}
}
