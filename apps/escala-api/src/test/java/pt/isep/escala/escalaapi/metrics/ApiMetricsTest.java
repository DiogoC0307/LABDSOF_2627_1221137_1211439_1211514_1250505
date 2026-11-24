package pt.isep.escala.escalaapi.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** The metrics of escala-api without a broker or a database (SCRUM-97). */
class ApiMetricsTest {

	@Test
	@DisplayName("TC-SCRUM-97-08 an escala.* meter with a tag outside the allowed keys is dropped (no IDs, no free text, T-07)")
	void tagPolicy() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		registry.config().meterFilter(new MetricsConfiguration().escalaTagPolicy());

		registry.counter("escala.evaluations.requested", Tags.of("purpose", "ASSIGNMENT")).increment();
		registry.counter("escala.evaluations.requested", Tags.of("slotId", "00000000-0000-4000-8000-000000000001"))
				.increment();

		assertThat(registry.getMeters()).extracting(meter -> meter.getId().getTags().toString())
				.anyMatch(tags -> tags.contains("purpose=ASSIGNMENT")).noneMatch(tags -> tags.contains("slotId"));
	}

	@Test
	@DisplayName("TC-SCRUM-97-03 purpose takes ASSIGNMENT, SWAP, COVER and CANDIDATE (SCRUM-65); anything else is OTHER")
	void purposeValues() {
		assertThat(MetricTags.purposeValue("ASSIGNMENT")).isEqualTo("ASSIGNMENT");
		assertThat(MetricTags.purposeValue("SWAP")).isEqualTo("SWAP");
		assertThat(MetricTags.purposeValue("COVER")).isEqualTo("COVER");
		assertThat(MetricTags.purposeValue("CANDIDATE")).isEqualTo("CANDIDATE");
		assertThat(MetricTags.purposeValue("free text")).isEqualTo(MetricTags.OTHER);
		assertThat(MetricTags.purposeValue(null)).isEqualTo(MetricTags.OTHER);
		assertThat(MetricTags.resultValue("INELIGIBLE")).isEqualTo("INELIGIBLE");
		assertThat(MetricTags.resultValue("x")).isEqualTo(MetricTags.OTHER);
	}

	@Test
	@DisplayName("TC-SCRUM-97-03 requested counts by purpose; completed without its request in the outbox counts but is not timed")
	void requestedAndCompleted() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		EvaluationMetrics metrics = new EvaluationMetrics(registry, mock(JdbcTemplate.class));

		metrics.requested("ASSIGNMENT");
		metrics.requested("CANDIDATE");
		metrics.requested("CANDIDATE");
		metrics.completed(null, "ELIGIBLE", true, Instant.now());

		assertThat(registry.get("escala.evaluations.requested").tag("purpose", "CANDIDATE").counter().count())
				.isEqualTo(2.0);
		assertThat(registry.get("escala.evaluations.completed").tags("result", "ELIGIBLE", "applied", "true").counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.get("escala.evaluation.latency").tag("result", "ELIGIBLE").timer().count()).isZero();
	}
}
