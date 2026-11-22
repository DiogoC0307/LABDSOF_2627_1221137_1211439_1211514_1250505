package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import static pt.isep.escala.elegibilidadeworker.extracao.baseline.ExtractionAssert.assertExtracts;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * TC-SCRUM-99-07. Several periods in one message (06: "não posso sábado nem dia 10": one period each), slang and
 * dictation without accents or punctuation. Periods come back ordered by start date.
 */
class MultiplePeriodsTest {

	@ParameterizedTest(name = "TC-SCRUM-99-07 positive: \"{0}\" on {1} gives {2}")
	@CsvSource(delimiter = '|', value = {
			"não posso sábado nem dia 10 | 2026-10-26 | 2026-10-31 ; 2026-11-10",
			// single days are separate periods; only the plural ("às segundas") is a repeating pattern
			"não posso segunda e quarta | 2026-10-26 | 2026-10-28 ; 2026-11-02",
			"não posso sábado e domingo | 2026-10-26 | 2026-10-31 ; 2026-11-01",
			"não posso nem sexta nem sábado | 2026-10-26 | 2026-10-30 ; 2026-10-31",
			// each day keeps its own part of the day
			"não posso sábado de manhã e domingo à tarde | 2026-10-26 | 2026-10-31 08:00-14:00 ; 2026-11-01 14:00-20:00",
			// dictation: no accents, no punctuation
			"nao posso sexta de manha nem sabado a noite | 2026-10-26 | 2026-10-30 08:00-14:00 ; 2026-10-31 20:00-08:00",
			// a part of the day after a list applies to the whole list
			"não posso sexta nem sábado à noite | 2026-10-26 | 2026-10-30 20:00-08:00 ; 2026-10-31 20:00-08:00",
			"não posso nos dias 10 e 12 de novembro | 2026-10-26 | 2026-11-10 ; 2026-11-12",
			// slang: "n" is "não"
			"n posso sexta | 2026-10-26 | 2026-10-30",
			// only the part about when the member cannot serve counts
			"não posso sexta, mas posso sábado | 2026-10-26 | 2026-10-30",
	})
	void severalPeriods(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}

	@ParameterizedTest(name = "TC-SCRUM-99-07 negative: \"{0}\" gives no period")
	@CsvSource(delimiter = '|', value = {
			"posso sexta e sábado | 2026-10-26 | -",
			"posso sexta mas não posso | 2026-10-26 | -",
			"o João não pode sexta nem sábado | 2026-10-26 | -",
	})
	void noPeriods(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}
}
