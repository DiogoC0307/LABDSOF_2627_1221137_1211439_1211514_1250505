package pt.isep.escala.escalaapi.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * The service starts before the broker knows its user, as on 2026-11-12 in Compose: the broker accepted
 * connections 84 ms before setup.sh created the user. The listener must keep trying and consume once the user
 * exists, without a restart of the service.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@Testcontainers
class ListenerAuthenticationIT {

	static final String LATE_USER = "tc-scrum-95-06";
	// A throwaway password for a user that exists only in this test's broker.
	static final String LATE_PASSWORD = UUID.randomUUID().toString();

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	static CachingConnectionFactory connection;

	@DynamicPropertySource
	static void lateUser(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.host", rabbit::getHost);
		registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
		registry.add("spring.rabbitmq.username", () -> LATE_USER);
		registry.add("spring.rabbitmq.password", () -> LATE_PASSWORD);
	}

	@AfterAll
	static void closeBroker() {
		if (connection != null) {
			connection.destroy();
		}
	}

	@Autowired
	JdbcTemplate jdbc;

	@Test
	@DisplayName("TC-SCRUM-95-06 a listener that failed to log in consumes once its user exists, without a restart")
	void listenerConsumesOnceTheUserExists() throws Exception {
		// Let the listener fail to log in at least once.
		await().pollDelay(Duration.ofSeconds(3)).until(() -> true);
		rabbit.execInContainer("rabbitmqctl", "add_user", LATE_USER, LATE_PASSWORD);
		rabbit.execInContainer("rabbitmqctl", "set_permissions", "-p", "/", LATE_USER, ".*", ".*", ".*");

		connection = MessagingTestSupport.adminConnection(rabbit);
		RabbitAdmin admin = new RabbitAdmin(connection);
		await().atMost(Duration.ofSeconds(60)).until(() -> {
			QueueInformation info = admin.getQueueInfo(MessagingTestSupport.QUEUE);
			return info != null && info.getConsumerCount() > 0;
		});

		UUID assignmentId = MessagingTestSupport.insertPendingSlot(jdbc);
		MessagingTestSupport.sendConcluida(new RabbitTemplate(connection), UUID.randomUUID(), assignmentId, "ELIGIBLE");
		await().atMost(Duration.ofSeconds(15))
				.until(() -> MessagingTestSupport.slotStatus(jdbc, assignmentId).equals("ELIGIBLE"));
		assertThat(MessagingTestSupport.slotStatus(jdbc, assignmentId)).isEqualTo("ELIGIBLE");
	}
}
