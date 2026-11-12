package pt.isep.escala.escalaapi.logging;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.listener.ListenerExecutionFailedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AMQP's default error handler with a different log line: it writes the queue, the exception classes and
 * the event's IDs, never the message body or the exception messages (SCRUM-94). Rejecting or requeueing the
 * message is left to {@link ConditionalRejectingErrorHandler}, unchanged; retries and the dead-letter queue are
 * SCRUM-95.
 */
public class SafeListenerErrorHandler extends ConditionalRejectingErrorHandler {

	private static final Logger log = LoggerFactory.getLogger(SafeListenerErrorHandler.class);

	private static final ObjectMapper JSON = JsonMapper.builder().build();

	private static final String UNKNOWN = "unknown";

	private static final String MISSING = "missing";

	@Override
	protected void log(Throwable t) {
		String queue = UNKNOWN;
		String type = UNKNOWN;
		String eventId = MISSING;
		String traceId = MISSING;
		if (t instanceof ListenerExecutionFailedException failed && failed.getFailedMessage() != null) {
			Message message = failed.getFailedMessage();
			if (message.getMessageProperties().getConsumerQueue() != null) {
				queue = message.getMessageProperties().getConsumerQueue();
			}
			try {
				JsonNode body = JSON.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
				type = knownType(body.path("type").asText(""));
				eventId = SafeLogValues.uuidOrInvalid(body.path("eventId").asText(""));
				traceId = SafeLogValues.uuidOrInvalid(body.path("traceId").asText(""));
			} catch (RuntimeException notJson) {
				type = "not JSON";
			}
		}
		if (log.isWarnEnabled()) {
			log.warn("listener failed queue={} type={} eventId={} traceId={} exception={}",
					queue, type, eventId, traceId, SafeLogValues.exceptionClasses(t));
		}
	}

	private static String knownType(String type) {
		return switch (type) {
			case "AvaliacaoPedida", "AvaliacaoConcluida", "ExtracaoPedida", "ExtracaoConcluida", "RiscoAtualizado" -> type;
			case "" -> MISSING;
			default -> UNKNOWN;
		};
	}
}
