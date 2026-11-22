package pt.isep.escala.escalaapi.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** The admin registers members with their functions, licences and certifications (SCRUM-77, AC1 and AC2). */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class RegisterMemberIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	@DisplayName("TC-SCRUM-77-01 a member registered on the active roster as a driver, class C, TAS until 2027-03-31, appears in the members list with those details")
	void registeredMemberAppearsInTheList() throws Exception {
		mockMvc.perform(post("/api/v1/members")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content(MemberRequests.driverWithTas("Synthetic Driver One", null)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/v1/members/")))
				.andExpect(jsonPath("$.accessRole").value("MEMBER"));

		String list = mockMvc.perform(get("/api/v1/members").header(HttpHeaders.AUTHORIZATION, MockTokens.admin()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode member = named(objectMapper.readTree(list), "Synthetic Driver One");

		assertThat(member.path("rank").asString()).isEqualTo("FIREFIGHTER_1ST_CLASS");
		assertThat(member.path("rosterStatus").asString()).isEqualTo("ACTIVE_ROSTER");
		assertThat(member.path("withinAgeLimit").asBoolean()).isTrue();
		assertThat(member.path("functions").get(0).asString()).isEqualTo("DRIVER");
		assertThat(member.path("licences").get(0).path("licenceClass").asString()).isEqualTo("C");
		JsonNode tas = member.path("certifications").get(0);
		assertThat(tas.path("type").asString()).isEqualTo("TAS");
		assertThat(tas.path("name").asString()).isEqualTo("Tripulante de Ambulância de Socorro");
		assertThat(tas.path("issuedOn").asString()).isEqualTo("2024-04-01");
		assertThat(tas.path("expiresOn").asString()).isEqualTo("2027-03-31");
	}

	@Test
	@DisplayName("TC-SCRUM-77-02 a certification that expires before it was issued is refused with 400, the field and a code, and nothing is saved")
	void certificationExpiringBeforeIssueIsRefused() throws Exception {
		Map<String, Long> before = rowCounts();
		String body = MemberRequests.driverWithTas("Synthetic Driver Two", null)
				.replace("\"expiresOn\": \"2027-03-31\"", "\"expiresOn\": \"2023-12-31\"");

		mockMvc.perform(post("/api/v1/members")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("certifications[0].expiresOn"))
				.andExpect(jsonPath("$.errors[0].code").value("expires_before_issued"));

		assertThat(rowCounts()).isEqualTo(before);
	}

	@Test
	@DisplayName("TC-SCRUM-77-02 editing the certifications of a member with an expiry before the issue date is refused and the member keeps the old ones")
	void certificationEditExpiringBeforeIssueIsRefused() throws Exception {
		UUID id = MemberRequests.register(mockMvc, objectMapper, MemberRequests.driverWithTas("Synthetic Driver Three", null));
		Map<String, Long> before = rowCounts();

		mockMvc.perform(put("/api/v1/members/" + id + "/certifications")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content(MemberRequests.qualifications("2025-05-01", "2025-04-30")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("certifications[0].expiresOn"))
				.andExpect(jsonPath("$.errors[0].code").value("expires_before_issued"));

		assertThat(rowCounts()).isEqualTo(before);
		assertThat(jdbc.queryForObject("SELECT expires_on::text FROM member_certification WHERE member_id = ?", String.class, id))
				.isEqualTo("2027-03-31");
	}

	@Test
	@DisplayName("the admin renews a certification: the new dates replace the old ones, with no history")
	void renewalReplacesTheCertification() throws Exception {
		UUID id = MemberRequests.register(mockMvc, objectMapper, MemberRequests.driverWithTas("Synthetic Driver Four", null));

		mockMvc.perform(put("/api/v1/members/" + id + "/certifications")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content(MemberRequests.qualifications("2027-03-01", "2030-02-28")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.certifications.length()").value(1))
				.andExpect(jsonPath("$.certifications[0].expiresOn").value("2030-02-28"));
	}

	@Test
	@DisplayName("a driver without a driving licence is refused with 400")
	void driverWithoutLicenceIsRefused() throws Exception {
		String body = MemberRequests.driverWithTas("Synthetic Driver Five", null)
				.replace("\"licences\": [{\"licenceClass\": \"C\", \"expiresOn\": \"2030-06-30\"}]", "\"licences\": []");

		mockMvc.perform(post("/api/v1/members")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("licences"))
				.andExpect(jsonPath("$.errors[0].code").value("driver_without_licence"));
	}

	@Test
	@DisplayName("missing name, unknown certification type and two certifications of one type are refused with a code per field, never the value")
	void invalidFieldsAreRefusedWithoutTheirValues(CapturedOutput output) throws Exception {
		String body = """
				{
				  "fullName": " ",
				  "rank": "FIREFIGHTER_1ST_CLASS",
				  "rosterStatus": "ACTIVE_ROSTER",
				  "withinAgeLimit": true,
				  "functions": ["CREW_MEMBER"],
				  "licences": [],
				  "certifications": [
				    {"type": "XYZ-MARKER-77", "issuedOn": "2024-01-01", "expiresOn": "2026-01-01"},
				    {"type": "TAT", "issuedOn": "2024-01-01", "expiresOn": "2026-01-01"},
				    {"type": "TAT", "issuedOn": "2025-01-01", "expiresOn": "2027-01-01"}
				  ]
				}
				""";

		String response = mockMvc.perform(post("/api/v1/members")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andReturn().getResponse().getContentAsString();

		JsonNode errors = objectMapper.readTree(response).path("errors");
		assertThat(errors.findValuesAsString("field"))
				.contains("fullName", "certifications[0].type", "certifications[2].type");
		assertThat(errors.findValuesAsString("code")).contains("required", "unknown_certification_type", "duplicate");
		assertThat(response).doesNotContain("XYZ-MARKER-77");
		assertThat(output.getAll()).doesNotContain("XYZ-MARKER-77");
	}

	@Test
	@DisplayName("the seeded synthetic users have a rank and a roster status, member and supervisor a function, and none a contact")
	void seededUsersHaveDetails() {
		List<Map<String, Object>> seeded = jdbc.queryForList(
				"SELECT rank, roster_status, contact FROM member WHERE id IN (?, ?, ?)",
				MockTokens.MEMBER_ID, MockTokens.SUPERVISOR_ID, MockTokens.ADMIN_ID);
		assertThat(seeded).hasSize(3).allSatisfy(row -> {
			assertThat(row.get("rank")).isNotNull();
			assertThat(row.get("roster_status")).isNotNull();
			assertThat(row.get("contact")).isNull();
		});
		assertThat(jdbc.queryForObject("SELECT count(*) FROM member_function WHERE member_id IN (?, ?, ?)", Long.class,
				MockTokens.MEMBER_ID, MockTokens.SUPERVISOR_ID, MockTokens.ADMIN_ID)).isEqualTo(2);
	}

	@Test
	@DisplayName("TC-SCRUM-77-09 saving a member's edit form unchanged (PUT details and certifications with the data the GET returned) changes nothing")
	void unchangedEditChangesNothing() throws Exception {
		String location = mockMvc.perform(post("/api/v1/members")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content(MemberRequests.driverWithTas("Synthetic Driver Unchanged", "synthetic.unchanged@example.test")))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		String before = mockMvc.perform(get(location).header(HttpHeaders.AUTHORIZATION, MockTokens.admin()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		List<Map<String, Object>> rowsBefore = memberRows(location);
		JsonNode member = objectMapper.readTree(before);

		ObjectNode details = objectMapper.createObjectNode();
		for (String field : List.of("fullName", "rank", "rosterStatus", "withinAgeLimit", "contact", "functions")) {
			details.set(field, member.get(field));
		}
		ObjectNode qualifications = objectMapper.createObjectNode();
		qualifications.set("licences", member.get("licences"));
		ArrayNode certifications = qualifications.putArray("certifications");
		member.get("certifications").forEach(certification -> certifications.addObject()
				.put("type", certification.path("type").asString())
				.put("issuedOn", certification.path("issuedOn").asString())
				.put("expiresOn", certification.path("expiresOn").asString()));

		mockMvc.perform(put(location).header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON).content(details.toString()))
				.andExpect(status().isOk());
		mockMvc.perform(put(location + "/certifications").header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON).content(qualifications.toString()))
				.andExpect(status().isOk());

		String after = mockMvc.perform(get(location).header(HttpHeaders.AUTHORIZATION, MockTokens.admin()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(objectMapper.readTree(after)).isEqualTo(member);
		assertThat(memberRows(location)).isEqualTo(rowsBefore);
	}

	/** The member's rows in the four member tables, without generated row IDs. */
	private List<Map<String, Object>> memberRows(String location) {
		String id = location.substring(location.lastIndexOf('/') + 1);
		List<Map<String, Object>> rows = new java.util.ArrayList<>();
		rows.addAll(jdbc.queryForList("SELECT full_name, rank, roster_status, within_age_limit, contact, access_role, active FROM member WHERE id = ?::uuid", id));
		rows.addAll(jdbc.queryForList("SELECT service_function FROM member_function WHERE member_id = ?::uuid ORDER BY 1", id));
		rows.addAll(jdbc.queryForList("SELECT licence_class, expires_on FROM member_driving_licence WHERE member_id = ?::uuid ORDER BY 1", id));
		rows.addAll(jdbc.queryForList("SELECT type_code, issued_on, expires_on FROM member_certification WHERE member_id = ?::uuid ORDER BY 1", id));
		return rows;
	}

	private JsonNode named(JsonNode members, String fullName) {
		for (JsonNode member : members) {
			if (fullName.equals(member.path("fullName").asString())) {
				return member;
			}
		}
		throw new AssertionError(fullName + " is not in the members list");
	}

	private Map<String, Long> rowCounts() {
		return Map.of(
				"member", count("member"),
				"member_function", count("member_function"),
				"member_certification", count("member_certification"),
				"member_driving_licence", count("member_driving_licence"));
	}

	private long count(String table) {
		return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
	}
}
