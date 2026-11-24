package pt.isep.escala.elegibilidadeworker;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The clock in UTC (ADR 0012). Tests replace it with a fixed clock. */
@Configuration
class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
