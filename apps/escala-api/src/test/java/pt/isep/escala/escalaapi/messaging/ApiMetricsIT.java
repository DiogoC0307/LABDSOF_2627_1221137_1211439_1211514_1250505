package pt.isep.escala.escalaapi.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;

/**
 * TC-SCRUM-97-04, 06, 07 and 08 for escala-api, against a real PostgreSQL and RabbitMQ. The Prometheus output is read
 * from the registry: the endpoint itself is opened in SecurityConfig with SCRUM-88.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@AutoConfigureMetrics
@Testcontainers
class ApiMetricsIT {

	private static final Pattern UUID_PATTERN = Pattern
			.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MeterRegistry registry;

	@Autowired
	PrometheusMeterRegistry prometheus;

	@Autowired
	MockMvc mockMvc;

	/** An AvaliacaoPedida in the outbox, written {@code secondsAgo} ago; returns its event ID. */
	private UUID requestInOutbox(long secondsAgo) {
		UUID requestEventId = UUID.randomUUID();
		jdbc.update("insert into outbox_event (id, created_at, payload, published_at, routing_key) values (?, ?, ?, ?, ?)",
				requestEventId, OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(secondsAgo), "{}",
				OffsetDateTime.now(ZoneOffset.UTC), "avaliacao.pedida");
		return requestEventId;
	}

	private static String concluida(UUID eventId, UUID requestEventId, UUID assignmentId, String result) {
		return """
				{"eventId":"%s","type":"AvaliacaoConcluida","schemaVersion":1,"occurredAt":"%s","traceId":"%s",
				 "requestEventId":"%s","assignmentId":"%s","result":"%s","violatedRules":[],
				 "engineVersion":"SEMPRE_ELEGIVEL"}
				""".formatted(eventId, Instant.now(), UUID.randomUUID(), requestEventId, assignmentId, result);
	}

	private double completed(String result, String applied) {
		Counter counter = registry.find("escala.evaluations.completed").tags("result", result, "applied", applied)
				.counter();
		return counter == null ? 0 : counter.count();
	}

	@Test
	@DisplayName("TC-SCRUM-97-04 an applied AvaliacaoConcluida counts once by result, and its latency is from the request's outbox row (SLI 1)")
	void completedAndLatency() {
		Timer latency = registry.get("escala.evaluation.latency").tag("result", "ELIGIBLE").timer();
		long timedBefore = latency.count();
		double before = completed("ELIGIBLE", "true");
		UUID assignmentId = MessagingTestSupport.insertPendingSlot(jdbc);
		UUID requestEventId = requestInOutbox(2);
		UUID eventId = UUID.randomUUID();
		byte[] answer = concluida(eventId, requestEventId, assignmentId, "ELIGIBLE").getBytes(StandardCharsets.UTF_8);

		MessagingTestSupport.send(rabbitTemplate, "avaliacao.concluida", answer);
		await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
			assertThat(completed("ELIGIBLE", "true")).isEqualTo(before + 1);
			assertThat(latency.count()).isEqualTo(timedBefore + 1);
		});
		assertThat(latency.max(TimeUnit.MILLISECONDS)).isBetween(2_000.0, 30_000.0);

		// the same event again is a duplicate: not counted, not timed
		MessagingTestSupport.send(rabbitTemplate, "avaliacao.concluida", answer);
		await().pollDelay(Duration.ofSeconds(3)).until(() -> true);
		assertThat(completed("ELIGIBLE", "true")).isEqualTo(before + 1);
		assertThat(latency.count()).isEqualTo(timedBefore + 1);
		// the 5 s target of SLI 1 is an exact bucket
		assertThat(prometheus.scrape())
				.contains("escala_evaluation_latency_seconds_bucket{application=\"escala-api\",result=\"ELIGIBLE\",le=\"5.0\"}");
	}

	@Test
	@DisplayName("TC-SCRUM-97-06 a malformed AvaliacaoConcluida is counted as dead-lettered and shows in the DLQ gauge")
	void countsDeadLetters() {
		double before = registry.get("escala.messages.dead_lettered").tag("queue", MessagingTestSupport.QUEUE).counter()
				.count();
		MessagingTestSupport.send(rabbitTemplate, "avaliacao.concluida", "not json".getBytes(StandardCharsets.UTF_8));

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(
				registry.get("escala.messages.dead_lettered").tag("queue", MessagingTestSupport.QUEUE).counter().count())
				.isEqualTo(before + 1));
		await().atMost(Duration.ofSeconds(40)).untilAsserted(() -> {
			Gauge gauge = registry.find("escala.dlq.messages").tag("queue", MessagingTestSupport.DLQ).gauge();
			assertThat(gauge).isNotNull();
			assertThat(gauge.value()).isGreaterThanOrEqualTo(1.0);
		});
	}

	@Test
	@DisplayName("TC-SCRUM-97-07 HTTP requests are counted by method, URI template and status; an ID in the path stays out of the labels")
	void httpRequests() throws Exception {
		UUID someone = UUID.randomUUID();
		mockMvc.perform(get("/api/v1/members/" + someone).header("Authorization", MockTokens.admin()));

		assertThat(registry.find("http.server.requests").tag("uri", "/api/v1/members/{id}").timers()).isNotEmpty();
		assertThat(prometheus.scrape()).contains("http_server_requests_seconds_count").doesNotContain(someone.toString());
	}

	@Test
	@DisplayName("TC-SCRUM-97-08 after real traffic no label of the Prometheus output holds an ID (threat model T-07)")
	void noIdsInLabels() throws Exception {
		UUID assignmentId = MessagingTestSupport.insertPendingSlot(jdbc);
		UUID requestEventId = requestInOutbox(1);
		MessagingTestSupport.send(rabbitTemplate, "avaliacao.concluida",
				concluida(UUID.randomUUID(), requestEventId, assignmentId, "FAILED").getBytes(StandardCharsets.UTF_8));
		mockMvc.perform(get("/api/v1/members/" + UUID.randomUUID()).header("Authorization", MockTokens.admin()));
		await().atMost(Duration.ofSeconds(20)).until(() -> completed("FAILED", "true") >= 1);

		for (String line : prometheus.scrape().split("\n")) {
			if (!line.startsWith("#")) {
				int labels = line.indexOf('{');
				String labelPart = labels < 0 ? "" : line.substring(labels, line.lastIndexOf('}') + 1);
				assertThat(UUID_PATTERN.matcher(labelPart).find()).as(line).isFalse();
			}
		}
	}
}
