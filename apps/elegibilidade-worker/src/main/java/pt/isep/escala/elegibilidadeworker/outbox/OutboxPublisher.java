package pt.isep.escala.elegibilidadeworker.outbox;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class OutboxPublisher {

	public static final String EXCHANGE = "escala.events";

	private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

	private final OutboxEventRepository outbox;
	private final RabbitTemplate rabbitTemplate;

	OutboxPublisher(OutboxEventRepository outbox, RabbitTemplate rabbitTemplate) {
		this.outbox = outbox;
		this.rabbitTemplate = rabbitTemplate;
	}

	@Scheduled(fixedDelay = 500)
	@Transactional
	void publishPending() {
		for (OutboxEvent event : outbox.findByPublishedAtIsNullOrderByCreatedAtAsc()) {
			MessageProperties properties = new MessageProperties();
			properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
			Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), properties);
			rabbitTemplate.send(EXCHANGE, event.getRoutingKey(), message);
			event.markPublished(Instant.now());
			outbox.save(event);
			log.info("outbox published routingKey={} eventId={}", event.getRoutingKey(), event.getId());
		}
	}
}
