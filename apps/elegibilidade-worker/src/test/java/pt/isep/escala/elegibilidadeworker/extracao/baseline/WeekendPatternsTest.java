package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import static pt.isep.escala.elegibilidadeworker.extracao.baseline.ExtractionAssert.assertExtracts;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** TC-SCRUM-99-02. "fds" and "fim de semana" are the coming Saturday and Sunday (README of the dataset; 06). */
class WeekendPatternsTest {

	@ParameterizedTest(name = "TC-SCRUM-99-02 positive: \"{0}\" on {1} gives {2}")
	@CsvSource(delimiter = '|', value = {
			// 06: "não posso este fds" on 2026-10-26
			"não posso este fds | 2026-10-26 | 2026-10-31..2026-11-01",
			"não posso o fds | 2026-10-26 | 2026-10-31..2026-11-01",
			"não posso o próximo fds | 2026-10-26 | 2026-10-31..2026-11-01",
			"não posso este fim de semana | 2026-10-26 | 2026-10-31..2026-11-01",
			"fds não posso | 2026-10-26 | 2026-10-31..2026-11-01",
			"n posso este fds | 2026-10-26 | 2026-10-31..2026-11-01",
			// from a Thursday
			"não posso este fds | 2026-11-12 | 2026-11-14..2026-11-15",
			// change of year (2026-12-28 is a Monday)
			"não posso este fds | 2026-12-28 | 2027-01-02..2027-01-03",
			// the times of a part of the day apply to every day in the range (README)
			"não posso este fds à noite | 2026-10-26 | 2026-10-31..2026-11-01 20:00-08:00",
			// repeating weekends
			"não posso aos fins de semana | 2026-10-26 | 2026-10-26..2026-11-30 [SAT,SUN]",
	})
	void weekend(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}

	@ParameterizedTest(name = "TC-SCRUM-99-02 negative: \"{0}\" gives no period")
	@CsvSource(delimiter = '|', value = {
			"só posso este fds | 2026-10-26 | -",
			"posso o fds | 2026-10-26 | -",
			"o João não pode este fds | 2026-10-26 | -",
	})
	void notUnavailability(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}
}
