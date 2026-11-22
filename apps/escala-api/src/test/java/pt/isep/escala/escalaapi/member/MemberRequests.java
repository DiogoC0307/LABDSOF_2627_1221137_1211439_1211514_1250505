package pt.isep.escala.escalaapi.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pt.isep.escala.escalaapi.auth.MockTokens;
import tools.jackson.databind.ObjectMapper;

/** Request bodies and helpers for the member tests (SCRUM-77). All data is synthetic. */
final class MemberRequests {

	private MemberRequests() {
	}

	/** The member of AC1: active roster, driver with a class C licence and a TAS valid until 2027-03-31. */
	static String driverWithTas(String fullName, String contact) {
		return """
				{
				  "fullName": "%s",
				  "rank": "FIREFIGHTER_1ST_CLASS",
				  "rosterStatus": "ACTIVE_ROSTER",
				  "withinAgeLimit": true,
				  "contact": %s,
				  "functions": ["DRIVER"],
				  "licences": [{"licenceClass": "C", "expiresOn": "2030-06-30"}],
				  "certifications": [{"type": "TAS", "issuedOn": "2024-04-01", "expiresOn": "2027-03-31"}]
				}
				""".formatted(fullName, contact == null ? "null" : "\"" + contact + "\"");
	}

	/** Certifications and licences only, for PUT /api/v1/members/{id}/certifications. */
	static String qualifications(String issuedOn, String expiresOn) {
		return """
				{
				  "licences": [{"licenceClass": "C", "expiresOn": "2030-06-30"}],
				  "certifications": [{"type": "TAS", "issuedOn": "%s", "expiresOn": "%s"}]
				}
				""".formatted(issuedOn, expiresOn);
	}

	/** Registers a member as the admin and returns its ID. */
	static UUID register(MockMvc mockMvc, ObjectMapper objectMapper, String body) throws Exception {
		String response = mockMvc.perform(post("/api/v1/members")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(objectMapper.readTree(response).path("id").asString());
	}
}
