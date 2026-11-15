package pt.isep.escala.escalaapi.messaging;

/**
 * A message that can never be processed (not JSON, or an ID that is not a UUID): it goes to the DLQ at once, without
 * retries (SCRUM-95). The message text names the field, never its value.
 */
public class MalformedEventException extends RuntimeException {

	public MalformedEventException(String problem) {
		super(problem);
	}
}
