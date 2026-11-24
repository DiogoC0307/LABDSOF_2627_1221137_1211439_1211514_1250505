package pt.isep.escala.escalaapi.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * TC-SCRUM-97-05 for escala-api: the outbox gauge counts the rows not published yet. The publisher is replaced by a
 * mock, so the rows stay in the outbox.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@Testcontainers
class ApiOutboxPendingMetricIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@MockitoBean
	OutboxPublisher publisher;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MeterRegistry registry;

	@Test
	@DisplayName("TC-SCRUM-97-05 escala.outbox.pending counts the rows not published yet, 0 before any")
	void countsPendingRows() {
		assertThat(registry.get("escala.outbox.pending").gauge().value()).isZero();
		insert(null);
		insert(null);
		insert(OffsetDateTime.now());

		await().atMost(Duration.ofSeconds(40)).untilAsserted(() -> {
			Gauge gauge = registry.find("escala.outbox.pending").gauge();
			assertThat(gauge).isNotNull();
			assertThat(gauge.value()).isEqualTo(2.0);
		});
	}

	private void insert(OffsetDateTime publishedAt) {
		jdbc.update("insert into outbox_event (id, created_at, payload, published_at, routing_key) values (?, ?, ?, ?, ?)",
				UUID.randomUUID(), OffsetDateTime.now(), "{}", publishedAt, "avaliacao.pedida");
	}
}
