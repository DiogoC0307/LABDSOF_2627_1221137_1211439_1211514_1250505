package pt.isep.escala.escalaapi.validation;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Answers an invalid request (members, unavailability) with Spring Boot's error body plus {@code errors: [{field, code}]}, never the
 * rejected values (SCRUM-77, SCRUM-64; documented in openapi.yaml as ValidationErrorResponse). Nothing is logged here.
 */
@RestControllerAdvice
class InvalidRequestHandler {

	@ExceptionHandler(InvalidRequestException.class)
	ResponseEntity<Map<String, Object>> invalid(InvalidRequestException e, HttpServletRequest request) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
		body.put("path", request.getRequestURI());
		body.put("errors", e.errors());
		return ResponseEntity.badRequest().body(body);
	}
}
