package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import static pt.isep.escala.elegibilidadeworker.extracao.baseline.ExtractionAssert.assertExtracts;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * TC-SCRUM-99-05. Explicit dates: "3 de novembro", "3/11", "dia 10" (06). A date without a year is its next occurrence
 * on or after the reference date.
 */
class ExplicitDatesTest {

	@ParameterizedTest(name = "TC-SCRUM-99-05 positive: \"{0}\" on {1} gives {2}")
	@CsvSource(delimiter = '|', value = {
			"não posso 3 de novembro | 2026-10-26 | 2026-11-03",
			"não posso dia 3 de novembro | 2026-10-26 | 2026-11-03",
			"não posso 3/11 | 2026-10-26 | 2026-11-03",
			"não posso 3 de novembro de 2026 | 2026-10-26 | 2026-11-03",
			"não posso 3/11/2026 | 2026-10-26 | 2026-11-03",
			// "dia 10" alone is the next day 10 (06: "não posso sábado nem dia 10")
			"não posso dia 10 | 2026-10-26 | 2026-11-10",
			// the reference date itself counts for an explicit day
			"não posso dia 26 | 2026-10-26 | 2026-10-26",
			// a day already gone this month is next month's
			"não posso dia 5 | 2026-11-12 | 2026-12-05",
			// change of year (2026-12-28)
			"não posso 3 de janeiro | 2026-12-28 | 2027-01-03",
			"não posso 3/1 | 2026-12-28 | 2027-01-03",
			// a weekday followed by its date is one date, not two
			"não posso sexta, dia 6 de novembro | 2026-10-26 | 2026-11-06",
	})
	void explicitDates(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}

	@ParameterizedTest(name = "TC-SCRUM-99-05 negative: \"{0}\" gives no period")
	@CsvSource(delimiter = '|', value = {
			// dates that do not exist
			"não posso 31/11 | 2026-10-26 | -",
			"não posso 30 de fevereiro | 2026-10-26 | -",
			"não posso dia 31 de abril | 2026-10-26 | -",
			// "dia" without a number
			"não posso um dia destes | 2026-10-26 | -",
			// a date in a message about when the member CAN serve
			"só posso dia 10 | 2026-10-26 | -",
	})
	void notADate(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}
}
