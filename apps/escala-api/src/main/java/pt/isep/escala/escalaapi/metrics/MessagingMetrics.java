package pt.isep.escala.escalaapi.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The messaging metrics of SCRUM-97, read every 15 s (a scrape is every 30 s):
 *
 * <ul>
 * <li>{@code escala.outbox.pending}: outbox rows not published yet (0 when there are none);</li>
 * <li>{@code escala.dlq.messages{queue}}: messages in this service's dead-letter queues, asked from the broker with a
 * passive declare (the service's broker user may do that on its own {@code api.*} queues);</li>
 * <li>{@code escala.messages.dead_lettered{queue}}: messages this service rejected to a DLQ (malformed, or retries
 * exhausted), counted by {@code BrokerRetryAdvice}; {@code queue} is the queue they were consumed from, not the DLQ.
 * It feeds SLI 2.</li>
 * </ul>
 *
 * The DLQs are found from the queues this service declares, so a new consumed queue is measured without a change here.
 */
@Component
public class MessagingMetrics {

	static final String OWN_QUEUE_PREFIX = "api.";

	private static final Logger log = LoggerFactory.getLogger(MessagingMetrics.class);

	private final MeterRegistry registry;
	private final JdbcTemplate jdbc;
	private final RabbitAdmin admin;
	private final List<String> deadLetterQueues;
	private final AtomicLong outboxPending = new AtomicLong();
	private final MultiGauge dlqMessages;
	private final Map<String, Long> messagesByQueue = new ConcurrentHashMap<>();

	public MessagingMetrics(MeterRegistry registry, JdbcTemplate jdbc, ConnectionFactory connectionFactory,
			List<Declarables> topology) {
		this.registry = registry;
		this.jdbc = jdbc;
		this.admin = new RabbitAdmin(connectionFactory);
		this.deadLetterQueues = topology.stream()
				.flatMap(group -> group.getDeclarablesByType(Queue.class).stream())
				.map(Queue::getName)
				.filter(name -> name.startsWith(OWN_QUEUE_PREFIX) && name.endsWith(".dlq"))
				.distinct().sorted().toList();
		Gauge.builder("escala.outbox.pending", outboxPending, AtomicLong::get)
				.description("Outbox rows not published to the broker yet").register(registry);
		this.dlqMessages = MultiGauge.builder("escala.dlq.messages")
				.description("Messages in the dead-letter queues of this service").register(registry);
		// every consumed queue shows 0 from the start, so a rate over time works before the first dead letter
		deadLetterQueues.forEach(dlq -> deadLettered(dlq.substring(0, dlq.length() - ".dlq".length()), 0));
	}

	/** Called by {@code BrokerRetryAdvice} for every message it rejects to the dead-letter queue. */
	public void deadLettered(String queue) {
		deadLettered(queue, 1);
	}

	private void deadLettered(String queue, double amount) {
		String tag = queue != null && queue.startsWith(OWN_QUEUE_PREFIX) ? queue : MetricTags.OTHER;
		Counter.builder("escala.messages.dead_lettered").description("Messages rejected to a dead-letter queue")
				.tag(MetricTags.QUEUE, tag).register(registry).increment(amount);
	}

	List<String> deadLetterQueues() {
		return deadLetterQueues;
	}

	@Scheduled(fixedDelay = 15_000, initialDelay = 5_000)
	void refresh() {
		refreshOutbox();
		refreshDeadLetterQueues();
	}

	void refreshOutbox() {
		try {
			Long pending = jdbc.queryForObject("select count(*) from outbox_event where published_at is null", Long.class);
			outboxPending.set(pending == null ? 0 : pending);
		}
		catch (RuntimeException e) {
			// the database is down: keep the last values; readiness already reports it
			log.debug("outbox metric not refreshed exception={}", e.getClass().getSimpleName());
		}
	}

	void refreshDeadLetterQueues() {
		for (String queue : deadLetterQueues) {
			try {
				QueueInformation info = admin.getQueueInfo(queue);
				if (info != null) {
					messagesByQueue.put(queue, (long) info.getMessageCount());
				}
			}
			catch (RuntimeException e) {
				// the broker is down: keep the last value
				log.debug("dlq metric not refreshed queue={} exception={}", queue, e.getClass().getSimpleName());
			}
		}
		dlqMessages.register(messagesByQueue.entrySet().stream()
				.map(e -> MultiGauge.Row.of(Tags.of(MetricTags.QUEUE, e.getKey()), e.getValue())).toList(), true);
	}
}
