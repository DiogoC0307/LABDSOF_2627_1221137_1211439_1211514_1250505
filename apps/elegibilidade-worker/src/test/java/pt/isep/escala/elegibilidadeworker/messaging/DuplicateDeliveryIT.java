package pt.isep.escala.elegibilidadeworker.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@Testcontainers
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
	@DisplayName("TC-SCRUM-95-02 the same AvaliacaoPedida delivered twice gives one evaluation and one AvaliacaoConcluida (RNF05)")
	void sameEventTwiceIsEvaluatedOnce() {
		UUID eventId = UUID.randomUUID();
		MessagingTestSupport.sendPedido(rabbitTemplate, eventId, UUID.randomUUID());
		MessagingTestSupport.sendPedido(rabbitTemplate, eventId, UUID.randomUUID());

		Message first = MessagingTestSupport.awaitMessage(rabbitTemplate, MessagingTestSupport.RESULTS, Duration.ofSeconds(15));
		assertThat(new String(first.getBody(), StandardCharsets.UTF_8)).contains(eventId.toString());
		// Leave time for a second result, if the duplicate were evaluated again.
		await().pollDelay(Duration.ofSeconds(3)).until(() -> true);

		assertThat(rabbitTemplate.receive(MessagingTestSupport.RESULTS, 500)).isNull();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM evaluation WHERE request_event_id = ?", Integer.class, eventId))
				.isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_event WHERE event_id = ?", Integer.class, eventId))
				.isEqualTo(1);
	}
}
