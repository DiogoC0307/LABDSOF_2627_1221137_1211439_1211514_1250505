package pt.isep.escala.escalaapi.unavailability;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.isep.escala.escalaapi.auth.SignedInMember;

/**
 * "When I can't", in my own words (SCRUM-75): send the text (202, read by the worker), read the proposal, confirm it
 * as edited, or discard it. Each member sees only their own messages: another member's answers 404.
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

	@GetMapping("/{id}")
	ResponseEntity<MessageResponse> one(@PathVariable UUID id, @AuthenticationPrincipal SignedInMember member) {
		return ResponseEntity.of(service.get(member.memberId(), id));
	}

	@PostMapping("/{id}/confirm")
	ResponseEntity<List<UnavailabilityResponse>> confirm(@PathVariable UUID id, @RequestBody ConfirmRequest request,
			@AuthenticationPrincipal SignedInMember member) {
		return ResponseEntity.of(service.confirm(member.memberId(), id, request));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> discard(@PathVariable UUID id, @AuthenticationPrincipal SignedInMember member) {
		return service.discard(member.memberId(), id)
				? ResponseEntity.noContent().build()
				: ResponseEntity.notFound().build();
	}
}
