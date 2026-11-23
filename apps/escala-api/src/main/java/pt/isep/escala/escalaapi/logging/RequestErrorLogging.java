package pt.isep.escala.escalaapi.logging;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Logs a database failure, whose exception message carries the rejected row, with the constraint name and the
 * exception classes only (SCRUM-94), and answers 500 through /error, as JSON (SCRUM-88). An unreadable request
 * body is answered and logged the same way by validation/InvalidRequestHandler.
 */
@RestControllerAdvice
class RequestErrorLogging {

	private static final Logger log = LoggerFactory.getLogger(RequestErrorLogging.class);

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
