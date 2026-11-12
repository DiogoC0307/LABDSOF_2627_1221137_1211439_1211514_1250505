package pt.isep.escala.escalaapi.messaging;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.amqp.autoconfigure.CachingConnectionFactoryConfigurer;
import org.springframework.boot.amqp.autoconfigure.RabbitConnectionFactoryBeanConfigurer;
import org.springframework.stereotype.Component;

/**
 * Sends a failed message to a retry queue and returns only once the broker has confirmed it and routed it, so the
 * original can then be acknowledged. If the service stops between the two steps the message is delivered twice,
 * never lost; consumers ignore the duplicate by its eventId (ADR 0009).
 *
 * <p>It has its own connection with publisher confirms: the application's connection, which the outbox uses, is
 * left as it is (publisher confirms for the outbox are SCRUM-155).
 */
@Component
public class RetryPublisher implements DisposableBean {

	public static final String RETRY_EXCHANGE = "escala.retry";
	public static final String RETRY_COUNT_HEADER = "escala-retry-count";

	private static final String RETRY_ID_HEADER = "escala-retry-id";
	private static final long CONFIRM_TIMEOUT_MS = 10_000;

	private final CachingConnectionFactory connectionFactory;
	private final RabbitTemplate template;
	private final Set<String> returned = ConcurrentHashMap.newKeySet();

	RetryPublisher(RabbitConnectionFactoryBeanConfigurer rabbitConfigurer, CachingConnectionFactoryConfigurer cachingConfigurer)
			throws Exception {
		RabbitConnectionFactoryBean bean = new RabbitConnectionFactoryBean();
		rabbitConfigurer.configure(bean);
		bean.afterPropertiesSet();
		this.connectionFactory = new CachingConnectionFactory(bean.getObject());
		cachingConfigurer.configure(this.connectionFactory);
		this.connectionFactory.setPublisherConfirmType(ConfirmType.SIMPLE);
		this.connectionFactory.setPublisherReturns(true);
		this.connectionFactory.setConnectionNameStrategy(factory -> "retry-publisher");
		this.template = new RabbitTemplate(this.connectionFactory);
		this.template.setMandatory(true);
		this.template.setReturnsCallback(message -> {
			Object id = message.getMessage().getMessageProperties().getHeader(RETRY_ID_HEADER);
			if (id != null) {
				this.returned.add(id.toString());
			}
		});
	}

	/** Publishes {@code message} to the retry queue {@code routingKey} with the new count; throws if not confirmed. */
	public void publish(Message message, String routingKey, int retryCount) {
		String id = UUID.randomUUID().toString();
		Message copy = MessageBuilder.fromClonedMessage(message)
				// A received message carries its mode in receivedDeliveryMode only; without this the copy would be
				// transient and lost if the broker restarts while it waits in a retry queue or in the DLQ.
				.setDeliveryMode(MessageDeliveryMode.PERSISTENT)
				.setHeader(RETRY_COUNT_HEADER, retryCount)
				.setHeader(RETRY_ID_HEADER, id)
				.build();
		this.template.invoke(operations -> {
			operations.send(RETRY_EXCHANGE, routingKey, copy);
			operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
			return null;
		});
		// The broker returns an unroutable mandatory message before it confirms it.
		if (this.returned.remove(id)) {
			throw new AmqpException("no retry queue is bound to " + routingKey);
		}
	}

	@Override
	public void destroy() {
		this.connectionFactory.destroy();
	}
}
