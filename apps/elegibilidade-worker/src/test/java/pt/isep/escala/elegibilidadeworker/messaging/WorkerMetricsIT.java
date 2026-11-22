package pt.isep.escala.elegibilidadeworker.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * TC-SCRUM-97-01, 06, 08, 09 and 10 for the worker, against a real PostgreSQL and RabbitMQ. Spring Boot leaves metrics
 * export off in tests unless {@link AutoConfigureMetrics} is there. The endpoint is exposed by a test property here; in
 * the application it is one line of application.properties (after SCRUM-88).
 */
@SpringBootTest(properties = { "spring.rabbitmq.dynamic=true",
		"management.endpoints.web.exposure.include=health,prometheus" })
@AutoConfigureMockMvc
@AutoConfigureMetrics
@Testcontainers
class WorkerMetricsIT {

	private static final Pattern UUID_PATTERN = Pattern
			.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	MeterRegistry registry;

	private String scrape() throws Exception {
		return mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("TC-SCRUM-97-01 the worker exposes its metrics on /actuator/prometheus, with the application tag")
	void exposesPrometheus() throws Exception {
		String scrape = scrape();
		assertThat(scrape).contains("escala_evaluation_processing_seconds", "escala_outbox_pending",
				"escala_messages_dead_lettered_total", "application=\"elegibilidade-worker\"");
	}

	@Test
	@DisplayName("TC-SCRUM-97-09 an evaluated AvaliacaoPedida counts as completed ELIGIBLE from the listener and is timed")
	void countsEvaluations() {
		double before = completed();
		long timedBefore = registry.get("escala.evaluation.processing").timer().count();
		UUID eventId = UUID.randomUUID();
		MessagingTestSupport.sendPedido(rabbitTemplate, eventId, UUID.randomUUID());
		MessagingTestSupport.awaitMessage(rabbitTemplate, MessagingTestSupport.RESULTS, Duration.ofSeconds(20));

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			assertThat(completed()).isEqualTo(before + 1);
			assertThat(registry.get("escala.evaluation.processing").timer().count()).isEqualTo(timedBefore + 1);
		});
	}

	private double completed() {
		Counter counter = registry.find("escala.evaluations.completed").tags("result", "ELIGIBLE", "source", "listener")
				.counter();
		return counter == null ? 0 : counter.count();
	}

	@Test
	@DisplayName("TC-SCRUM-97-06 a malformed message is counted as dead-lettered and shows in the DLQ gauge")
	void countsDeadLetters() {
		double before = registry.get("escala.messages.dead_lettered").tag("queue", "worker.avaliacao-pedida").counter()
				.count();
		MessagingTestSupport.send(rabbitTemplate, "avaliacao.pedida", "not json".getBytes(StandardCharsets.UTF_8));

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(
				registry.get("escala.messages.dead_lettered").tag("queue", "worker.avaliacao-pedida").counter().count())
				.isEqualTo(before + 1));
		// the gauge is refreshed every 15 s from the broker; the message is left in the DLQ
		await().atMost(Duration.ofSeconds(40)).untilAsserted(() -> {
			Gauge gauge = registry.find("escala.dlq.messages").tag("queue", "worker.avaliacao-pedida.dlq").gauge();
			assertThat(gauge).isNotNull();
			assertThat(gauge.value()).isGreaterThanOrEqualTo(1.0);
		});
	}

	@Test
	@DisplayName("TC-SCRUM-97-10 every metric the dashboard asks the worker for is in its /actuator/prometheus output")
	void dashboardMetricsExist() throws Exception {
		DashboardQueries dashboard = new DashboardQueries();
		assertThat(dashboard.selectorsWithoutApplication()).isEmpty();
		Set<String> asked = dashboard.metricsOf("elegibilidade-worker");
		assertThat(asked).isNotEmpty();
		// the DLQ gauge has its rows after the first refresh from the broker (every 15 s)
		await().atMost(Duration.ofSeconds(30))
				.untilAsserted(() -> assertThat(DashboardQueries.seriesNames(scrape())).containsAll(asked));
	}

	@Test
	@DisplayName("TC-SCRUM-97-08 after real traffic no label of the scrape holds an ID (threat model T-07)")
	void noIdsInLabels() throws Exception {
		UUID eventId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		MessagingTestSupport.sendPedido(rabbitTemplate, eventId, assignmentId);
		MessagingTestSupport.awaitMessage(rabbitTemplate, MessagingTestSupport.RESULTS, Duration.ofSeconds(20));

		String scrape = scrape();
		assertThat(scrape).doesNotContain(eventId.toString()).doesNotContain(assignmentId.toString());
		for (String line : scrape.split("\n")) {
			if (!line.startsWith("#")) {
				int labels = line.indexOf('{');
				String labelPart = labels < 0 ? "" : line.substring(labels, line.lastIndexOf('}') + 1);
				assertThat(UUID_PATTERN.matcher(labelPart).find()).as(line).isFalse();
			}
		}
	}
}
