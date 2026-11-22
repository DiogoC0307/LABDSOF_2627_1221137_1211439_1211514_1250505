package pt.isep.escala.elegibilidadeworker.extracao;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * One unavailability period, in the format of 06 · Responsible AI Opportunity Assessment ("Expected outputs").
 *
 * <p>{@code startDate} and {@code endDate} are inclusive. {@code weekdays} is empty unless the period is a repeating
 * pattern inside the range. {@code startTime} and {@code endTime} are both set or both null; a window that ends before
 * it starts crosses midnight. Without times the whole day is unavailable.
 *
 * <p>This is a list-of-periods format, which differs from the single interval of
 * {@code contracts/events/extracao-concluida.schema.json}. The contract is not changed here (recorded for SCRUM-75).
 */
public record UnavailabilityPeriod(LocalDate startDate, LocalDate endDate, Set<DayOfWeek> weekdays,
		LocalTime startTime, LocalTime endTime) {

	public UnavailabilityPeriod {
		Objects.requireNonNull(startDate, "startDate");
		Objects.requireNonNull(endDate, "endDate");
		if (endDate.isBefore(startDate)) {
			throw new IllegalArgumentException("endDate is before startDate");
		}
		if ((startTime == null) != (endTime == null)) {
			throw new IllegalArgumentException("startTime and endTime must both be set or both be null");
		}
		weekdays = weekdays == null || weekdays.isEmpty() ? Set.of()
				: Collections.unmodifiableSet(EnumSet.copyOf(weekdays));
	}

	/** A period of whole days, with no weekday pattern. */
	public static UnavailabilityPeriod ofDays(LocalDate startDate, LocalDate endDate) {
		return new UnavailabilityPeriod(startDate, endDate, Set.of(), null, null);
	}
}
