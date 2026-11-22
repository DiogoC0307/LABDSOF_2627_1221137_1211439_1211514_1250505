package pt.isep.escala.escalaapi.auth;

import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.isep.escala.escalaapi.member.Member;
import pt.isep.escala.escalaapi.member.MemberRepository;

/**
 * The mocked sign-in (ADR 0006, SCRUM-63): the sign-in screen lists the seeded users and the user picks one. No
 * password: only synthetic members may exist while sign-in is mocked (09, T-01).
 */
@RestController
@RequestMapping("/api/v1/auth")
class MockSignInController {

	private static final Logger log = LoggerFactory.getLogger(MockSignInController.class);

	private final MemberRepository members;

	MockSignInController(MemberRepository members) {
		this.members = members;
	}

	/** The active members the user can sign in as: ID, name and role only, never a contact. */
	@GetMapping("/mock-users")
	List<MockUserResponse> users() {
		return members.findByActiveTrue().stream()
				.sorted(Comparator.comparing(Member::getAccessRole).thenComparing(Member::getFullName))
				.map(member -> new MockUserResponse(member.getId(), member.getFullName(), member.getAccessRole()))
				.toList();
	}

	/** A token for an active member; 401 and no token for an unknown or inactive one. */
	@PostMapping("/mock-sign-in")
	ResponseEntity<MockSignInResponse> signIn(@RequestBody MockSignInRequest request) {
		if (request.memberId() == null) {
			return ResponseEntity.badRequest().build();
		}
		return members.findByIdAndActiveTrue(request.memberId())
				.map(member -> {
					log.info("mock sign-in memberId={} role={}", member.getId(), member.getAccessRole());
					String token = new MockToken(member.getId(), member.getAccessRole()).encode();
					return ResponseEntity.ok(new MockSignInResponse(
							token, member.getId(), member.getAccessRole(), member.getFullName()));
				})
				.orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
	}
}
