package pt.isep.escala.elegibilidadeworker.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * When the worker cannot evaluate a request after the retries, it answers FAILED, so escala-api can show the
 * evaluation as failed instead of pending for ever (RF24).
 */
@SpringBootTest(properties = {
		"spring.rabbitmq.dynamic=true",
		"escala.messaging.retry-stages=" + MessagingTestSupport.TEST_RETRY_STAGES})
@Testcontainers
class EvaluationFailedIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	static CachingConnectionFactory connection;

	@BeforeAll
	static void shortRetryQueues() {
		connection = MessagingTestSupport.adminConnection(rabbit);
		MessagingTestSupport.declareShortRetryQueues(new RabbitAdmin(connection), MessagingTestSupport.QUEUE);
	}

	@AfterAll
	static void closeBroker() {
		connection.destroy();
	}

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ObjectMapper objectMapper;

	@Test
	@DisplayName("TC-SCRUM-95-03 a request the worker cannot evaluate after the retries is answered FAILED, never ELIGIBLE")
	void exhaustedRetriesAnswerFailed() throws Exception {
		UUID eventId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		jdbc.execute("ALTER TABLE evaluation ADD CONSTRAINT tc_scrum_95_03 CHECK (assignment_id <> '" + assignmentId + "'::uuid)");
		try {
			MessagingTestSupport.sendPedido(rabbitTemplate, eventId, assignmentId);

			Message answer = MessagingTestSupport.awaitMessage(rabbitTemplate, MessagingTestSupport.RESULTS, Duration.ofSeconds(30));
			JsonNode body = objectMapper.readTree(new String(answer.getBody(), StandardCharsets.UTF_8));
			assertThat(body.path("type").asText()).isEqualTo("AvaliacaoConcluida");
			assertThat(body.path("result").asText()).isEqualTo("FAILED");
			assertThat(body.path("requestEventId").asText()).isEqualTo(eventId.toString());
			assertThat(body.path("assignmentId").asText()).isEqualTo(assignmentId.toString());

			Message dead = MessagingTestSupport.awaitDeadLetter(rabbitTemplate, MessagingTestSupport.DLQ, Duration.ofSeconds(10));
			assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).contains(eventId.toString());
			await().pollDelay(Duration.ofSeconds(2)).until(() -> true);
			assertThat(rabbitTemplate.receive(MessagingTestSupport.RESULTS, 500)).as("a second answer").isNull();
			assertThat(jdbc.queryForObject("SELECT count(*) FROM evaluation WHERE request_event_id = ?", Integer.class, eventId))
					.isZero();
		} finally {
			jdbc.execute("ALTER TABLE evaluation DROP CONSTRAINT tc_scrum_95_03");
		}
	}
}
