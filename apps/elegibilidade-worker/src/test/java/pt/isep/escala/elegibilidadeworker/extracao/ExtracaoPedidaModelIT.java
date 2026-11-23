package pt.isep.escala.elegibilidadeworker.extracao;

import static org.assertj.core.api.Assertions.assertThat;
import static pt.isep.escala.elegibilidadeworker.extracao.ExtracaoPedidaIT.awaitResult;
import static pt.isep.escala.elegibilidadeworker.extracao.ExtracaoPedidaIT.pedido;
import static pt.isep.escala.elegibilidadeworker.extracao.ExtracaoPedidaIT.send;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.elegibilidadeworker.extracao.claude.AiExtractionConfiguration;
import pt.isep.escala.elegibilidadeworker.extracao.claude.FakeClaudeSupport;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * TC-SCRUM-75-27 and 28: the worker side of SCRUM-75 with AI on (as in dev and QA), with {@code FakeClaude} as the CLI
 * and a fake token, so nothing reaches Anthropic. The fake picks its answer from the message ("DORME": too slow,
 * "LONGE": a period out of the window, otherwise two periods).
 */
@SpringBootTest(properties = { "spring.rabbitmq.dynamic=true", "escala.ai.enabled=true", "escala.ai.timeout=3s" })
@Testcontainers
@Import(ExtracaoPedidaModelIT.FakeToken.class)
class ExtracaoPedidaModelIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@TestConfiguration
	static class FakeToken {
		@Bean
		AiExtractionConfiguration.ProcessEnvironment processEnvironment() {
			return () -> FakeClaudeSupport.environment(Map.of());
		}
	}

	@DynamicPropertySource
	static void fakeCli(DynamicPropertyRegistry registry) {
		List<String> command = FakeClaudeSupport.command("by-text",
				Path.of(System.getProperty("java.io.tmpdir"), "fake-claude-75-" + UUID.randomUUID() + ".properties"));
		for (int i = 0; i < command.size(); i++) {
			int index = i;
			registry.add("escala.ai.cli-command[" + i + "]", () -> command.get(index));
		}
	}

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Test
	@DisplayName("TC-SCRUM-75-27 with AI on the model's periods are published as MODEL, with the model and the latency")
	void modelAnswer() {
		UUID eventId = UUID.randomUUID();
		send(rabbitTemplate, pedido(eventId, UUID.randomUUID(), "trace-75-27", "não posso aos domingos de manhã"));

		JsonNode result = awaitResult(rabbitTemplate, objectMapper, eventId, Duration.ofSeconds(30));
		assertThat(result.get("method").asText()).isEqualTo("MODEL");
		assertThat(result.get("model").asText()).isEqualTo("claude-haiku-4-5-20251001");
		assertThat(result.get("latencyMs").asLong()).isNotNegative();
		assertThat(result.get("periods")).hasSize(2);
		JsonNode sundays = result.get("periods").get(0);
		assertThat(sundays.get("startDate").asText()).isEqualTo("2026-10-26");
		assertThat(sundays.get("weekdays").get(0).asText()).isEqualTo("SUN");
		assertThat(sundays.get("startTime").asText()).isEqualTo("08:00");
	}

	@Test
	@DisplayName("TC-SCRUM-75-28 a timeout or an error is FALLBACK; a period out of the window empties the whole MODEL proposal")
	void fallbackAndWindow() {
		UUID slow = UUID.randomUUID();
		send(rabbitTemplate, pedido(slow, UUID.randomUUID(), "trace-75-28a", "DORME não posso sexta"));
		JsonNode timedOut = awaitResult(rabbitTemplate, objectMapper, slow, Duration.ofSeconds(30));
		assertThat(timedOut.get("method").asText()).isEqualTo("FALLBACK");
		assertThat(timedOut.get("periods")).isEmpty();
		assertThat(timedOut.get("model").isNull()).isTrue();

		UUID failing = UUID.randomUUID();
		send(rabbitTemplate, pedido(failing, UUID.randomUUID(), "trace-75-28b", "ERRO não posso sexta"));
		assertThat(awaitResult(rabbitTemplate, objectMapper, failing, Duration.ofSeconds(30)).get("method").asText())
				.isEqualTo("FALLBACK");

		UUID far = UUID.randomUUID();
		send(rabbitTemplate, pedido(far, UUID.randomUUID(), "trace-75-28c", "LONGE não posso sexta"));
		JsonNode outOfWindow = awaitResult(rabbitTemplate, objectMapper, far, Duration.ofSeconds(30));
		assertThat(outOfWindow.get("method").asText()).isEqualTo("MODEL");
		assertThat(outOfWindow.get("periods")).isEmpty();
	}
}
