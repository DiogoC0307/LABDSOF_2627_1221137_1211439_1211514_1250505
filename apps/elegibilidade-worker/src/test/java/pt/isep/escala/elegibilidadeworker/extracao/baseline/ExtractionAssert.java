package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityExtractor;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityPeriod;

/**
 * Test helper: runs the extractor and compares with a compact notation of the expected periods.
 *
 * <p>Notation: periods separated by {@code ;}, each {@code START[..END] [MON,WED] [HH:mm-HH:mm]}, ISO dates;
 * {@code -} means no period. Periods are listed in the order the extractor returns them (by start date).
 */
final class ExtractionAssert {

	static final UnavailabilityExtractor EXTRACTOR = new RuleBasedExtractor();

	private ExtractionAssert() {
	}

	static void assertExtracts(String text, String referenceDate, String expected) {
		assertExtracts(EXTRACTOR, text, referenceDate, expected);
	}

	static void assertExtracts(UnavailabilityExtractor extractor, String text, String referenceDate, String expected) {
		ExtractionResult actual = extractor.extract(text, LocalDate.parse(referenceDate));
		assertThat(actual.periods()).as("'%s' on %s", text, referenceDate).containsExactlyElementsOf(periods(expected));
	}

	static List<UnavailabilityPeriod> periods(String notation) {
		if (notation.strip().equals("-")) {
			return List.of();
		}
		return Arrays.stream(notation.split(";")).map(String::strip).map(ExtractionAssert::period).toList();
	}

	private static UnavailabilityPeriod period(String notation) {
		String[] parts = notation.split("\\s+");
		String[] dates = parts[0].split("\\.\\.");
		LocalDate start = LocalDate.parse(dates[0]);
		LocalDate end = dates.length > 1 ? LocalDate.parse(dates[1]) : start;
		Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
		LocalTime startTime = null;
		LocalTime endTime = null;
		for (int i = 1; i < parts.length; i++) {
			if (parts[i].startsWith("[")) {
				for (String code : parts[i].substring(1, parts[i].length() - 1).split(",")) {
					weekdays.add(Arrays.stream(DayOfWeek.values()).filter(d -> d.name().startsWith(code)).findFirst()
							.orElseThrow());
				}
			}
			else {
				String[] times = parts[i].split("-");
				startTime = LocalTime.parse(times[0]);
				endTime = LocalTime.parse(times[1]);
			}
		}
		return new UnavailabilityPeriod(start, end, weekdays, startTime, endTime);
	}
}
