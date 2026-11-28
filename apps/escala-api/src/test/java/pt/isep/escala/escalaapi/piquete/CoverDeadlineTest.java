package pt.isep.escala.escalaapi.piquete;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CoverDeadlineTest {

	private static final Instant OPENED = Instant.parse("2026-12-05T10:00:00Z");

	@Test
	@DisplayName("TC-SCRUM-72-04 30 min after the request, or 2 h before the shift, whichever comes first")
	void whicheverComesFirst() {
		CoverDeadline deadline = new CoverDeadline(Duration.ofMinutes(30), Duration.ofHours(2));

		// The shift is far away: 30 min after the request
		assertThat(deadline.of(OPENED, Instant.parse("2026-12-05T20:00:00Z"))).isEqualTo(Instant.parse("2026-12-05T10:30:00Z"));
		// The shift starts in 2 h 10 min: 2 h before it comes first
		assertThat(deadline.of(OPENED, Instant.parse("2026-12-05T12:10:00Z"))).isEqualTo(Instant.parse("2026-12-05T10:10:00Z"));
		// The shift is less than 2 h away: at once
		assertThat(deadline.hasPassed(OPENED, Instant.parse("2026-12-05T11:00:00Z"), OPENED)).isTrue();
		assertThat(deadline.hasPassed(OPENED, Instant.parse("2026-12-05T20:00:00Z"), Instant.parse("2026-12-05T10:29:59Z")))
				.isFalse();
		assertThat(deadline.hasPassed(OPENED, Instant.parse("2026-12-05T20:00:00Z"), Instant.parse("2026-12-05T10:30:00Z")))
				.isTrue();
	}

	@Test
	@DisplayName("TC-SCRUM-72-04 both times can be set per corps")
	void bothTimesCanBeSet() {
		CoverDeadline deadline = new CoverDeadline(Duration.ofMinutes(10), Duration.ofHours(6));

		assertThat(deadline.of(OPENED, Instant.parse("2026-12-06T10:00:00Z"))).isEqualTo(Instant.parse("2026-12-05T10:10:00Z"));
		assertThat(deadline.of(OPENED, Instant.parse("2026-12-05T16:05:00Z"))).isEqualTo(Instant.parse("2026-12-05T10:05:00Z"));
	}
}
