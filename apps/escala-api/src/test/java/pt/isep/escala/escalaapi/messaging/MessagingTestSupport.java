package pt.isep.escala.escalaapi.messaging;

import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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

	static final String QUEUE = "api.avaliacao-concluida";
	static final String DLQ = QUEUE + ".dlq";
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

	static void sendConcluida(RabbitTemplate rabbit, UUID eventId, UUID assignmentId, String result) {
		send(rabbit, "avaliacao.concluida", concluida(eventId, assignmentId, result).getBytes(StandardCharsets.UTF_8));
	}

	static String concluida(UUID eventId, UUID assignmentId, String result) {
		return """
				{"eventId":"%s","type":"AvaliacaoConcluida","schemaVersion":1,"occurredAt":"%s","traceId":"%s",
				 "requestEventId":"%s","assignmentId":"%s","result":"%s","violatedRules":[],
				 "engineVersion":"SEMPRE_ELEGIVEL"}
				""".formatted(eventId, Instant.now(), UUID.randomUUID(), UUID.randomUUID(), assignmentId, result);
	}

	/** A piquete with one PENDING slot, written straight to the database; returns the slot's assignment ID. */
	static UUID insertPendingSlot(JdbcTemplate jdbc) {
		UUID piqueteId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		jdbc.update("INSERT INTO piquete (id, duty_date) VALUES (?, ?)", piqueteId, LocalDate.of(2031, 8, 1));
		jdbc.update("INSERT INTO slot (id, assignment_id, role, status, piquete_id) VALUES (?, ?, 'CREW_MEMBER', 'PENDING', ?)",
				UUID.randomUUID(), assignmentId, piqueteId);
		return assignmentId;
	}

	static String slotStatus(JdbcTemplate jdbc, UUID assignmentId) {
		return jdbc.queryForObject("SELECT status FROM slot WHERE assignment_id = ?", String.class, assignmentId);
	}

	static long messageCount(RabbitAdmin admin, String queue) {
		QueueInformation info = admin.getQueueInfo(queue);
		return info == null ? -1 : info.getMessageCount();
	}

	/** Waits for the next message in the DLQ; the DLQ may not exist yet. */
	static Message awaitDeadLetter(RabbitTemplate rabbit, String dlq, Duration atMost) {
		return await().atMost(atMost).ignoreExceptions().until(() -> rabbit.receive(dlq, 200), java.util.Objects::nonNull);
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
