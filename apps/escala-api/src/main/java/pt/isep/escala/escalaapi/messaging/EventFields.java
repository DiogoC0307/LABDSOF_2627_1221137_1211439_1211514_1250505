package pt.isep.escala.escalaapi.messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Reads an event and its IDs; anything unreadable is a {@link MalformedEventException}. */
public final class EventFields {

	private EventFields() {
	}

	public static JsonNode body(ObjectMapper objectMapper, Message message) {
		try {
			JsonNode body = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
			if (body == null || !body.isObject()) {
				throw new MalformedEventException("the body is not a JSON object");
			}
			return body;
		} catch (JacksonException e) {
			throw new MalformedEventException("the body is not JSON");
		}
	}

	public static UUID uuid(JsonNode body, String field) {
		try {
			return UUID.fromString(body.path(field).asText(""));
		} catch (IllegalArgumentException e) {
			throw new MalformedEventException(field + " is not a UUID");
		}
	}
}
