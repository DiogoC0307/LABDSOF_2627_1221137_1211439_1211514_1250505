package pt.isep.escala.escalaapi.piquete;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class PiqueteExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<Void> badRequest(IllegalArgumentException ignored) {
		return ResponseEntity.badRequest().build();
	}
}
