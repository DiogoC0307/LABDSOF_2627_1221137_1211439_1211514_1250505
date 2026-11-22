package pt.isep.escala.escalaapi.member;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.isep.escala.escalaapi.auth.SignedInMember;

/**
 * Members, their functions, licences and certifications (SCRUM-77). The role is checked in SecurityConfig: only the
 * admin registers and edits; the supervisor and the admin read the list; a member reads only themself (checked here).
 */
@RestController
@RequestMapping("/api/v1")
class MemberController {

	private final MemberService service;

	MemberController(MemberService service) {
		this.service = service;
	}

	@GetMapping("/members")
	List<MemberResponse> list(@AuthenticationPrincipal SignedInMember viewer) {
		return service.list(viewer);
	}

	@GetMapping("/members/{id}")
	ResponseEntity<MemberResponse> get(@PathVariable UUID id, @AuthenticationPrincipal SignedInMember viewer) {
		if (viewer.role() == AccessRole.MEMBER && !viewer.memberId().equals(id)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		return ResponseEntity.of(service.get(id, viewer));
	}

	@PostMapping("/members")
	ResponseEntity<MemberResponse> register(@RequestBody MemberRequest request,
			@AuthenticationPrincipal SignedInMember admin) {
		MemberResponse created = service.register(request, admin);
		return ResponseEntity.created(URI.create("/api/v1/members/" + created.id())).body(created);
	}

	@PutMapping("/members/{id}")
	MemberResponse changeDetails(@PathVariable UUID id, @RequestBody MemberDetailsRequest request,
			@AuthenticationPrincipal SignedInMember admin) {
		return service.changeDetails(id, request, admin).orElseThrow(MemberController::notFound);
	}

	@PutMapping("/members/{id}/certifications")
	MemberResponse replaceQualifications(@PathVariable UUID id, @RequestBody QualificationsRequest request,
			@AuthenticationPrincipal SignedInMember admin) {
		return service.replaceQualifications(id, request, admin).orElseThrow(MemberController::notFound);
	}

	@GetMapping("/certification-types")
	List<CertificationTypeResponse> certificationTypes() {
		return service.certificationTypes();
	}

	private static ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND);
	}
}
