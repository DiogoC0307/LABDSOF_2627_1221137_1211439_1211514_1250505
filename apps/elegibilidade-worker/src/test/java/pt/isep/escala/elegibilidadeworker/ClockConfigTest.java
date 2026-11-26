package pt.isep.escala.elegibilidadeworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The poll clock is UTC (ADR 0012). */
class ClockConfigTest {

	@Test
	@DisplayName("the application clock is UTC")
	void utc() {
		assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneOffset.UTC);
	}
}
