package pt.isep.escala.elegibilidadeworker.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

// The levels of dev: root INFO, the team's own packages at DEBUG.
@SpringBootTest(properties = {
		"spring.rabbitmq.dynamic=true",
		"logging.level.root=INFO",
		"logging.level.pt.isep.escala=DEBUG"})
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class LogLeakIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Test
	@DisplayName("TC-SCRUM-94-01 a processed message does not log its payload")
	void processedMessageDoesNotLogItsPayload(CapturedOutput output) {
		String marker = LogLeakSupport.marker();
		UUID eventId = UUID.randomUUID();
		String pedido = """
				{"eventId":"%s","type":"AvaliacaoPedida","schemaVersion":1,"occurredAt":"%s","traceId":"%s",
				 "purpose":"ASSIGNMENT","assignmentId":"%s",
				 "slot":{"slotId":"%s","start":"2026-11-02T00:00:00Z","end":"2026-11-03T00:00:00Z",
				         "requiredCertification":null,"requiredLicenceClass":null},
				 "candidate":{"memberId":"00000000-0000-4000-8000-000000000001","inActiveRoster":true,
				              "withinAgeLimit":true,"certifications":[],"licenceClasses":[],"unavailabilities":[],
				              "otherAssignments":[]},
				 "note":"%s"}
				""".formatted(eventId, Instant.now(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), marker);
		LogLeakSupport.send(rabbitTemplate, "avaliacao.pedida", pedido);

		await().atMost(Duration.ofSeconds(15))
				.until(() -> output.getAll().contains("SEMPRE_ELEGIVEL written requestEventId=" + eventId));
		assertThat(output.getAll()).doesNotContain(marker);
	}

	@Test
	@DisplayName("TC-SCRUM-94-02 a message the listener cannot process does not log its payload")
	void failedMessageDoesNotLogItsPayload(CapturedOutput output) {
		String marker = LogLeakSupport.marker();
		LogLeakSupport.send(rabbitTemplate, "avaliacao.pedida", LogLeakSupport.shortInvalidEvent(marker));

		LogLeakSupport.awaitListenerFailure(output);
		assertThat(output.getAll()).doesNotContain(marker);
	}
}
