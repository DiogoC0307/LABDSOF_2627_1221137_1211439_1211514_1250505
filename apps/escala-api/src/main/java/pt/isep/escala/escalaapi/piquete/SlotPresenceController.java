package pt.isep.escala.escalaapi.piquete;

import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/slots")
class SlotPresenceController {

	private final ShiftConfirmationService confirmationService;

	SlotPresenceController(ShiftConfirmationService confirmationService) {
		this.confirmationService = confirmationService;
	}

	@PostMapping("/{id}/presence")
	ResponseEntity<PresenceResponse> confirmPresence(
			@PathVariable UUID id,
			@RequestBody PresenceRequest request) {
		try {
			return ResponseEntity.accepted().body(confirmationService.respond(id, request));
		}
		catch (NoSuchElementException ex) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
		}
	}
}