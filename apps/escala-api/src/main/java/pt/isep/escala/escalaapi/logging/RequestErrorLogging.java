package pt.isep.escala.escalaapi.logging;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Logs the failures whose exception messages carry request or database values (an unreadable date, the
 * rejected row of a constraint) with the exception classes only (SCRUM-94). The response is unchanged:
 * {@code sendError} lets Spring Boot write its usual error body, as before.
 */
@RestControllerAdvice
class RequestErrorLogging {

	private static final Logger log = LoggerFactory.getLogger(RequestErrorLogging.class);

	@ExceptionHandler(HttpMessageNotReadableException.class)
	void unreadableBody(HttpMessageNotReadableException e, HttpServletResponse response) throws IOException {
		if (log.isWarnEnabled()) {
			log.warn("request body not readable status=400 exception={}", SafeLogValues.exceptionClasses(e));
		}
		response.sendError(HttpStatus.BAD_REQUEST.value());
	}

	@ExceptionHandler(DataAccessException.class)
	void databaseFailure(DataAccessException e, HttpServletResponse response) throws IOException {
		if (log.isErrorEnabled()) {
			log.error("request failed in the database status=500 constraint={} exception={}",
					constraintName(e), SafeLogValues.exceptionClasses(e));
		}
		response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
	}

	private static String constraintName(Throwable t) {
		for (Throwable current = t; current != null; current = current.getCause()) {
			if (current instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
				return violation.getConstraintName();
			}
		}
		return "none";
	}
}
