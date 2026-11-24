package pt.isep.escala.escalaapi.unavailability;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.isep.escala.escalaapi.auth.SignedInMember;

/**
 * "When I can't", in my own words (SCRUM-75): send the text (202, read by the worker), read the proposal, confirm it
 * as edited, or discard it. Each member sees only their own messages: another member's answers 404, with the JSON
 * error body like every other error (SCRUM-88). GET settings tells the PWA whether AI is on, so that with it off only
 * the form is shown (SCRUM-89).
 */
@RestController
@RequestMapping("/api/v1/unavailability-messages")
class UnavailabilityMessageController {

	private final UnavailabilityMessageService service;

	UnavailabilityMessageController(UnavailabilityMessageService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<MessageResponse> send(@RequestBody MessageRequest request,
			@AuthenticationPrincipal SignedInMember member) {
		MessageResponse sent = service.send(member.memberId(), request);
		return ResponseEntity.accepted().location(URI.create("/api/v1/unavailability-messages/" + sent.id())).body(sent);
	}

	/** Any signed-in member; a literal path, so Spring prefers it to /{id} ("settings" is never read as an ID). */
	@GetMapping("/settings")
	MessageSettings settings() {
		return new MessageSettings(service.aiEnabled());
	}

	@GetMapping("/{id}")
	MessageResponse one(@PathVariable UUID id, @AuthenticationPrincipal SignedInMember member) {
		return service.get(member.memberId(), id).orElseThrow(UnavailabilityMessageController::notFound);
	}

	@PostMapping("/{id}/confirm")
	List<UnavailabilityResponse> confirm(@PathVariable UUID id, @RequestBody ConfirmRequest request,
			@AuthenticationPrincipal SignedInMember member) {
		return service.confirm(member.memberId(), id, request).orElseThrow(UnavailabilityMessageController::notFound);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> discard(@PathVariable UUID id, @AuthenticationPrincipal SignedInMember member) {
		if (!service.discard(member.memberId(), id)) {
			throw notFound();
		}
		return ResponseEntity.noContent().build();
	}

	private static ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND);
	}
}
