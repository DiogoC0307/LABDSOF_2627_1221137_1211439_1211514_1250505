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
 * "When I can't": each member declares, reads and deletes only their own periods (SCRUM-64). A period of another
 * member answers 404, not 403, so its existence is not revealed. There is no PUT: a change is delete and declare.
 */
@RestController
@RequestMapping("/api/v1/unavailabilities")
class UnavailabilityController {

	private final UnavailabilityService service;

	UnavailabilityController(UnavailabilityService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<UnavailabilityResponse> declare(@RequestBody UnavailabilityRequest request,
			@AuthenticationPrincipal SignedInMember member) {
		UnavailabilityResponse created = service.declare(member.memberId(), request);
		return ResponseEntity.created(URI.create("/api/v1/unavailabilities/" + created.id())).body(created);
	}

	@GetMapping
	List<UnavailabilityResponse> mine(@AuthenticationPrincipal SignedInMember member) {
		return service.mine(member.memberId());
	}

	@GetMapping("/{id}")
	ResponseEntity<UnavailabilityResponse> one(@PathVariable UUID id, @AuthenticationPrincipal SignedInMember member) {
		return ResponseEntity.ok(service.mine(member.memberId(), id).orElseThrow(UnavailabilityController::notFound));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal SignedInMember member) {
		if (!service.delete(member.memberId(), id)) {
			throw notFound();
		}
		return ResponseEntity.noContent().build();
	}

	/** 404 through /error, so it has the JSON error body (SCRUM-88). */
	private static ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND);
	}
}
