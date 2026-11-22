package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import static pt.isep.escala.elegibilidadeworker.extracao.baseline.ExtractionAssert.assertExtracts;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * TC-SCRUM-99-03. "manhã / tarde / noite": morning 08:00-14:00, afternoon 14:00-20:00, night 20:00-08:00 (README of the
 * dataset; 06 for the night). The windows are configuration (ADR 0014).
 */
class PartsOfDayPatternsTest {

	@ParameterizedTest(name = "TC-SCRUM-99-03 positive: \"{0}\" on {1} gives {2}")
	@CsvSource(delimiter = '|', value = {
			// 06: "quinta à noite não posso" on 2026-10-26
			"quinta à noite não posso | 2026-10-26 | 2026-10-29 20:00-08:00",
			"nao posso quinta a noite | 2026-10-26 | 2026-10-29 20:00-08:00",
			"não posso sexta de manhã | 2026-10-26 | 2026-10-30 08:00-14:00",
			"não posso sexta à tarde | 2026-10-26 | 2026-10-30 14:00-20:00",
			"não posso amanhã de manhã | 2026-10-26 | 2026-10-27 08:00-14:00",
			"não posso hoje à noite | 2026-10-26 | 2026-10-26 20:00-08:00",
			"não posso esta noite | 2026-10-26 | 2026-10-26 20:00-08:00",
			// the part applies when it is written before the day, too
			"não posso na manhã de sexta | 2026-10-26 | 2026-10-30 08:00-14:00",
	})
	void partsOfDay(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}

	@ParameterizedTest(name = "TC-SCRUM-99-03 negative: \"{0}\" gives {2}")
	@CsvSource(delimiter = '|', value = {
			// a part of the day is not enough: without a date there is nothing to propose
			"não posso à noite | 2026-10-26 | -",
			// about when the member CAN serve
			"só posso à noite | 2026-10-26 | -",
			// "de manhã à noite" is not a range of dates
			"de manhã à noite não posso | 2026-10-26 | -",
			// without a part of the day there are no times
			"não posso sexta | 2026-10-26 | 2026-10-30",
	})
	void notAPartOfDay(String text, String referenceDate, String expected) {
		assertExtracts(text, referenceDate, expected);
	}

	@Test
	@DisplayName("TC-SCRUM-99-03 the windows come from configuration, not from constants (ADR 0014)")
	void configuredDayParts() {
		var corps = new RuleBasedExtractor(new DayParts(LocalTime.of(6, 0), LocalTime.of(12, 0), LocalTime.of(18, 0)));
		assertExtracts(corps, "não posso sexta de manhã", "2026-10-26", "2026-10-30 06:00-12:00");
		assertExtracts(corps, "não posso sexta à tarde", "2026-10-26", "2026-10-30 12:00-18:00");
		assertExtracts(corps, "não posso sexta à noite", "2026-10-26", "2026-10-30 18:00-06:00");
	}
}
