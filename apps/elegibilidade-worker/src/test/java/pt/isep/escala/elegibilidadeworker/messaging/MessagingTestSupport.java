package pt.isep.escala.elegibilidadeworker.messaging;

import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Helpers for the SCRUM-95 tests. The tests talk to the broker and the database with their own connections (the
 * broker's admin user), so they work while the service is stopped or cannot log in.
 */
final class MessagingTestSupport {

	static final String QUEUE = "worker.avaliacao-pedida";
	static final String DLQ = QUEUE + ".dlq";
	static final String RESULTS = "api.avaliacao-concluida";
	static final String RETRY_COUNT_HEADER = "escala-retry-count";

	/**
	 * Short retry stages, declared only by the tests. The second one is longer, so TC-01 can restart the service
	 * while the message waits in it.
	 */
	static final String TEST_RETRY_STAGES = "test-retry.1,test-retry.2,test-retry.3";
	static final int[] TEST_RETRY_TTLS_MS = {300, 4000, 300};

	private MessagingTestSupport() {
	}

	static CachingConnectionFactory adminConnection(RabbitMQContainer rabbit) {
		CachingConnectionFactory factory = new CachingConnectionFactory(rabbit.getHost(), rabbit.getAmqpPort());
		factory.setUsername(rabbit.getAdminUsername());
		factory.setPassword(rabbit.getAdminPassword());
		return factory;
	}

	static JdbcTemplate jdbc(PostgreSQLContainer postgres) {
		return new JdbcTemplate(new SimpleDriverDataSource(
				new org.postgresql.Driver(), postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}

	/** The test's retry queues: each holds a message for its TTL, then sends it back to {@code queue}. */
	static void declareShortRetryQueues(RabbitAdmin admin, String queue) {
		DirectExchange retry = new DirectExchange("escala.retry", true, false);
		admin.declareExchange(retry);
		String[] stages = TEST_RETRY_STAGES.split(",");
		for (int i = 0; i < stages.length; i++) {
			Queue stage = QueueBuilder.durable(queue + "." + stages[i])
					.withArgument("x-queue-type", "classic")
					.ttl(TEST_RETRY_TTLS_MS[i])
					.deadLetterExchange("")
					.deadLetterRoutingKey(queue)
					.build();
			admin.declareQueue(stage);
			admin.declareBinding(BindingBuilder.bind(stage).to(retry).with(stage.getName()));
		}
	}

	static void send(RabbitTemplate rabbit, String routingKey, byte[] body) {
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbit.send("escala.events", routingKey, new Message(body, properties));
	}

	static void sendPedido(RabbitTemplate rabbit, UUID eventId, UUID assignmentId) {
		send(rabbit, "avaliacao.pedida", pedido(eventId, assignmentId).getBytes(StandardCharsets.UTF_8));
	}

	static String pedido(UUID eventId, UUID assignmentId) {
		return """
				{"eventId":"%s","type":"AvaliacaoPedida","schemaVersion":1,"occurredAt":"%s","traceId":"%s",
				 "purpose":"ASSIGNMENT","assignmentId":"%s",
				 "slot":{"slotId":"%s","start":"2031-08-01T00:00:00Z","end":"2031-08-02T00:00:00Z",
				         "requiredCertification":null,"requiredLicenceClass":null},
				 "candidate":{"memberId":"00000000-0000-4000-8000-000000000001","inActiveRoster":true,
				              "withinAgeLimit":true,"certifications":[],"licenceClasses":[],"unavailabilities":[],
				              "otherAssignments":[]}}
				""".formatted(eventId, Instant.now(), UUID.randomUUID(), assignmentId, UUID.randomUUID());
	}

	static long messageCount(RabbitAdmin admin, String queue) {
		QueueInformation info = admin.getQueueInfo(queue);
		return info == null ? -1 : info.getMessageCount();
	}

	/** Waits for the next message in a queue; the queue may not exist yet. */
	static Message awaitMessage(RabbitTemplate rabbit, String queue, Duration atMost) {
		return await().atMost(atMost).ignoreExceptions().until(() -> rabbit.receive(queue, 200), java.util.Objects::nonNull);
	}

	static Message awaitDeadLetter(RabbitTemplate rabbit, String dlq, Duration atMost) {
		return awaitMessage(rabbit, dlq, atMost);
	}

	/** How many times the broker dead-lettered the message from {@code queue}, from its {@code x-death} header. */
	static long deathCount(Message message, String queue) {
		List<Map<String, ?>> deaths = message.getMessageProperties().getXDeathHeader();
		if (deaths == null) {
			return 0;
		}
		return deaths.stream()
				.filter(death -> queue.equals(String.valueOf(death.get("queue"))))
				.mapToLong(death -> ((Number) death.get("count")).longValue())
				.sum();
	}

	static long occurrences(String text, String part) {
		long count = 0;
		for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) {
			count++;
		}
		return count;
	}
}
