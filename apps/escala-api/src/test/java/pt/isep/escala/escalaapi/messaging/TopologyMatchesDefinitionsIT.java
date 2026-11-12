package pt.isep.escala.escalaapi.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * The broker topology lives in infra/rabbitmq/definitions.json (imported in Compose and in the three clusters'
 * ConfigMaps) and is declared again in code, which the tests use (Compose and the clusters set SPRING_RABBITMQ_DYNAMIC=false). Both must say the same, and the retry
 * delays (5 s, 30 s, 2 min) are the TTLs of the retry queues there.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@Testcontainers
class TopologyMatchesDefinitionsIT {

	static final Path REPO = Path.of("..", "..");
	static final ObjectMapper JSON = JsonMapper.builder().build();

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	ApplicationContext context;

	@Autowired
	RabbitListenerEndpointRegistry listeners;

	@Autowired
	Environment environment;

	@Test
	@DisplayName("TC-SCRUM-95-05 every queue, exchange and binding declared in code is in definitions.json with the same arguments")
	void codeMatchesDefinitions() throws IOException {
		JsonNode definitions = JSON.readTree(Files.readString(REPO.resolve("infra/rabbitmq/definitions.json")));

		for (Queue queue : declared(Queue.class)) {
			JsonNode defined = named(definitions.path("queues"), queue.getName());
			assertThat(defined != null).as("queue %s in definitions.json", queue.getName()).isTrue();
			assertThat(queue.isDurable()).as("durable %s", queue.getName()).isEqualTo(defined.path("durable").asBoolean());
			assertThat((JsonNode) JSON.valueToTree(queue.getArguments())).as("arguments of %s", queue.getName())
					.isEqualTo(defined.path("arguments"));
		}
		for (Exchange exchange : declared(Exchange.class)) {
			JsonNode defined = named(definitions.path("exchanges"), exchange.getName());
			assertThat(defined != null).as("exchange %s in definitions.json", exchange.getName()).isTrue();
			assertThat(exchange.getType()).isEqualTo(defined.path("type").asText());
			assertThat(exchange.isDurable()).isEqualTo(defined.path("durable").asBoolean());
		}
		for (Binding binding : declared(Binding.class)) {
			boolean found = false;
			for (JsonNode defined : definitions.path("bindings")) {
				found |= defined.path("source").asText().equals(binding.getExchange())
						&& defined.path("destination").asText().equals(binding.getDestination())
						&& defined.path("routing_key").asText().equals(binding.getRoutingKey());
			}
			assertThat(found).as("binding %s -> %s (%s) in definitions.json",
					binding.getExchange(), binding.getDestination(), binding.getRoutingKey()).isTrue();
		}
	}

	@Test
	@DisplayName("TC-SCRUM-95-05 each queue this service consumes has, in code, its DLQ and the retry queues of 5 s, 30 s and 2 min")
	void consumedQueuesHaveRetryQueuesAndDlq() {
		assertThat(environment.getProperty("escala.messaging.retry-stages")).isEqualTo("retry.5s,retry.30s,retry.2m");
		List<String> consumed = new ArrayList<>();
		listeners.getListenerContainers().forEach(container ->
				consumed.addAll(List.of(((AbstractMessageListenerContainer) container).getQueueNames())));
		assertThat(consumed).isNotEmpty();

		for (String name : consumed) {
			Queue main = queue(name);
			assertThat(main.getArguments()).containsEntry("x-dead-letter-exchange", "escala.dlx")
					.containsEntry("x-dead-letter-routing-key", name);
			assertThat(queue(name + ".dlq")).isNotNull();
			assertThat(hasBinding("escala.dlx", name + ".dlq", name)).isTrue();

			String[] stages = {"retry.5s", "retry.30s", "retry.2m"};
			int[] ttls = {5_000, 30_000, 120_000};
			for (int i = 0; i < stages.length; i++) {
				String retryName = name + "." + stages[i];
				assertThat(queue(retryName).getArguments()).as(retryName)
						.containsEntry("x-message-ttl", ttls[i])
						.containsEntry("x-dead-letter-exchange", "")
						.containsEntry("x-dead-letter-routing-key", name);
				assertThat(hasBinding("escala.retry", retryName, retryName)).as(retryName).isTrue();
			}
		}
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"dev", "qa", "prod"})
	@DisplayName("TC-SCRUM-95-05 the definitions.json in the cluster's ConfigMap is the same as infra/rabbitmq/definitions.json")
	void configMapsMatchDefinitions(String environmentName) throws IOException {
		JsonNode file = JSON.readTree(Files.readString(REPO.resolve("infra/rabbitmq/definitions.json")));
		Path configMap = REPO.resolve("infra/k8s/" + environmentName + "/shared/rabbitmq-config.yaml");

		assertThat(JSON.readTree(embeddedFile(configMap, "definitions.json"))).isEqualTo(file);
	}

	private <T extends Declarable> List<T> declared(Class<T> type) {
		List<T> all = new ArrayList<>(context.getBeansOfType(type).values());
		context.getBeansOfType(Declarables.class).values().forEach(group -> all.addAll(group.getDeclarablesByType(type)));
		return all;
	}

	private Queue queue(String name) {
		return declared(Queue.class).stream().filter(queue -> queue.getName().equals(name)).findFirst()
				.orElseThrow(() -> new AssertionError("queue not declared in code: " + name));
	}

	private boolean hasBinding(String exchange, String queue, String routingKey) {
		return declared(Binding.class).stream().anyMatch(binding -> binding.getExchange().equals(exchange)
				&& binding.getDestination().equals(queue) && binding.getRoutingKey().equals(routingKey));
	}

	private static JsonNode named(JsonNode array, String name) {
		for (JsonNode node : array) {
			if (node.path("name").asText().equals(name)) {
				return node;
			}
		}
		return null;
	}

	/** The text of a "key: |" block in a ConfigMap, without its indentation. */
	static String embeddedFile(Path configMap, String key) throws IOException {
		List<String> lines = Files.readAllLines(configMap, StandardCharsets.UTF_8);
		StringBuilder text = new StringBuilder();
		int indent = -1;
		boolean inBlock = false;
		for (String line : lines) {
			if (!inBlock) {
				inBlock = line.trim().equals(key + ": |");
				continue;
			}
			if (line.isBlank()) {
				text.append('\n');
				continue;
			}
			int lineIndent = line.length() - line.stripLeading().length();
			if (indent < 0) {
				indent = lineIndent;
			}
			if (lineIndent < indent) {
				break;
			}
			text.append(line.substring(indent)).append('\n');
		}
		return text.toString();
	}
}
