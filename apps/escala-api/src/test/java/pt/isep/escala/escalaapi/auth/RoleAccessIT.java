package pt.isep.escala.escalaapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** The mocked sign-in and the role check on the server (SCRUM-63, ADR 0006, access-control matrix on 09). */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class RoleAccessIT {

	static final UUID INACTIVE_ID = UUID.fromString("00000000-0000-4000-8000-0000000000ff");

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ObjectMapper objectMapper;

	@BeforeEach
	void inactiveMember() {
		jdbc.update("""
				INSERT INTO member (id, full_name, access_role, active) VALUES (?, 'Synthetic Inactive', 'SUPERVISOR', false)
				ON CONFLICT (id) DO NOTHING
				""", INACTIVE_ID);
	}

	@Test
	@DisplayName("TC-SCRUM-63-02 a member calling the supervisor-only POST /api/v1/piquetes gets 403 and nothing changes")
	void memberCannotCreatePiquete() throws Exception {
		Map<String, Long> before = rowCounts();

		mockMvc.perform(createPiquete().header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isForbidden());

		assertThat(rowCounts()).isEqualTo(before);
	}

	@Test
	@DisplayName("TC-SCRUM-63-02 the admin cannot build the schedule either (09: admin ❌ on build and publish): 403, nothing changes")
	void adminCannotCreatePiquete() throws Exception {
		Map<String, Long> before = rowCounts();

		mockMvc.perform(createPiquete().header(HttpHeaders.AUTHORIZATION, MockTokens.admin()))
				.andExpect(status().isForbidden());

		assertThat(rowCounts()).isEqualTo(before);
	}

	@Test
	@DisplayName("the supervisor creates a piquete, and every role reads the schedule")
	void supervisorCreatesAndEveryRoleReads() throws Exception {
		mockMvc.perform(createPiquete().header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isCreated());

		for (String token : List.of(MockTokens.member(), MockTokens.supervisor(), MockTokens.admin())) {
			mockMvc.perform(get("/api/v1/piquetes").header(HttpHeaders.AUTHORIZATION, token))
					.andExpect(status().isOk());
		}
	}

	@Test
	@DisplayName("TC-SCRUM-63-03 without a token, the business endpoints answer 401 and nothing changes")
	void noTokenIsUnauthorized() throws Exception {
		Map<String, Long> before = rowCounts();

		mockMvc.perform(get("/api/v1/piquetes")).andExpect(status().isUnauthorized());
		mockMvc.perform(createPiquete()).andExpect(status().isUnauthorized());

		assertThat(rowCounts()).isEqualTo(before);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {
			"Bearer not-a-mock-token",
			"Bearer mock.%%%",
			"Bearer mock.bm90IGpzb24",
			"Basic c3VwZXJ2aXNvcjpzZWNyZXQ"})
	@DisplayName("TC-SCRUM-63-03 a token that cannot be read answers 401")
	void unreadableTokenIsUnauthorized(String authorization) throws Exception {
		mockMvc.perform(get("/api/v1/piquetes").header(HttpHeaders.AUTHORIZATION, authorization))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("TC-SCRUM-63-03 a token of an unknown or inactive member, or with another role than the member's, answers 401")
	void tokenNotMatchingAnActiveMemberIsUnauthorized() throws Exception {
		Map<String, Long> before = rowCounts();

		mockMvc.perform(createPiquete().header(HttpHeaders.AUTHORIZATION, MockTokens.bearer(UUID.randomUUID(), "SUPERVISOR")))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(createPiquete().header(HttpHeaders.AUTHORIZATION, MockTokens.bearer(INACTIVE_ID, "SUPERVISOR")))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(createPiquete()
						.header(HttpHeaders.AUTHORIZATION, MockTokens.bearer(MockTokens.MEMBER_ID, "SUPERVISOR")))
				.andExpect(status().isUnauthorized());

		assertThat(rowCounts()).isEqualTo(before);
	}

	@Test
	@DisplayName("TC-SCRUM-63-03 mock-sign-in with an unknown or inactive memberId answers 401 and issues no token")
	void signInOfUnknownOrInactiveMemberIsUnauthorized() throws Exception {
		for (UUID memberId : List.of(UUID.randomUUID(), INACTIVE_ID)) {
			String body = mockMvc.perform(post("/api/v1/auth/mock-sign-in")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"memberId\":\"" + memberId + "\"}"))
					.andExpect(status().isUnauthorized())
					.andReturn().getResponse().getContentAsString();
			assertThat(body).doesNotContain("mock.").doesNotContain("token");
		}
	}

	@Test
	@DisplayName("TC-SCRUM-63-04 without a token, the health endpoints answer as before")
	void healthIsOpen() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("the sign-in screen lists the three active seeded users, without contacts, and never an inactive one")
	void mockUsersListsActiveSeededUsers() throws Exception {
		String body = mockMvc.perform(get("/api/v1/auth/mock-users"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode users = objectMapper.readTree(body);

		assertThat(users.findValuesAsString("id")).containsExactlyInAnyOrder(
				MockTokens.MEMBER_ID.toString(), MockTokens.SUPERVISOR_ID.toString(), MockTokens.ADMIN_ID.toString());
		assertThat(users.findValuesAsString("role")).containsExactlyInAnyOrder("MEMBER", "SUPERVISOR", "ADMIN");
		for (JsonNode user : users) {
			assertThat(user.propertyNames()).containsExactlyInAnyOrder("id", "fullName", "role");
		}
	}

	@Test
	@DisplayName("mock-sign-in of the supervisor issues a token that the API accepts for the supervisor-only endpoint")
	void signInIssuesAWorkingToken() throws Exception {
		String body = mockMvc.perform(post("/api/v1/auth/mock-sign-in")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"memberId\":\"" + MockTokens.SUPERVISOR_ID + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberId").value(MockTokens.SUPERVISOR_ID.toString()))
				.andExpect(jsonPath("$.role").value("SUPERVISOR"))
				.andExpect(jsonPath("$.fullName").value("Synthetic Supervisor"))
				.andReturn().getResponse().getContentAsString();
		String token = objectMapper.readTree(body).path("token").asText();

		mockMvc.perform(createPiquete().header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("the bearer token is never written to the logs")
	void tokenIsNotLogged(CapturedOutput output) throws Exception {
		String token = MockTokens.token(MockTokens.SUPERVISOR_ID, "SUPERVISOR");
		mockMvc.perform(createPiquete().header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/v1/piquetes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token + "x"))
				.andExpect(status().isUnauthorized());

		assertThat(output.getAll()).doesNotContain(token.substring("mock.".length()));
	}

	private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createPiquete() {
		return post("/api/v1/piquetes").contentType(MediaType.APPLICATION_JSON).content("{\"dutyDate\":\"2026-12-01\"}");
	}

	private Map<String, Long> rowCounts() {
		return Map.of(
				"piquete", count("piquete"),
				"slot", count("slot"),
				"outbox_event", count("outbox_event"));
	}

	private long count(String table) {
		return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
	}
}
