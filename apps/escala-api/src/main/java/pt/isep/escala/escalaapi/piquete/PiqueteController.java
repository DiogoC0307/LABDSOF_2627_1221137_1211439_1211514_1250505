package pt.isep.escala.escalaapi.piquete;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/piquetes")
class PiqueteController {

	private final PiqueteService piquetes;

	PiqueteController(PiqueteService piquetes) {
		this.piquetes = piquetes;
	}

	@PostMapping
	ResponseEntity<PiqueteResponse> create(@RequestBody PiqueteRequest request) {
		PiqueteResponse created = piquetes.create(request);
		return ResponseEntity.created(URI.create("/api/v1/piquetes/" + created.id())).body(created);
	}

	@GetMapping("/{id}")
	PiqueteResponse get(@PathVariable UUID id) {
		return piquetes.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	/** Changes a shift (SCRUM-65): 404 when there is no such piquete, 409 when its day has passed. */
	@PutMapping("/{id}")
	PiqueteResponse update(@PathVariable UUID id, @RequestBody PiqueteRequest request) {
		return piquetes.update(id, request).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	/** Every piquete, or with {@code week} (e.g. {@code 2026-W48}) the week's, Monday to Sunday, in date order. */
	@GetMapping
	List<PiqueteResponse> list(@RequestParam(required = false) String week) {
		return week == null ? piquetes.list() : piquetes.listWeek(week);
	}
}
