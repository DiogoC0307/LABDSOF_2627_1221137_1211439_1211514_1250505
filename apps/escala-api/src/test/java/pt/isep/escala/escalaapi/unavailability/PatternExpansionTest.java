package pt.isep.escala.escalaapi.unavailability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.isep.escala.escalaapi.unavailability.PatternExpansion.Interval;

/**
 * On confirmation a pattern in Lisbon time becomes continuous periods in UTC (SCRUM-75, decision D): the format of
 * ExtracaoConcluida v2, stored in the SCRUM-64 table without new columns.
 */
class PatternExpansionTest {

	@Test
	@DisplayName("TC-SCRUM-75-11 every Sunday from 08:00 to 14:00 gives one period per Sunday, in UTC across the change to summer time")
	void weekdaysWithTimes() {
		List<Interval> periods = PatternExpansion.expand(List.of(pattern("2027-03-14", "2027-04-04",
				EnumSet.of(DayOfWeek.SUNDAY), "08:00", "14:00")));

		assertThat(periods).containsExactly(
				interval("2027-03-14T08:00:00Z", "2027-03-14T14:00:00Z"),
				interval("2027-03-21T08:00:00Z", "2027-03-21T14:00:00Z"),
				interval("2027-03-28T07:00:00Z", "2027-03-28T13:00:00Z"),
				interval("2027-04-04T07:00:00Z", "2027-04-04T13:00:00Z"));
	}

	@Test
	@DisplayName("TC-SCRUM-75-11 whole Saturdays and Sundays give one period per weekend")
	void weekendsAreJoined() {
		List<Interval> periods = PatternExpansion.expand(List.of(pattern("2026-11-28", "2026-12-06",
				EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), null, null)));

		assertThat(periods).containsExactly(
				interval("2026-11-28T00:00:00Z", "2026-11-30T00:00:00Z"),
				interval("2026-12-05T00:00:00Z", "2026-12-07T00:00:00Z"));
	}

	@Test
	@DisplayName("TC-SCRUM-75-12 whole days are one continuous period; a window that crosses midnight ends the next day; Lisbon days in summer and on the 23-hour day")
	void wholeDaysAndMidnight() {
		assertThat(PatternExpansion.expand(List.of(pattern("2026-12-05", "2026-12-07", null, null, null))))
				.containsExactly(interval("2026-12-05T00:00:00Z", "2026-12-08T00:00:00Z"));
		assertThat(PatternExpansion.expand(List.of(pattern("2026-12-05", "2026-12-05", null, "20:00", "08:00"))))
				.containsExactly(interval("2026-12-05T20:00:00Z", "2026-12-06T08:00:00Z"));
		assertThat(PatternExpansion.expand(List.of(pattern("2027-07-01", "2027-07-01", null, null, null))))
				.containsExactly(interval("2027-06-30T23:00:00Z", "2027-07-01T23:00:00Z"));
		assertThat(PatternExpansion.expand(List.of(pattern("2027-03-28", "2027-03-28", null, null, null))))
				.containsExactly(interval("2027-03-28T00:00:00Z", "2027-03-28T23:00:00Z"));
	}

	@Test
	@DisplayName("TC-SCRUM-75-12 periods that overlap or touch are joined, so no two saved periods overlap")
	void overlappingPeriodsAreJoined() {
		List<Interval> periods = PatternExpansion.expand(List.of(
				pattern("2026-12-05", "2026-12-06", null, null, null),
				pattern("2026-12-07", "2026-12-07", null, "00:00", "10:00"),
				pattern("2026-12-06", "2026-12-06", null, "09:00", "12:00"),
				pattern("2026-12-10", "2026-12-10", null, "09:00", "12:00")));

		assertThat(periods).containsExactly(
				interval("2026-12-05T00:00:00Z", "2026-12-07T10:00:00Z"),
				interval("2026-12-10T09:00:00Z", "2026-12-10T12:00:00Z"));
	}

	private static PeriodPattern pattern(String start, String end, Set<DayOfWeek> weekdays, String from, String to) {
		return new PeriodPattern(LocalDate.parse(start), LocalDate.parse(end), weekdays,
				from == null ? null : LocalTime.parse(from), to == null ? null : LocalTime.parse(to));
	}

	private static Interval interval(String start, String end) {
		return new Interval(Instant.parse(start), Instant.parse(end));
	}
}
