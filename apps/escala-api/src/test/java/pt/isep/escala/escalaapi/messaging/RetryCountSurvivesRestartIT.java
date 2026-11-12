package pt.isep.escala.escalaapi.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pt.isep.escala.escalaapi.messaging.MessagingTestSupport.DLQ;
import static pt.isep.escala.escalaapi.messaging.MessagingTestSupport.QUEUE;
import static pt.isep.escala.escalaapi.messaging.MessagingTestSupport.RETRY_COUNT_HEADER;

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
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.EscalaApiApplication;

/**
 * TC-01 starts escala-api itself, twice, so the service can be stopped while a failed message waits in a retry
 * queue. The retry count must be carried by the message, not kept in the service.
 */
@Testcontainers
class RetryCountSurvivesRestartIT {

	@Container
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	static CachingConnectionFactory connection;
	static RabbitAdmin admin;
	static RabbitTemplate rabbitTemplate;

	@BeforeAll
	static void broker() {
		connection = MessagingTestSupport.adminConnection(rabbit);
		admin = new RabbitAdmin(connection);
		rabbitTemplate = new RabbitTemplate(connection);
		MessagingTestSupport.declareShortRetryQueues(admin, QUEUE);
	}

	@AfterAll
	static void closeBroker() {
		connection.destroy();
	}

	static ConfigurableApplicationContext startService() {
		// Command-line arguments, so they win over application.properties and the environment.
		return new SpringApplicationBuilder(EscalaApiApplication.class).run(
						"--server.port=0",
						"--spring.datasource.url=" + postgres.getJdbcUrl(),
						"--spring.datasource.username=" + postgres.getUsername(),
						"--spring.datasource.password=" + postgres.getPassword(),
						"--spring.rabbitmq.host=" + rabbit.getHost(),
						"--spring.rabbitmq.port=" + rabbit.getAmqpPort(),
						"--spring.rabbitmq.username=" + rabbit.getAdminUsername(),
						"--spring.rabbitmq.password=" + rabbit.getAdminPassword(),
						"--spring.rabbitmq.dynamic=true",
						"--escala.messaging.retry-stages=" + MessagingTestSupport.TEST_RETRY_STAGES);
	}

	@Test
	@DisplayName("TC-SCRUM-95-01 a failing message is retried through the broker's retry queues, keeps its count across a restart of the service, then lands in the DLQ")
	void failingMessageIsRetriedAcrossARestartThenDeadLettered() {
		ConfigurableApplicationContext service = startService();
		try {
			JdbcTemplate jdbc = MessagingTestSupport.jdbc(postgres);
			UUID assignmentId = MessagingTestSupport.insertPendingSlot(jdbc);
			// The database refuses to make this slot ELIGIBLE, so every delivery of the result fails.
			jdbc.execute("ALTER TABLE slot ADD CONSTRAINT tc_scrum_95_01 CHECK (assignment_id <> '" + assignmentId
					+ "'::uuid OR status <> 'ELIGIBLE')");
			MessagingTestSupport.sendConcluida(rabbitTemplate, UUID.randomUUID(), assignmentId, "ELIGIBLE");

			// After two failed deliveries the message waits in the second retry queue: stop the service there.
			await().atMost(Duration.ofSeconds(15))
					.until(() -> MessagingTestSupport.messageCount(admin, QUEUE + ".test-retry.2") == 1);
			service.close();

			service = startService();
			Message dead = MessagingTestSupport.awaitDeadLetter(rabbitTemplate, DLQ, Duration.ofSeconds(30));

			assertThat(dead.getMessageProperties().<Object>getHeader(RETRY_COUNT_HEADER)).hasToString("3");
			// Each retry queue held the message once: the count did not start again after the restart.
			assertThat(MessagingTestSupport.deathCount(dead, QUEUE + ".test-retry.1")).isEqualTo(1);
			assertThat(MessagingTestSupport.deathCount(dead, QUEUE + ".test-retry.2")).isEqualTo(1);
			assertThat(MessagingTestSupport.deathCount(dead, QUEUE + ".test-retry.3")).isEqualTo(1);
			assertThat(MessagingTestSupport.messageCount(admin, QUEUE)).isZero();
		} finally {
			service.close();
		}
	}
}
