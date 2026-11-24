package pt.isep.escala.escalaapi.unavailability;

import java.time.Instant;
import org.springframework.stereotype.Component;
import pt.isep.escala.escalaapi.trace.TraceId;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * ExtracaoPedida v1 (contracts/events/extracao-pedida.schema.json): the text and the reference date only. The member,
 * their name and anything else about them never go in the event (09).
 */
@Component
class ExtracaoPedidaFactory {

	static final String ROUTING_KEY = "extracao.pedida";

	private final ObjectMapper objectMapper;

	ExtracaoPedidaFactory(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	String payload(UnavailabilityMessage message, Instant now) {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("eventId", message.getRequestEventId().toString());
		root.put("type", "ExtracaoPedida");
		root.put("schemaVersion", 1);
		root.put("occurredAt", now.toString());
		root.put("traceId", TraceId.current());
		root.put("unavailabilityRequestId", message.getId().toString());
		root.put("freeText", message.getFreeText());
		root.put("referenceDate", message.getReferenceDate().toString());
		return objectMapper.writeValueAsString(root);
	}
}
