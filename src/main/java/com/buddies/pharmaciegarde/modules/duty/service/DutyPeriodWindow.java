package com.buddies.pharmaciegarde.modules.duty.service;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;

/**
 * Fenêtre réelle d'une période de garde : [startDate à l'heure de relève,
 * endDate + 1 jour à l'heure de relève), dans le fuseau configuré.
 * Exemple : 7–13 novembre, relève à 08:00 → samedi 7 08:00 inclus jusqu'au
 * samedi 14 08:00 exclu.
 */
@Component
public class DutyPeriodWindow {

	public static final String DEFAULT_ZONE = "Africa/Abidjan";
	public static final LocalTime DEFAULT_HANDOVER = LocalTime.of(8, 0);

	private final ZoneId zoneId;
	private final LocalTime handover;
	private final Clock clock;

	@Autowired
	public DutyPeriodWindow(
			@Value("${app.duty.zone:Africa/Abidjan}") String zone,
			@Value("${app.duty.handover-time:08:00}") String handoverTime,
			ObjectProvider<Clock> clockProvider) {
		this(ZoneId.of(zone), LocalTime.parse(handoverTime),
				clockProvider == null ? null : clockProvider.getIfAvailable());
	}

	public DutyPeriodWindow(ZoneId zoneId, LocalTime handover, Clock clock) {
		this.zoneId = zoneId;
		this.handover = handover;
		this.clock = (clock == null ? Clock.system(zoneId) : clock).withZone(zoneId);
	}

	public static DutyPeriodWindow defaults(Clock clock) {
		return new DutyPeriodWindow(ZoneId.of(DEFAULT_ZONE), DEFAULT_HANDOVER, clock);
	}

	public ZoneId zoneId() {
		return zoneId;
	}

	public LocalTime handover() {
		return handover;
	}

	public Clock clock() {
		return clock;
	}

	public ZonedDateTime now() {
		return ZonedDateTime.now(clock);
	}

	public ZonedDateTime windowStart(DutyPeriod period) {
		if (period == null || period.getStartDate() == null) {
			return null;
		}
		return ZonedDateTime.of(period.getStartDate(), handover, zoneId);
	}

	public ZonedDateTime windowEnd(DutyPeriod period) {
		if (period == null || period.getEndDate() == null) {
			return null;
		}
		return ZonedDateTime.of(period.getEndDate().plusDays(1), handover, zoneId);
	}

	public boolean isWithinWindow(DutyPeriod period) {
		return isWithinWindow(period, now());
	}

	public boolean isWithinWindow(DutyPeriod period, ZonedDateTime instant) {
		ZonedDateTime start = windowStart(period);
		ZonedDateTime end = windowEnd(period);
		if (start == null || end == null || instant == null) {
			return false;
		}
		return !instant.isBefore(start) && instant.isBefore(end);
	}

	public boolean isActive(DutyPeriod period) {
		return isActive(period, now());
	}

	public boolean isActive(DutyPeriod period, ZonedDateTime instant) {
		return period != null && Boolean.TRUE.equals(period.getIsCurrent()) && isWithinWindow(period, instant);
	}

	/**
	 * Jours restants inclusifs : le jour de fin compte pour 1, et tant que la
	 * fenêtre est ouverte (y compris le matin de relève) on renvoie au moins 1.
	 */
	public int daysRemaining(DutyPeriod period) {
		return daysRemaining(period, now());
	}

	public int daysRemaining(DutyPeriod period, ZonedDateTime instant) {
		if (period == null || period.getEndDate() == null || !isWithinWindow(period, instant)) {
			return 0;
		}
		long inclusive = ChronoUnit.DAYS.between(instant.toLocalDate(), period.getEndDate()) + 1;
		return (int) Math.max(inclusive, 1);
	}
}
