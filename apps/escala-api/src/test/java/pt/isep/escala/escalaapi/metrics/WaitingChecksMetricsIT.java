package pt.isep.escala.escalaapi.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * TC-SCRUM-98-01 to 04: {@code escala_checks_waiting{purpose}} goes up with a request still waiting for
 * elegibilidade-worker and down when its answer arrives, against a real PostgreSQL and RabbitMQ. Nothing consumes the
 * worker's queues here: the answers are sent as the worker would send them.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMetrics
@Testcontainers
class WaitingChecksMetricsIT {

	/** A seeded user (V3), so the rows below have a member. */
	private static final UUID SYNTHETIC_MEMBER = UUID.fromString("00000000-0000-4000-8000-000000000001");

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	WaitingChecksMetrics metrics;

	@Autowired
	MeterRegistry registry;

	@Autowired
	PrometheusMeterRegistry prometheus;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	RabbitTemplate rabbitTemplate;

	private double gauge(String purpose) {
		return registry.get("escala.checks.waiting").tag("purpose", purpose).gauge().value();
	}

	private long refreshed(String purpose) {
		metrics.refresh();
		return metrics.waiting(purpose);
	}

	private void send(String routingKey, String json) {
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbitTemplate.send("escala.events", routingKey, new Message(json.getBytes(StandardCharsets.UTF_8), properties));
	}

	private void sendAvaliacaoConcluida(UUID assignmentId) {
		send("avaliacao.concluida", """
				{"eventId":"%s","type":"AvaliacaoConcluida","schemaVersion":1,"occurredAt":"%s","traceId":"%s",
				 "requestEventId":"%s","assignmentId":"%s","result":"ELIGIBLE","violatedRules":[],
				 "engineVersion":"SEMPRE_ELEGIVEL"}
				""".formatted(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), UUID.randomUUID(), assignmentId));
	}

	@Test
	@DisplayName("TC-SCRUM-98-01 a slot still PENDING counts as an ASSIGNMENT waiting for the worker (the 15 s refresh shows it) and stops counting when its AvaliacaoConcluida arrives")
	void assignment() {
		long before = refreshed(WaitingChecksMetrics.ASSIGNMENT);
		UUID piqueteId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		jdbc.update("INSERT INTO piquete (id, duty_date) VALUES (?, ?)", piqueteId, LocalDate.of(2031, 9, 6));
		jdbc.update("INSERT INTO slot (id, assignment_id, role, status, piquete_id, shift_starts_at) "
				+ "VALUES (?, ?, 'CREW_MEMBER', 'PENDING', ?, ?)", UUID.randomUUID(), assignmentId, piqueteId,
				OffsetDateTime.parse("2031-09-06T07:00:00Z"));

		// the scheduled refresh, every 15 s, without calling it
		await().atMost(Duration.ofSeconds(30))
				.untilAsserted(() -> assertThat(gauge(WaitingChecksMetrics.ASSIGNMENT)).isEqualTo(before + 1));
		assertThat(prometheus.scrape())
				.contains("escala_checks_waiting{application=\"escala-api\",purpose=\"ASSIGNMENT\"}");

		sendAvaliacaoConcluida(assignmentId);
		await().atMost(Duration.ofSeconds(20)).until(() -> !"PENDING".equals(
				jdbc.queryForObject("SELECT status FROM slot WHERE assignment_id = ?", String.class, assignmentId)));
		assertThat(refreshed(WaitingChecksMetrics.ASSIGNMENT)).isEqualTo(before);
	}

	@Test
	@DisplayName("TC-SCRUM-98-02 a \"who can come\" check still PENDING counts as CANDIDATE until its AvaliacaoConcluida arrives")
	void candidate() {
		long before = refreshed(WaitingChecksMetrics.CANDIDATE);
		UUID checkId = UUID.randomUUID();
		jdbc.update("INSERT INTO candidate_check (id, request_id, member_id, role, starts_at, ends_at, evaluation_state, "
				+ "created_at) VALUES (?, ?, ?, 'DRIVER', ?, ?, 'PENDING', ?)", checkId, UUID.randomUUID(),
				SYNTHETIC_MEMBER, OffsetDateTime.parse("2031-09-06T07:00:00Z"),
				OffsetDateTime.parse("2031-09-06T19:00:00Z"), OffsetDateTime.now(ZoneOffset.UTC));
		assertThat(refreshed(WaitingChecksMetrics.CANDIDATE)).isEqualTo(before + 1);

		sendAvaliacaoConcluida(checkId);
		await().atMost(Duration.ofSeconds(20)).untilAsserted(
				() -> assertThat(refreshed(WaitingChecksMetrics.CANDIDATE)).isEqualTo(before));
	}

	@Test
	@DisplayName("TC-SCRUM-98-03 a free-text message still PENDING counts as EXTRACTION until its ExtracaoConcluida arrives")
	void extraction() {
		long before = refreshed(WaitingChecksMetrics.EXTRACTION);
		UUID messageId = UUID.randomUUID();
		UUID requestEventId = UUID.randomUUID();
		jdbc.update("INSERT INTO unavailability_message (id, member_id, free_text, reference_date, status, "
				+ "request_event_id, created_at) VALUES (?, ?, ?, ?, 'PENDING', ?, ?)", messageId, SYNTHETIC_MEMBER,
				"Synthetic text: I can't come on Saturday.", LocalDate.of(2031, 9, 1), requestEventId,
				OffsetDateTime.now(ZoneOffset.UTC));
		assertThat(refreshed(WaitingChecksMetrics.EXTRACTION)).isEqualTo(before + 1);

		send("extracao.concluida", """
				{"eventId":"%s","type":"ExtracaoConcluida","schemaVersion":2,"occurredAt":"%s","traceId":"%s",
				 "requestEventId":"%s","unavailabilityRequestId":"%s","periods":[],"method":"FALLBACK",
				 "model":"synthetic-model","latencyMs":1200}
				""".formatted(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), requestEventId, messageId));
		await().atMost(Duration.ofSeconds(20)).untilAsserted(
				() -> assertThat(refreshed(WaitingChecksMetrics.EXTRACTION)).isEqualTo(before));
	}

	@Test
	@DisplayName("TC-SCRUM-98-04 every count of the gauge can be answered from its (state, date) index of V11, without reading the table")
	void countsUseIndexes() {
		String plan = jdbc.execute((ConnectionCallback<String>) connection -> {
			try (Statement statement = connection.createStatement()) {
				// the tables of a test are tiny, so the planner would read them whole; this shows the index can answer
				statement.execute("SET enable_seqscan = off");
				List<String> lines = new ArrayList<>();
				try (ResultSet rs = statement.executeQuery("EXPLAIN " + WaitingChecksMetrics.COUNTS)) {
					while (rs.next()) {
						lines.add(rs.getString(1));
					}
				}
				statement.execute("RESET enable_seqscan");
				return String.join("\n", lines);
			}
		});
		assertThat(plan).contains("slot_status_starts_idx", "assignment_evaluation_state_proposed_idx",
				"candidate_check_state_created_idx", "unavailability_message_status_created_idx")
				.doesNotContain("Seq Scan");
	}
}
