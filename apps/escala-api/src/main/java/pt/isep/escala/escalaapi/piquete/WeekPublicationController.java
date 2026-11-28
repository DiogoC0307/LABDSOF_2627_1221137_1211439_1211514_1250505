package pt.isep.escala.escalaapi.piquete;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.isep.escala.escalaapi.auth.SignedInMember;

/**
 * Weekly schedule publication API 
 */
@RestController
@RequestMapping("/api/v1/schedule-publications")
class WeekPublicationController {

	private final WeekPublicationService publications;

	WeekPublicationController(WeekPublicationService publications) {
		this.publications = publications;
	}

	@PostMapping
	ResponseEntity<PublicationResponse> publish(@RequestBody PublicationRequest request,
			@AuthenticationPrincipal SignedInMember supervisor) {
		PublicationResponse started = publications.publish(request, supervisor.memberId());
		return ResponseEntity.accepted()
				.location(URI.create("/api/v1/schedule-publications/" + started.id()))
				.body(started);
	}

	@GetMapping("/{id}")
	PublicationResponse get(@PathVariable UUID id) {
		return publications.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}
}
