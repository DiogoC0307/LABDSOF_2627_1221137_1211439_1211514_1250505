package pt.isep.escala.escalaapi.unavailability;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import pt.isep.escala.escalaapi.validation.InvalidRequestException.InvalidField;

/**
 * Reads a {@link ProposedPeriod} in the ExtracaoConcluida v2 format into a {@link PeriodPattern}, or adds the
 * {field, code} of what is wrong. The same rules for the worker's answer and for what the member confirms.
 */
final class PeriodPatterns {

	static final String REQUIRED = "required";
	static final String INVALID = "invalid";
	static final String ENDS_BEFORE_START = "ends_before_start";

	private static final Pattern TIME = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

	private static final Map<String, DayOfWeek> WEEKDAYS = Map.of(
			"MON", DayOfWeek.MONDAY, "TUE", DayOfWeek.TUESDAY, "WED", DayOfWeek.WEDNESDAY, "THU", DayOfWeek.THURSDAY,
			"FRI", DayOfWeek.FRIDAY, "SAT", DayOfWeek.SATURDAY, "SUN", DayOfWeek.SUNDAY);

	private PeriodPatterns() {
	}

	/** The pattern, or null with the problems added to {@code errors}, each field prefixed with {@code prefix}. */
	static PeriodPattern read(ProposedPeriod period, String prefix, List<InvalidField> errors) {
		int before = errors.size();
		if (period == null) {
			errors.add(new InvalidField(prefix, REQUIRED));
			return null;
		}
		LocalDate start = date(period.startDate(), prefix + ".startDate", errors);
		LocalDate end = date(period.endDate(), prefix + ".endDate", errors);
		if (start != null && end != null && end.isBefore(start)) {
			errors.add(new InvalidField(prefix + ".endDate", ENDS_BEFORE_START));
		}
		Set<DayOfWeek> weekdays = weekdays(period.weekdays(), prefix + ".weekdays", errors);
		LocalTime from = time(period.startTime(), period.endTime(), prefix + ".startTime", errors);
		LocalTime to = time(period.endTime(), period.startTime(), prefix + ".endTime", errors);
		if (from != null && from.equals(to)) {
			errors.add(new InvalidField(prefix + ".endTime", ENDS_BEFORE_START));
		}
		return errors.size() == before ? new PeriodPattern(start, end, weekdays, from, to) : null;
	}

	private static LocalDate date(String value, String field, List<InvalidField> errors) {
		if (value == null || value.isBlank()) {
			errors.add(new InvalidField(field, REQUIRED));
			return null;
		}
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException e) {
			errors.add(new InvalidField(field, INVALID));
			return null;
		}
	}

	/** Every day when none is given; the codes MON to SUN otherwise. */
	private static Set<DayOfWeek> weekdays(List<String> values, String field, List<InvalidField> errors) {
		if (values == null || values.isEmpty()) {
			return EnumSet.allOf(DayOfWeek.class);
		}
		Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
		for (String value : values) {
			DayOfWeek day = value == null ? null : WEEKDAYS.get(value);
			if (day == null) {
				errors.add(new InvalidField(field, INVALID));
				return days;
			}
			days.add(day);
		}
		return days;
	}

	/** A time HH:mm, which needs its pair (startTime with endTime); null when neither is given. */
	private static LocalTime time(String value, String pair, String field, List<InvalidField> errors) {
		boolean absent = value == null || value.isEmpty();
		boolean pairAbsent = pair == null || pair.isEmpty();
		if (absent) {
			if (!pairAbsent) {
				errors.add(new InvalidField(field, REQUIRED));
			}
			return null;
		}
		if (!TIME.matcher(value).matches()) {
			errors.add(new InvalidField(field, INVALID));
			return null;
		}
		return LocalTime.parse(value);
	}
}
