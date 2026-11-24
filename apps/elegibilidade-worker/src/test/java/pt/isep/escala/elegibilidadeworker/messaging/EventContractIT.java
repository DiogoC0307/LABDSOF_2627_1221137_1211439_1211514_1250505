package pt.isep.escala.elegibilidadeworker.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.elegibilidadeworker.contract.Contracts;
import tools.jackson.databind.ObjectMapper;

/**
 * TC-SCRUM-96-10 and 11: the events of the worker against {@code contracts/events/}, as the consumer of AvaliacaoPedida
 * and as the producer of AvaliacaoConcluida.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@Testcontainers
class EventContractIT {

	private static final String AVALIACAO_PEDIDA = "avaliacao-pedida.schema.json";

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
	@DisplayName("TC-SCRUM-96-10 the AvaliacaoConcluida the worker publishes matches its schema and the envelope, and answers the request (producer)")
	void publishedAvaliacaoConcluidaMatchesTheSchema() {
		UUID requestEventId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		MessagingTestSupport.sendPedido(rabbitTemplate, requestEventId, assignmentId);

		Message answer = MessagingTestSupport.awaitMessage(rabbitTemplate, MessagingTestSupport.RESULTS,
				Duration.ofSeconds(20));
		String json = new String(answer.getBody(), StandardCharsets.UTF_8);
		assertThat(Contracts.eventErrors("avaliacao-concluida.schema.json", json)).as(json).isEmpty();
		assertThat(objectMapper.readTree(json).path("requestEventId").asString()).isEqualTo(requestEventId.toString());
	}

	@Test
	@DisplayName("TC-SCRUM-96-11 the AvaliacaoPedida the tests feed to the worker match the schema, and a full schema-valid one is evaluated (consumer)")
	void consumedAvaliacaoPedidaMatchesTheSchema() {
		String fixture = MessagingTestSupport.pedido(UUID.randomUUID(), UUID.randomUUID());
		assertThat(Contracts.eventErrors(AVALIACAO_PEDIDA, fixture)).as(fixture).isEmpty();

		UUID requestEventId = UUID.randomUUID();
		String full = """
				{"eventId":"%s","type":"AvaliacaoPedida","schemaVersion":1,"occurredAt":"%s","traceId":"contract-96-11",
				 "purpose":"SWAP","assignmentId":"%s",
				 "slot":{"slotId":"%s","start":"2031-08-01T08:00:00Z","end":"2031-08-01T20:00:00Z",
				         "requiredCertification":"TAS","requiredLicenceClass":"C"},
				 "candidate":{"memberId":"%s","inActiveRoster":true,"withinAgeLimit":true,
				              "certifications":[{"type":"TAS","expiresOn":"2032-03-31"}],"licenceClasses":["C"],
				              "unavailabilities":[{"start":"2031-07-01T00:00:00Z","end":"2031-07-03T00:00:00Z"}],
				              "otherAssignments":[{"start":"2031-08-03T08:00:00Z","end":"2031-08-03T20:00:00Z"}]}}
				""".formatted(requestEventId, Instant.now(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		assertThat(Contracts.eventErrors(AVALIACAO_PEDIDA, full)).isEmpty();

		MessagingTestSupport.send(rabbitTemplate, "avaliacao.pedida", full.getBytes(StandardCharsets.UTF_8));
		Message answer = MessagingTestSupport.awaitMessage(rabbitTemplate, MessagingTestSupport.RESULTS,
				Duration.ofSeconds(20));
		assertThat(objectMapper.readTree(answer.getBody()).path("requestEventId").asString())
				.isEqualTo(requestEventId.toString());
	}
}
