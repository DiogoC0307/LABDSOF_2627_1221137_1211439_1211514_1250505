package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import static pt.isep.escala.elegibilidadeworker.extracao.baseline.ExtractionAssert.assertExtracts;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * TC-SCRUM-99-06. Ranges ("de 3 a 7 de novembro") and limits ("até dezembro"). README of the dataset: "até dezembro" is
 * until 30 November. Decisions: an end limit that names a day ("até dia 15 de novembro") includes that day; a range or
 * limit with no start begins on the reference date.
 */
class RangesAndLimitsTest {

	@ParameterizedTest(name = "TC-SCRUM-99-06 positive: \"{0}\" on {1} gives {2}")
	@CsvSource(delimiter = '|', value = {
			// 06: "de 3 a 7 de novembro estou fora" on 2026-10-26
			"de 3 a 7 de novembro estou fora | 2026-10-26 | 2026-11-03..2026-11-07",
			"não posso do dia 3 ao dia 7 de novembro | 2026-10-26 | 2026-11-03..2026-11-07",
			"não posso de 3/11 a 7/11 | 2026-10-26 | 2026-11-03..2026-11-07",
			"não posso de 3 de novembro a 7 de novembro | 2026-10-26 | 2026-11-03..2026-11-07",
			// a range across two months; the month of the start is inferred
			"não posso de 28 de outubro a 2 de novembro | 2026-10-26 | 2026-10-28..2026-11-02",
			"não posso de 28 a 2 de novembro | 2026-10-26 | 2026-10-28..2026-11-02",
			// change of year (2026-12-28)
			"não posso de 30 de dezembro a 3 de janeiro | 2026-12-28 | 2026-12-30..2027-01-03",
			// a range of weekdays: each is its next occurrence after the reference date
			"estou fora de segunda a sexta | 2026-10-26 | 2026-11-02..2026-11-06",
			// 06: "não posso aos domingos de manhã até dezembro" on 2026-10-26
			"não posso aos domingos de manhã até dezembro | 2026-10-26 | 2026-10-26..2026-11-30 [SUN] 08:00-14:00",
			"estou fora até dezembro | 2026-10-26 | 2026-10-26..2026-11-30",
			"não posso até dia 15 de novembro | 2026-10-26 | 2026-10-26..2026-11-15",
			// "até janeiro" in December is until the end of December
			"estou fora até janeiro | 2026-12-28 | 2026-12-28..2026-12-31",
	})
	void rangesAndLimits(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}

	@ParameterizedTest(name = "TC-SCRUM-99-06 negative: \"{0}\" gives no period")
	@CsvSource(delimiter = '|', value = {
			// "de ... a ..." that are not dates
			"de manhã a tarde não posso | 2026-10-26 | -",
			// a range in a message about when the member CAN serve
			"só posso de 3 a 7 de novembro | 2026-10-26 | -",
			// a range whose end does not exist
			"não posso de 30 a 31 de abril | 2026-10-26 | -",
	})
	void notARange(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}
}
