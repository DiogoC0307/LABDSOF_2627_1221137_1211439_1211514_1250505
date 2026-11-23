package pt.isep.escala.escalaapi.unavailability;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The 409 and 429 of the free-text messages (SCRUM-75), with Spring Boot's JSON error fields (timestamp, status,
 * error, path), the same body as every other error, and Retry-After on the 429.
 */
@RestControllerAdvice(assignableTypes = UnavailabilityMessageController.class)
class MessageRefusals {

	/** More than the allowed messages within an hour (09 T-11). */
	static class TooManyMessages extends RuntimeException {

		private final long retryAfterSeconds;

		TooManyMessages(long retryAfterSeconds) {
			super("too many messages", null, false, false);
			this.retryAfterSeconds = retryAfterSeconds;
		}
	}

	/** A message that cannot be confirmed in its state: no proposal yet, or already closed. */
	static class NotConfirmable extends RuntimeException {

		NotConfirmable() {
			super("not confirmable", null, false, false);
		}
	}

	@ExceptionHandler(TooManyMessages.class)
	ResponseEntity<Map<String, Object>> tooMany(TooManyMessages e, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, Long.toString(e.retryAfterSeconds))
				.contentType(MediaType.APPLICATION_JSON)
				.body(body(HttpStatus.TOO_MANY_REQUESTS, request));
	}

	@ExceptionHandler(NotConfirmable.class)
	ResponseEntity<Map<String, Object>> conflict(HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
				.body(body(HttpStatus.CONFLICT, request));
	}

	private static Map<String, Object> body(HttpStatus status, HttpServletRequest request) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("path", request.getRequestURI());
		return body;
	}
}
