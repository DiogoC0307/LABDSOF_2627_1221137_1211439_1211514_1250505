package pt.isep.escala.escalaapi.logging;

import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.system.CapturedOutput;

/**
 * Helpers for the SCRUM-94 tests: a marker stands for free text or a contact, and must never reach the logs.
 */
final class LogLeakSupport {

	private LogLeakSupport() {
	}

	static String marker() {
		return "LEAKMARK" + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * A message of at most 50 bytes, so Spring AMQP would print its body (it prints only the size of longer
	 * bodies). Its eventId is not a UUID, so the listener fails on it.
	 */
	static String shortInvalidEvent(String marker) {
		return "{\"eventId\":\"" + marker + "\"}";
	}

	static void send(RabbitTemplate rabbit, String routingKey, String body) {
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbit.send("escala.events", routingKey, new Message(body.getBytes(StandardCharsets.UTF_8), properties));
	}

	/** Waits until the listener has failed on a message, whatever is logged for it. */
	static void awaitListenerFailure(CapturedOutput output) {
		await().atMost(Duration.ofSeconds(15)).until(() -> output.getAll().toLowerCase().contains("listener failed")
				|| output.getAll().toLowerCase().contains("listener threw"));
		// let any further lines of the same failure (stack traces, redeliveries) be written
		await().pollDelay(Duration.ofSeconds(2)).until(() -> true);
	}
}
