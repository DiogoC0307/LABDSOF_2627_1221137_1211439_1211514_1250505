package pt.isep.escala.escalaapi.piquete;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An ISO 8601 week such as {@code 2026-W48}: Monday to Sunday. Week 1 is the
 * week with the year's first Thursday, so it can start in December. Duty dates are Lisbon
 * dates, so no time zone is involved here. Public for the team's unavailability of a week (SCRUM-65).
 */
public record IsoWeek(LocalDate monday) {
	private static final Pattern FORMAT = Pattern.compile("(\\d{4})-W(\\d{2})");

	/**
	 * Parses a string into an {@code IsoWeek}.
	 * 
	 * @param text the string to parse
	 * @return the parsed week
	 * @throws IllegalArgumentException when the text is not a week that exists, e.g. {@code 2025-W54}
	 */
	public static IsoWeek parse(String text) {
		Matcher matcher = text == null ? null : FORMAT.matcher(text);
		if (matcher == null || !matcher.matches()) {
			throw new IllegalArgumentException("Week must be in format YYYY-Www (e.g. 2026-W48), but was " + text);
		}

		int year = Integer.parseInt(matcher.group(1));
		int week = Integer.parseInt(matcher.group(2));

		LocalDate weekOne = LocalDate.of(year, 1, 4); // Gets first week of the year from 4th of January (always in the 1st week)
		long weeksInYear = weekOne.range(IsoFields.WEEK_OF_WEEK_BASED_YEAR).getMaximum();
		if (week < 1 || week > weeksInYear) {
			throw new IllegalArgumentException("Week " + week + " is invalid for year " + year + ".");
		}

		LocalDate monday = weekOne.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) // Gets the Monday of the first week
								.plusWeeks(week - 1L); // Adds the number of weeks to get to the requested week
								
		return new IsoWeek(monday);
	}

	public LocalDate sunday() {
		return monday.plusDays(6);
	}
}
