package pt.isep.escala.escalaapi.piquete;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Who can take a role on a shift. 
 * 
 * Members are checked by the eligibility rules.
 */
@RestController
@RequestMapping("/api/v1/candidate-checks")
class CandidateCheckController {

	private final CandidateCheckService checks;

	CandidateCheckController(CandidateCheckService checks) {
		this.checks = checks;
	}

	@PostMapping
	ResponseEntity<CandidateCheckResponse> start(@RequestBody CandidateCheckRequest request) {
		CandidateCheckResponse started = checks.start(request);
		return ResponseEntity.accepted()
				.location(URI.create("/api/v1/candidate-checks/" + started.id()))
				.body(started);
	}

	@GetMapping("/{id}")
	CandidateCheckResponse get(@PathVariable UUID id) {
		return checks.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}
}
