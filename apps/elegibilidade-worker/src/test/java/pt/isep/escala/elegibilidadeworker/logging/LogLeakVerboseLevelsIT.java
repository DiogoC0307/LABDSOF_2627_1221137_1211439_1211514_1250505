package pt.isep.escala.elegibilidadeworker.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

// What someone could set in a ConfigMap: everything at DEBUG and Spring AMQP at DEBUG.
@SpringBootTest(properties = {
		"spring.rabbitmq.dynamic=true",
		"logging.level.root=DEBUG",
		"logging.level.org.springframework.amqp=DEBUG"})
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class LogLeakVerboseLevelsIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Test
	@DisplayName("TC-SCRUM-94-04 with Spring AMQP at DEBUG, a message body is not logged")
	void amqpDebugDoesNotLogMessageBodies(CapturedOutput output) {
		String marker = LogLeakSupport.marker();
		LogLeakSupport.send(rabbitTemplate, "avaliacao.pedida", LogLeakSupport.shortInvalidEvent(marker));

		LogLeakSupport.awaitListenerFailure(output);
		assertThat(output.getAll()).doesNotContain(marker);
	}
}
