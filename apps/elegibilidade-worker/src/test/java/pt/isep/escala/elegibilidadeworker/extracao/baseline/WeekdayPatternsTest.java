package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import static pt.isep.escala.elegibilidadeworker.extracao.baseline.ExtractionAssert.assertExtracts;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * TC-SCRUM-99-01. Weekday names, alone and repeating. Conventions: README of the evaluation dataset ("a weekday alone
 * is its next occurrence after the reference date"); 06, expected output of "não posso aos domingos de manhã até
 * dezembro". Decision for "aos domingos" with no limit: until the end of the next month.
 */
class WeekdayPatternsTest {

	@ParameterizedTest(name = "TC-SCRUM-99-01 positive: \"{0}\" on {1} gives {2}")
	@CsvSource(delimiter = '|', value = {
			// 2026-10-26 is a Monday
			"não posso quinta | 2026-10-26 | 2026-10-29",
			"quinta não posso | 2026-10-26 | 2026-10-29",
			"não posso sexta | 2026-10-26 | 2026-10-30",
			"não posso sábado | 2026-10-26 | 2026-10-31",
			"nao posso sabado | 2026-10-26 | 2026-10-31",
			"não posso domingo | 2026-10-26 | 2026-11-01",
			"não posso terça-feira | 2026-10-26 | 2026-10-27",
			"não posso quarta | 2026-10-26 | 2026-10-28",
			// the reference date itself is not "after" the reference date: a Monday on a Monday is the next one
			"não posso segunda | 2026-10-26 | 2026-11-02",
			// a weekday alone, on another reference date (2026-11-12 is a Thursday)
			"não posso quinta | 2026-11-12 | 2026-11-19",
			"não posso sexta | 2026-11-12 | 2026-11-13",
			// repeating: start is the reference date, end is the end of the next month
			"não posso aos domingos | 2026-10-26 | 2026-10-26..2026-11-30 [SUN]",
			"não posso aos sábados | 2026-10-26 | 2026-10-26..2026-11-30 [SAT]",
			"não posso às segundas e quartas | 2026-10-26 | 2026-10-26..2026-11-30 [MON,WED]",
			"não posso aos domingos | 2026-11-12 | 2026-11-12..2026-12-31 [SUN]",
			// change of year
			"não posso aos domingos | 2026-12-28 | 2026-12-28..2027-01-31 [SUN]",
	})
	void weekdays(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}

	@ParameterizedTest(name = "TC-SCRUM-99-01 negative: \"{0}\" gives no period")
	@CsvSource(delimiter = '|', value = {
			// a message about when the member CAN serve is not unavailability (06)
			"só posso quinta | 2026-10-26 | -",
			"posso aos domingos | 2026-10-26 | -",
			// about someone else (06)
			"o João não pode quinta | 2026-10-26 | -",
			// no date at all
			"olá, bom dia | 2026-10-26 | -",
	})
	void notUnavailability(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}
}
