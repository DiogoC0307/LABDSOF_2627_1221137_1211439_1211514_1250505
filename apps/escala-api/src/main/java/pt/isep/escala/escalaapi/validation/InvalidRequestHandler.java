package pt.isep.escala.escalaapi.validation;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pt.isep.escala.escalaapi.logging.SafeLogValues;
import pt.isep.escala.escalaapi.validation.InvalidRequestException.InvalidField;
import tools.jackson.core.JacksonException;

/**
 * The one format of every 400 (SCRUM-88): Spring Boot's error fields plus {@code errors: [{field, code}]}, never the
 * rejected values (documented in openapi.yaml as ValidationErrorResponse). Always JSON, whatever the Accept header.
 * <ul>
 * <li>an invalid field found by the services (SCRUM-77, SCRUM-64): the codes they chose;</li>
 * <li>a value Jackson cannot read (a date, an enum value): the field and {@code invalid};</li>
 * <li>no body, or a body that is not JSON: {@code body} and {@code not_readable};</li>
 * <li>a path parameter of the wrong type (an ID that is not a UUID): the parameter and {@code invalid}.</li>
 * </ul>
 * An unreadable body is logged with the exception classes only, as in SCRUM-94; nothing else is logged here.
 */
@RestControllerAdvice
class InvalidRequestHandler {

	private static final Logger log = LoggerFactory.getLogger(InvalidRequestHandler.class);

	private static final String INVALID = "invalid";

	@ExceptionHandler(InvalidRequestException.class)
	ResponseEntity<Map<String, Object>> invalid(InvalidRequestException e, HttpServletRequest request) {
		return badRequest(request, e.errors());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
		if (log.isWarnEnabled()) {
			log.warn("request body not readable status=400 exception={}", SafeLogValues.exceptionClasses(e));
		}
		String field = jsonField(e);
		return badRequest(request, List.of(field == null
				? new InvalidField("body", "not_readable")
				: new InvalidField(field, INVALID)));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<Map<String, Object>> wrongType(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
		return badRequest(request, List.of(new InvalidField(e.getName(), INVALID)));
	}

	/** The JSON path of the value Jackson could not read, e.g. {@code licences[0].licenceClass}; null for no path. */
	private static String jsonField(Throwable e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof JacksonException jackson && !jackson.getPath().isEmpty()) {
				return path(jackson.getPath());
			}
		}
		return null;
	}

	private static String path(List<JacksonException.Reference> references) {
		StringBuilder field = new StringBuilder();
		for (JacksonException.Reference reference : references) {
			if (reference.getPropertyName() != null) {
				field.append(field.isEmpty() ? "" : ".").append(reference.getPropertyName());
			} else if (reference.getIndex() >= 0) {
				field.append('[').append(reference.getIndex()).append(']');
			}
		}
		return field.isEmpty() ? null : field.toString();
	}

	private static ResponseEntity<Map<String, Object>> badRequest(HttpServletRequest request, List<InvalidField> errors) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
		body.put("path", request.getRequestURI());
		body.put("errors", errors);
		// A preset Content-Type is written as is, so an Accept of text/html still gets this JSON (no 406).
		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(body);
	}
}
