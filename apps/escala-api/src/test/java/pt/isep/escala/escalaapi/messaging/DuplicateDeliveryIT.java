package pt.isep.escala.escalaapi.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class DuplicateDeliveryIT {

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

	@Test
	@DisplayName("TC-SCRUM-95-02 the same AvaliacaoConcluida delivered twice is applied once (RNF05)")
	void sameEventTwiceIsAppliedOnce(CapturedOutput output) {
		UUID assignmentId = MessagingTestSupport.insertPendingSlot(jdbc);
		UUID eventId = UUID.randomUUID();
		MessagingTestSupport.sendConcluida(rabbitTemplate, eventId, assignmentId, "ELIGIBLE");
		MessagingTestSupport.sendConcluida(rabbitTemplate, eventId, assignmentId, "ELIGIBLE");

		await().atMost(Duration.ofSeconds(15)).until(() -> output.getAll().contains("duplicate ignored eventId=" + eventId));
		assertThat(MessagingTestSupport.occurrences(output.getAll(), "AvaliacaoConcluida applied assignmentId=" + assignmentId))
				.isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_event WHERE event_id = ?", Integer.class, eventId))
				.isEqualTo(1);
		assertThat(MessagingTestSupport.slotStatus(jdbc, assignmentId)).isEqualTo("ELIGIBLE");
	}
}
