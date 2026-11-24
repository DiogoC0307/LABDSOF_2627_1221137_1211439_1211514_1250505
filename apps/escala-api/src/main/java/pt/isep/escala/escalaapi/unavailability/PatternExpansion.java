package pt.isep.escala.escalaapi.unavailability;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns patterns in Lisbon time into continuous periods in UTC, [start, end), for the SCRUM-64 table (SCRUM-75,
 * decision D: no new columns). Whole days in a row become one period; periods that overlap or touch are joined.
 * A local time that does not exist on the day summer time starts is moved forward by the hour, as java.time does.
 */
final class PatternExpansion {

	static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");

	record Interval(Instant start, Instant end) {
	}

	private PatternExpansion() {
	}

	/** Every pattern expanded, then joined: sorted by start, with no two periods overlapping or touching. */
	static List<Interval> expand(List<PeriodPattern> patterns) {
		List<Interval> all = new ArrayList<>();
		patterns.forEach(pattern -> all.addAll(days(pattern)));
		return join(all);
	}

	/** One interval per day of the pattern, not joined; empty when no day matches the weekdays. */
	static List<Interval> days(PeriodPattern pattern) {
		List<Interval> days = new ArrayList<>();
		for (LocalDate day = pattern.startDate(); !day.isAfter(pattern.endDate()); day = day.plusDays(1)) {
			if (pattern.weekdays() != null && !pattern.weekdays().contains(day.getDayOfWeek())) {
				continue;
			}
			if (pattern.startTime() == null) {
				days.add(new Interval(day.atStartOfDay(LISBON).toInstant(), day.plusDays(1).atStartOfDay(LISBON).toInstant()));
			} else {
				LocalDate endDay = pattern.endTime().isAfter(pattern.startTime()) ? day : day.plusDays(1);
				days.add(new Interval(ZonedDateTime.of(day, pattern.startTime(), LISBON).toInstant(),
						ZonedDateTime.of(endDay, pattern.endTime(), LISBON).toInstant()));
			}
		}
		return days;
	}

	static List<Interval> join(List<Interval> intervals) {
		List<Interval> sorted = new ArrayList<>(intervals);
		sorted.sort(Comparator.comparing(Interval::start));
		List<Interval> joined = new ArrayList<>();
		for (Interval next : sorted) {
			int last = joined.size() - 1;
			if (last >= 0 && !next.start().isAfter(joined.get(last).end())) {
				Interval current = joined.get(last);
				joined.set(last, new Interval(current.start(),
						next.end().isAfter(current.end()) ? next.end() : current.end()));
			} else {
				joined.add(next);
			}
		}
		return joined;
	}
}
