package pt.isep.escala.elegibilidadeworker.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pt.isep.escala.elegibilidadeworker.messaging.MessagingTestSupport.DLQ;
import static pt.isep.escala.elegibilidadeworker.messaging.MessagingTestSupport.QUEUE;
import static pt.isep.escala.elegibilidadeworker.messaging.MessagingTestSupport.RETRY_COUNT_HEADER;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(properties = {
		"spring.rabbitmq.dynamic=true",
		"escala.messaging.retry-stages=" + MessagingTestSupport.TEST_RETRY_STAGES})
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class MalformedMessageIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	static CachingConnectionFactory connection;
	static RabbitAdmin admin;

	@BeforeAll
	static void shortRetryQueues() {
		connection = MessagingTestSupport.adminConnection(rabbit);
		admin = new RabbitAdmin(connection);
		MessagingTestSupport.declareShortRetryQueues(admin, QUEUE);
	}

	@AfterAll
	static void closeBroker() {
		connection.destroy();
	}

	@Autowired
	RabbitTemplate rabbitTemplate;

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"not JSON at all", "{\"eventId\":\"not-a-uuid\",\"type\":\"AvaliacaoPedida\"}"})
	@DisplayName("TC-SCRUM-95-04 a malformed message goes to the DLQ at once, with one delivery, and never loops")
	void malformedMessageIsDeadLetteredAtOnce(String body, CapturedOutput output) {
		int logStart = output.getAll().length();
		MessagingTestSupport.send(rabbitTemplate, "avaliacao.pedida", body.getBytes(StandardCharsets.UTF_8));

		Message dead = MessagingTestSupport.awaitDeadLetter(rabbitTemplate, DLQ, Duration.ofSeconds(10));
		// Give a loop, if any, time to show up in the logs.
		await().pollDelay(Duration.ofSeconds(2)).until(() -> true);

		assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).isEqualTo(body);
		assertThat(dead.getMessageProperties().<Object>getHeader(RETRY_COUNT_HEADER)).isNull();
		assertThat(MessagingTestSupport.deathCount(dead, QUEUE)).isEqualTo(1);
		assertThat(MessagingTestSupport.occurrences(output.getAll().substring(logStart), "listener failed")).isEqualTo(1);
		assertThat(MessagingTestSupport.messageCount(admin, QUEUE)).isZero();
		assertThat(MessagingTestSupport.messageCount(admin, QUEUE + ".test-retry.1")).isZero();
	}
}
