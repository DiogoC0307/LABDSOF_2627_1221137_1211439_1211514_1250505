package pt.isep.escala.escalaapi.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

// What someone could set in a ConfigMap: everything at DEBUG, SQL bind values at TRACE, Spring AMQP at DEBUG.
@SpringBootTest(properties = {
		"spring.rabbitmq.dynamic=true",
		"logging.level.root=DEBUG",
		"logging.level.org.hibernate.orm.jdbc.bind=TRACE",
		"logging.level.org.springframework.amqp=DEBUG"})
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class LogLeakVerboseLevelsIT {

	// No log line of the walking skeleton prints the duty date, so it stands for a value bound into SQL.
	static final String DUTY_DATE_MARKER = "2031-07-29";

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

	@Test
	@DisplayName("TC-SCRUM-94-03 with root DEBUG and SQL binds at TRACE, request and SQL values are not logged")
	void sqlAndRequestValuesAreNotLogged(CapturedOutput output) throws Exception {
		mockMvc.perform(post("/api/v1/piquetes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dutyDate\":\"" + DUTY_DATE_MARKER + "\"}"))
				.andExpect(status().isCreated());

		await().atMost(Duration.ofSeconds(15)).until(() -> output.getAll().contains("outbox published"));
		assertThat(output.getAll()).doesNotContain(DUTY_DATE_MARKER);
	}

	@Test
	@DisplayName("TC-SCRUM-94-04 with Spring AMQP at DEBUG, a message body is not logged")
	void amqpDebugDoesNotLogMessageBodies(CapturedOutput output) {
		String marker = LogLeakSupport.marker();
		LogLeakSupport.send(rabbitTemplate, "avaliacao.concluida", LogLeakSupport.shortInvalidEvent(marker));

		LogLeakSupport.awaitListenerFailure(output);
		assertThat(output.getAll()).doesNotContain(marker);
	}
}
