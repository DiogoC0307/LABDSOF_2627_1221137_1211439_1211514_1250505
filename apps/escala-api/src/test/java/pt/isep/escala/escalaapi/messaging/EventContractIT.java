package pt.isep.escala.escalaapi.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;
import pt.isep.escala.escalaapi.contract.Contracts;

/**
 * TC-SCRUM-96-07 and 08: the events of escala-api against {@code contracts/events/}, as the producer of
 * AvaliacaoPedida and as the consumer of AvaliacaoConcluida.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
class EventContractIT {

	private static final String AVALIACAO_CONCLUIDA = "avaliacao-concluida.schema.json";

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

	@Autowired
	JdbcTemplate jdbc;

	@Test
	@DisplayName("TC-SCRUM-96-07 the AvaliacaoPedida escala-api publishes for a new piquete matches its schema and the envelope (producer)")
	void publishedAvaliacaoPedidaMatchesTheSchema() throws Exception {
		mockMvc.perform(post("/api/v1/piquetes").header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor())
				.contentType(MediaType.APPLICATION_JSON).content("{\"dutyDate\":\"2026-12-03\"}"))
				.andExpect(status().isCreated());

		Message pedido = await().atMost(Duration.ofSeconds(15))
				.until(() -> rabbitTemplate.receive("worker.avaliacao-pedida", 200), Objects::nonNull);
		String json = new String(pedido.getBody(), StandardCharsets.UTF_8);
		assertThat(Contracts.eventErrors("avaliacao-pedida.schema.json", json)).as(json).isEmpty();
	}

	@Test
	@DisplayName("TC-SCRUM-96-08 the AvaliacaoConcluida the tests feed to escala-api match the schema, and a schema-valid one is applied (consumer)")
	void consumedAvaliacaoConcluidaMatchesTheSchema() {
		for (String result : new String[] { "ELIGIBLE", "INELIGIBLE", "FAILED" }) {
			String fixture = MessagingTestSupport.concluida(UUID.randomUUID(), UUID.randomUUID(), result);
			assertThat(Contracts.eventErrors(AVALIACAO_CONCLUIDA, fixture)).as(fixture).isEmpty();
		}

		UUID assignmentId = MessagingTestSupport.insertPendingSlot(jdbc);
		String answer = """
				{"eventId":"%s","type":"AvaliacaoConcluida","schemaVersion":1,"occurredAt":"%s",
				 "traceId":"contract-96-08","requestEventId":"%s","assignmentId":"%s","result":"FAILED",
				 "violatedRules":[],"engineVersion":"SEMPRE_ELEGIVEL"}
				""".formatted(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), assignmentId);
		assertThat(Contracts.eventErrors(AVALIACAO_CONCLUIDA, answer)).isEmpty();

		MessagingTestSupport.send(rabbitTemplate, "avaliacao.concluida", answer.getBytes(StandardCharsets.UTF_8));
		await().atMost(Duration.ofSeconds(15))
				.untilAsserted(() -> assertThat(MessagingTestSupport.slotStatus(jdbc, assignmentId))
						.isEqualTo("EVALUATION_FAILED"));
	}

	@Test
	@DisplayName("TC-SCRUM-96-08 a fixture with a renamed field breaks the AvaliacaoConcluida schema (the check is not vacuous)")
	void renamedFieldIsRejected() {
		String fixture = MessagingTestSupport.concluida(UUID.randomUUID(), UUID.randomUUID(), "ELIGIBLE")
				.replace("\"result\"", "\"outcome\"");
		assertThat(Contracts.eventErrors(AVALIACAO_CONCLUIDA, fixture)).isNotEmpty();
		String badUuid = MessagingTestSupport.concluida(UUID.randomUUID(), UUID.randomUUID(), "ELIGIBLE")
				.replaceFirst("\"eventId\":\"[^\"]+\"", "\"eventId\":\"not-a-uuid\"");
		assertThat(Contracts.eventErrors(AVALIACAO_CONCLUIDA, badUuid)).as("format uuid is asserted").isNotEmpty();
	}
}
