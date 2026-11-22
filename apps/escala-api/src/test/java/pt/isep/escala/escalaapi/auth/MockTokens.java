package pt.isep.escala.escalaapi.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Builds the mocked bearer token of ADR 0006 the way a client does, independently of the production code, for the
 * three users seeded by V3 (SCRUM-63).
 */
public final class MockTokens {

	public static final UUID MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
	public static final UUID SUPERVISOR_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
	public static final UUID ADMIN_ID = UUID.fromString("00000000-0000-4000-8000-000000000003");

	private MockTokens() {
	}

	public static String member() {
		return bearer(MEMBER_ID, "MEMBER");
	}

	public static String supervisor() {
		return bearer(SUPERVISOR_ID, "SUPERVISOR");
	}

	public static String admin() {
		return bearer(ADMIN_ID, "ADMIN");
	}

	/** The value of the Authorization header for any member ID and role, valid or not. */
	public static String bearer(UUID memberId, String role) {
		return "Bearer " + token(memberId, role);
	}

	public static String token(UUID memberId, String role) {
		String json = "{\"memberId\":\"" + memberId + "\",\"role\":\"" + role + "\"}";
		return "mock." + Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}
}
