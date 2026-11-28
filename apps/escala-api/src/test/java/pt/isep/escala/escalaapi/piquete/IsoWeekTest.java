package pt.isep.escala.escalaapi.piquete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** The {@code week} of {@code GET /api/v1/piquetes} is an ISO week, Monday to Sunday (RN12 week, SCRUM-65). */
class IsoWeekTest {

	@ParameterizedTest
	@CsvSource({
			"2026-W48, 2026-11-23, 2026-11-29",
			"2026-W01, 2025-12-29, 2026-01-04", // Week 1 is the week with the year's first Thursday, so it can start in December.
			"2026-W53, 2026-12-28, 2027-01-03" // 2026 has 53 weeks; the last one ends in January.
	})
	void aWeekRunsFromMondayToSunday(String week, LocalDate monday, LocalDate sunday) {
		IsoWeek parsed = IsoWeek.parse(week);

		assertThat(parsed.monday()).isEqualTo(monday);
		assertThat(parsed.sunday()).isEqualTo(sunday);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"2025-W53", // 2025 has only 52 weeks
			"2026-W00",
			"2026-W54",
			"2026-W8",
			"2026-48",
			"2026W48",
			"2026-w48",
			"2026-11-23",
			" 2026-W48"
	})
	void anythingElseIsRejected(String week) {
		assertThatThrownBy(() -> IsoWeek.parse(week)).isInstanceOf(IllegalArgumentException.class);
	}
}
