package pt.isep.escala.elegibilidadeworker.extracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.elegibilidadeworker.contract.Contracts;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * TC-SCRUM-96-14 and 15 through the broker: the worker as the consumer of ExtracaoPedida and the producer of
 * ExtracaoConcluida v2, against {@code contracts/events/}. AI off (the default), so the answer is FALLBACK; the shapes of
 * the other answers are checked by ExtracaoConcluidaContractTest.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@Testcontainers
class ExtracaoContractIT {

	private static final String EXTRACAO_PEDIDA = "extracao-pedida.schema.json";

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
	@DisplayName("TC-SCRUM-96-14 the ExtracaoConcluida the worker publishes matches the v2 schema and the envelope (producer)")
	void publishedExtracaoConcluidaMatchesTheSchema() {
		UUID eventId = UUID.randomUUID();
		ExtracaoPedidaIT.send(rabbitTemplate,
				ExtracaoPedidaIT.pedido(eventId, UUID.randomUUID(), "contract-96-14", "não posso sexta"));

		JsonNode answer = ExtracaoPedidaIT.awaitResult(rabbitTemplate, objectMapper, eventId, Duration.ofSeconds(20));
		String json = objectMapper.writeValueAsString(answer);
		assertThat(Contracts.eventErrors("extracao-concluida.schema.json", json)).as(json).isEmpty();
	}

	@Test
	@DisplayName("TC-SCRUM-96-15 the ExtracaoPedida the tests feed to the worker match the schema, and a schema-valid one is answered (consumer)")
	void consumedExtracaoPedidaMatchesTheSchema() {
		String fixture = ExtracaoPedidaIT.pedido(UUID.randomUUID(), UUID.randomUUID(), "contract-96-15", "não posso sexta");
		assertThat(Contracts.eventErrors(EXTRACAO_PEDIDA, fixture)).as(fixture).isEmpty();

		UUID eventId = UUID.randomUUID();
		String request = """
				{"eventId":"%s","type":"ExtracaoPedida","schemaVersion":1,"occurredAt":"%s","traceId":"contract-96-15",
				 "unavailabilityRequestId":"%s","freeText":"de 7 a 9 de dezembro estou de férias","referenceDate":"2026-11-23"}
				""".formatted(eventId, Instant.now(), UUID.randomUUID());
		assertThat(Contracts.eventErrors(EXTRACAO_PEDIDA, request)).isEmpty();

		ExtracaoPedidaIT.send(rabbitTemplate, request);
		JsonNode answer = ExtracaoPedidaIT.awaitResult(rabbitTemplate, objectMapper, eventId, Duration.ofSeconds(20));
		assertThat(answer.path("type").asString()).isEqualTo("ExtracaoConcluida");
	}
}
