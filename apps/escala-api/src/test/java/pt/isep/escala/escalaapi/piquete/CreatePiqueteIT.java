package pt.isep.escala.escalaapi.piquete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
class CreatePiqueteIT {

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
	ObjectMapper objectMapper;

	@Test
	void postWithoutDutyDateIsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/piquetes").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void postCreatesPiqueteWithPendingSlotThenEligibleAfterAvaliacaoConcluida() throws Exception {
		String traceId = UUID.randomUUID().toString();
		mockMvc.perform(post("/api/v1/piquetes")
						.contentType(MediaType.APPLICATION_JSON)
						.header("X-Trace-Id", traceId)
						.content("{\"dutyDate\":\"2026-11-02\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/v1/piquetes/")))
				.andExpect(header().string("X-Trace-Id", traceId))
				.andExpect(jsonPath("$.dutyDate").value("2026-11-02"))
				.andExpect(jsonPath("$.slots.length()").value(1))
				.andExpect(jsonPath("$.slots[0].status").value("PENDING"));

		Message pedido = await().atMost(Duration.ofSeconds(10)).until(
				() -> rabbitTemplate.receive("worker.avaliacao-pedida", 200), java.util.Objects::nonNull);
		JsonNode body = objectMapper.readTree(new String(pedido.getBody(), StandardCharsets.UTF_8));
		assertThat(body.path("type").asText()).isEqualTo("AvaliacaoPedida");
		assertThat(body.path("traceId").asText()).isEqualTo(traceId);
		assertThat(body.path("assignmentId").asText()).isNotBlank();
		assertThat(body.path("candidate").path("memberId").asText())
				.isEqualTo(AvaliacaoPedidaFactory.SKELETON_MEMBER_ID.toString());

		String concluida = """
				{
				  "eventId": "%s",
				  "type": "AvaliacaoConcluida",
				  "schemaVersion": 1,
				  "occurredAt": "%s",
				  "traceId": "%s",
				  "requestEventId": "%s",
				  "assignmentId": "%s",
				  "result": "ELIGIBLE",
				  "violatedRules": [],
				  "engineVersion": "SEMPRE_ELEGIVEL"
				}
				""".formatted(
						UUID.randomUUID(),
						Instant.now(),
						traceId,
						body.path("eventId").asText(),
						body.path("assignmentId").asText());
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbitTemplate.send(
				"escala.events",
				"avaliacao.concluida",
				new Message(concluida.getBytes(StandardCharsets.UTF_8), properties));

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> mockMvc.perform(get("/api/v1/piquetes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].slots[0].status").value("ELIGIBLE")));
	}
}
