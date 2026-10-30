package pt.isep.escala.escalaapi.piquete;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/piquetes")
class PiqueteController {

	private final PiqueteService piquetes;

	PiqueteController(PiqueteService piquetes) {
		this.piquetes = piquetes;
	}

	@PostMapping
	ResponseEntity<PiqueteResponse> create(@RequestBody CreatePiqueteRequest request) {
		PiqueteResponse created = piquetes.create(request);
		return ResponseEntity.created(URI.create("/api/v1/piquetes/" + created.id())).body(created);
	}

	@GetMapping
	List<PiqueteResponse> list() {
		return piquetes.list();
	}
}
