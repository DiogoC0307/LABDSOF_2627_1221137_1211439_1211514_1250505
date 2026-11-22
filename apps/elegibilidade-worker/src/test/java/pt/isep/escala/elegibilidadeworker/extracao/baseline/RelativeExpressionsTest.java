package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import static pt.isep.escala.elegibilidadeworker.extracao.baseline.ExtractionAssert.assertExtracts;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * TC-SCRUM-99-04. "hoje", "amanhã", "esta semana", "próxima semana", "para a semana", "nos próximos N dias". README of
 * the dataset: "esta semana" is from the reference date to Sunday; "próxima semana" is Monday to Sunday of the
 * following week; "nos próximos N dias" starts tomorrow. Decision: "para a semana" is next week, unless the message is
 * vague ("se calhar", "talvez"), which gives nothing (06).
 */
class RelativeExpressionsTest {

	@ParameterizedTest(name = "TC-SCRUM-99-04 positive: \"{0}\" on {1} gives {2}")
	@CsvSource(delimiter = '|', value = {
			"não posso hoje | 2026-10-26 | 2026-10-26",
			"não posso amanhã | 2026-10-26 | 2026-10-27",
			"não posso depois de amanhã | 2026-10-26 | 2026-10-28",
			// change of month and of year (06: "relative dates across months and years")
			"não posso amanhã | 2026-10-31 | 2026-11-01",
			"não posso amanhã | 2026-12-31 | 2027-01-01",
			// this week: from the reference date to Sunday
			"não posso esta semana | 2026-10-26 | 2026-10-26..2026-11-01",
			"não posso esta semana | 2026-11-12 | 2026-11-12..2026-11-15",
			// next week: Monday to Sunday of the following week
			"não posso a próxima semana | 2026-10-26 | 2026-11-02..2026-11-08",
			"não posso próxima semana | 2026-11-12 | 2026-11-16..2026-11-22",
			"não posso na semana que vem | 2026-10-26 | 2026-11-02..2026-11-08",
			"para a semana não posso | 2026-10-26 | 2026-11-02..2026-11-08",
			// in the next N days, starting tomorrow
			"não posso nos próximos 3 dias | 2026-10-26 | 2026-10-27..2026-10-29",
			"não posso nos próximos 10 dias | 2026-10-26 | 2026-10-27..2026-11-05",
	})
	void relative(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}

	@ParameterizedTest(name = "TC-SCRUM-99-04 negative: \"{0}\" gives no period")
	@CsvSource(delimiter = '|', value = {
			// 06: "para a semana se calhar" is vague: no entry
			"para a semana se calhar | 2026-10-26 | -",
			"talvez para a semana | 2026-10-26 | -",
			"talvez amanhã | 2026-10-26 | -",
			// the past is rejected (06)
			"não posso ontem | 2026-10-26 | -",
			// "esta semana" in a message about when the member CAN serve
			"só posso esta semana | 2026-10-26 | -",
	})
	void notFirm(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}
}
