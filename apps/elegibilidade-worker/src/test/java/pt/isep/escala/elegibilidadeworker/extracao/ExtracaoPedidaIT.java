package pt.isep.escala.elegibilidadeworker.extracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * TC-SCRUM-75-23 to 26: the worker side of SCRUM-75 with AI off (the default, as in prod and in the evaluation
 * profile), against a real PostgreSQL and RabbitMQ. With the levels of dev, as in TC-SCRUM-94-01.
 */
@SpringBootTest(properties = { "spring.rabbitmq.dynamic=true", "logging.level.root=INFO",
		"logging.level.pt.isep.escala=DEBUG" })
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class ExtracaoPedidaIT {

	static final String RESULTS = "api.extracao-concluida";
	static final String DLQ = "worker.extracao-pedida.dlq";

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

	@Autowired
	ObjectMapper objectMapper;

	static String pedido(UUID eventId, UUID requestId, String traceId, String freeText) {
		return """
				{"eventId":"%s","type":"ExtracaoPedida","schemaVersion":1,"occurredAt":"%s","traceId":"%s",
				 "unavailabilityRequestId":"%s","freeText":"%s","referenceDate":"2026-10-26"}
				""".formatted(eventId, Instant.now(), traceId, requestId, freeText);
	}

	static void send(RabbitTemplate rabbit, String body) {
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbit.send("escala.events", "extracao.pedida", new Message(body.getBytes(StandardCharsets.UTF_8), properties));
	}

	/** The next ExtracaoConcluida that answers {@code requestEventId}. */
	static JsonNode awaitResult(RabbitTemplate rabbit, ObjectMapper mapper, UUID requestEventId, Duration atMost) {
		return await().atMost(atMost).ignoreExceptions().until(() -> {
			Message message = rabbit.receive(RESULTS, 200);
			return message == null ? null : mapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
		}, json -> json != null && json.path("requestEventId").asText().equals(requestEventId.toString()));
	}

	@Test
	@DisplayName("TC-SCRUM-75-23 with AI off an ExtracaoPedida is answered by ExtracaoConcluida v2 with FALLBACK, no periods, through the outbox")
	void aiOffAnswersFallback() {
		UUID eventId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		send(rabbitTemplate, pedido(eventId, requestId, "trace-75-23", "não posso sexta"));

		JsonNode result = awaitResult(rabbitTemplate, objectMapper, eventId, Duration.ofSeconds(20));
		assertThat(result.get("type").asText()).isEqualTo("ExtracaoConcluida");
		assertThat(result.get("schemaVersion").asInt()).isEqualTo(2);
		assertThat(result.get("unavailabilityRequestId").asText()).isEqualTo(requestId.toString());
		assertThat(result.get("traceId").asText()).isEqualTo("trace-75-23");
		assertThat(result.get("method").asText()).isEqualTo("FALLBACK");
		assertThat(result.get("periods")).isEmpty();
		assertThat(result.get("model").isNull()).isTrue();
		assertThat(jdbc.queryForObject("select count(*) from outbox_event where payload like ?", Long.class,
				"%" + eventId + "%")).isEqualTo(1L);
	}

	@Test
	@DisplayName("TC-SCRUM-75-24 the same ExtracaoPedida delivered twice gives one ExtracaoConcluida (RNF05)")
	void duplicateAnsweredOnce() {
		UUID eventId = UUID.randomUUID();
		String body = pedido(eventId, UUID.randomUUID(), "trace-75-24", "não posso sábado");
		send(rabbitTemplate, body);
		send(rabbitTemplate, body);

		awaitResult(rabbitTemplate, objectMapper, eventId, Duration.ofSeconds(20));
		await().pollDelay(Duration.ofSeconds(3)).until(() -> true);
		assertThat(jdbc.queryForObject("select count(*) from outbox_event where payload like ?", Long.class,
				"%" + eventId + "%")).isEqualTo(1L);
		assertThat(jdbc.queryForObject("select count(*) from processed_event where event_id = ?", Long.class,
				eventId)).isEqualTo(1L);
	}

	@Test
	@DisplayName("TC-SCRUM-75-25 the free text is never stored in the worker's database and never logged")
	void freeTextNeverStoredNorLogged(CapturedOutput output) {
		String marker = "TEXTOLIVRE" + UUID.randomUUID().toString().substring(0, 8);
		UUID eventId = UUID.randomUUID();
		send(rabbitTemplate, pedido(eventId, UUID.randomUUID(), "trace-75-25", "tenho consulta terça " + marker));

		awaitResult(rabbitTemplate, objectMapper, eventId, Duration.ofSeconds(20));
		await().atMost(Duration.ofSeconds(10))
				.until(() -> output.getAll().contains("ExtracaoConcluida written requestEventId=" + eventId));
		assertThat(output.getAll()).doesNotContain(marker);
		List<String> tables = jdbc.queryForList(
				"select table_name from information_schema.tables where table_schema = 'public' and table_type = 'BASE TABLE'",
				String.class);
		assertThat(tables).isNotEmpty();
		for (String table : tables) {
			Long rows = jdbc.queryForObject(
					"select count(*) from \"" + table + "\" t where cast(t as text) like ?", Long.class, "%" + marker + "%");
			assertThat(rows).as("rows of %s holding the free text", table).isZero();
		}
	}

	@Test
	@DisplayName("TC-SCRUM-75-26 an ExtracaoPedida with no valid referenceDate goes to the DLQ at once and is not answered")
	void malformedGoesToDlq() {
		UUID eventId = UUID.randomUUID();
		send(rabbitTemplate, pedido(eventId, UUID.randomUUID(), "trace-75-26", "não posso sexta")
				.replace("\"referenceDate\":\"2026-10-26\"", "\"referenceDate\":\"amanhã\""));

		Message dead = await().atMost(Duration.ofSeconds(20)).ignoreExceptions()
				.until(() -> rabbitTemplate.receive(DLQ, 200), java.util.Objects::nonNull);
		assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).contains(eventId.toString());
		assertThat(jdbc.queryForObject("select count(*) from outbox_event where payload like ?", Long.class,
				"%" + eventId + "%")).isZero();
	}
}
