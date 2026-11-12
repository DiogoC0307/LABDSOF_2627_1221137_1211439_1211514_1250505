package pt.isep.escala.elegibilidadeworker.messaging;

import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;

/**
 * The broker topology as infra/rabbitmq/definitions.json declares it (TC-SCRUM-95-05 checks they match). Compose and
 * the clusters import that file and set SPRING_RABBITMQ_DYNAMIC=false; the tests declare from here.
 */
public final class EventQueues {

	public static final String EVENTS_EXCHANGE = "escala.events";
	public static final String DEAD_LETTER_EXCHANGE = "escala.dlx";

	/** The retry queues of definitions.json, in order: name suffix and TTL (the delay before the next delivery). */
	private static final String[] RETRY_STAGES = {"retry.5s", "retry.30s", "retry.2m"};
	private static final int[] RETRY_TTLS_MS = {5_000, 30_000, 120_000};

	private EventQueues() {
	}

	public static Declarables exchanges() {
		return new Declarables(
				new TopicExchange(EVENTS_EXCHANGE, true, false),
				new DirectExchange(RetryPublisher.RETRY_EXCHANGE, true, false),
				new DirectExchange(DEAD_LETTER_EXCHANGE, true, false));
	}

	/** A queue bound to escala.events, with its DLX and DLQ, and its three retry queues. */
	public static Declarables queue(String name, String routingKey) {
		List<Declarable> declarables = new ArrayList<>();
		Queue main = QueueBuilder.durable(name)
				.withArgument("x-queue-type", "classic")
				.deadLetterExchange(DEAD_LETTER_EXCHANGE)
				.deadLetterRoutingKey(name)
				.build();
		declarables.add(main);
		declarables.add(binding(main, EVENTS_EXCHANGE, routingKey));
		for (int i = 0; i < RETRY_STAGES.length; i++) {
			Queue retry = QueueBuilder.durable(name + "." + RETRY_STAGES[i])
					.withArgument("x-queue-type", "classic")
					.ttl(RETRY_TTLS_MS[i])
					.deadLetterExchange("")
					.deadLetterRoutingKey(name)
					.build();
			declarables.add(retry);
			declarables.add(binding(retry, RetryPublisher.RETRY_EXCHANGE, retry.getName()));
		}
		Queue dlq = QueueBuilder.durable(name + ".dlq").withArgument("x-queue-type", "classic").build();
		declarables.add(dlq);
		declarables.add(binding(dlq, DEAD_LETTER_EXCHANGE, name));
		return new Declarables(declarables);
	}

	private static Binding binding(Queue queue, String exchange, String routingKey) {
		return new Binding(queue.getName(), Binding.DestinationType.QUEUE, exchange, routingKey, null);
	}
}
