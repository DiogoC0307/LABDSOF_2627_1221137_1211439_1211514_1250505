package pt.isep.escala.escalaapi.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import pt.isep.escala.escalaapi.member.AccessRole;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The mocked bearer token of ADR 0006: {@code mock.} followed by the base64url of
 * {@code {"memberId":"…","role":"…"}}. It is not signed, so it proves nothing by itself: the server accepts it only
 * for an active member whose role in the database is the role in the token (SCRUM-63).
 */
record MockToken(UUID memberId, AccessRole role) {

	static final String PREFIX = "mock.";

	private static final ObjectMapper JSON = JsonMapper.builder().build();

	String encode() {
		ObjectNode body = JSON.createObjectNode();
		body.put("memberId", memberId.toString());
		body.put("role", role.name());
		return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(JSON.writeValueAsBytes(body));
	}

	/** The token, or empty when it is not a mocked token that can be read. */
	static Optional<MockToken> decode(String token) {
		if (token == null || !token.startsWith(PREFIX)) {
			return Optional.empty();
		}
		try {
			byte[] json = Base64.getUrlDecoder().decode(token.substring(PREFIX.length()));
			JsonNode body = JSON.readTree(new String(json, StandardCharsets.UTF_8));
			return Optional.of(new MockToken(
					UUID.fromString(body.path("memberId").asText("")),
					AccessRole.valueOf(body.path("role").asText(""))));
		} catch (IllegalArgumentException | JacksonException unreadable) {
			return Optional.empty();
		}
	}
}
