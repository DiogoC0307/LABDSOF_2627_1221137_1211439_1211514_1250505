package pt.isep.escala.elegibilidadeworker.avaliacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest
@Testcontainers
class AvaliacaoPedidaIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Test
	void sempreElegivelPublishesAvaliacaoConcluidaWithSharedTraceId() throws Exception {
		String traceId = UUID.randomUUID().toString();
		UUID eventId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		UUID slotId = UUID.randomUUID();
		String pedido = """
				{
				  "eventId": "%s",
				  "type": "AvaliacaoPedida",
				  "schemaVersion": 1,
				  "occurredAt": "%s",
				  "traceId": "%s",
				  "purpose": "ASSIGNMENT",
				  "assignmentId": "%s",
				  "slot": {
				    "slotId": "%s",
				    "start": "2026-11-02T00:00:00Z",
				    "end": "2026-11-03T00:00:00Z",
				    "requiredCertification": null,
				    "requiredLicenceClass": null
				  },
				  "candidate": {
				    "memberId": "00000000-0000-4000-8000-000000000001",
				    "inActiveRoster": true,
				    "withinAgeLimit": true,
				    "certifications": [],
				    "licenceClasses": [],
				    "unavailabilities": [],
				    "otherAssignments": []
				  }
				}
				""".formatted(eventId, Instant.now(), traceId, assignmentId, slotId);
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbitTemplate.send(
				"escala.events",
				"avaliacao.pedida",
				new Message(pedido.getBytes(StandardCharsets.UTF_8), properties));

		Message concluida = await().atMost(Duration.ofSeconds(10)).until(
				() -> rabbitTemplate.receive("api.avaliacao-concluida", 200), java.util.Objects::nonNull);
		JsonNode body = objectMapper.readTree(new String(concluida.getBody(), StandardCharsets.UTF_8));
		assertThat(body.path("type").asText()).isEqualTo("AvaliacaoConcluida");
		assertThat(body.path("traceId").asText()).isEqualTo(traceId);
		assertThat(body.path("requestEventId").asText()).isEqualTo(eventId.toString());
		assertThat(body.path("assignmentId").asText()).isEqualTo(assignmentId.toString());
		assertThat(body.path("result").asText()).isEqualTo("ELIGIBLE");
		assertThat(body.path("engineVersion").asText()).isEqualTo(Evaluation.ENGINE_SEMPRE_ELEGIVEL);
	}
}
