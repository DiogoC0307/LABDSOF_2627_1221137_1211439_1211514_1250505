package pt.isep.escala.escalaapi.unavailability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;
import tools.jackson.databind.ObjectMapper;

/** A member declares when they cannot serve, by form (SCRUM-64, RF12, RN06, RNF02). The clock is fixed. */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
class UnavailabilityIT {

	/** Sunday 2026-11-22, 10:00 UTC. */
	static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

	static final UUID OTHER_MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000064");

	@TestConfiguration
	static class FixedClock {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}
	}

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

	@Autowired
	UnavailabilityRetention retention;

	@BeforeEach
	void otherMember() {
		jdbc.update("""
				INSERT INTO member (id, full_name, access_role) VALUES (?, 'Synthetic Other Member', 'MEMBER')
				ON CONFLICT (id) DO NOTHING
				""", OTHER_MEMBER_ID);
	}

	@Test
	@DisplayName("TC-SCRUM-64-01 a period with a start and an end is saved (201 and Location) and appears in the member's list")
	void savedPeriodAppearsInTheList() throws Exception {
		String body = mockMvc.perform(declare(MockTokens.member(), "2026-12-05T00:00:00+00:00", "2026-12-08T00:00:00+00:00"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/v1/unavailabilities/")))
				.andExpect(jsonPath("$.startsAt").value("2026-12-05T00:00:00Z"))
				.andExpect(jsonPath("$.endsAt").value("2026-12-08T00:00:00Z"))
				.andExpect(jsonPath("$.source").value("FORM"))
				.andReturn().getResponse().getContentAsString();
		String id = objectMapper.readTree(body).path("id").asString();

		mockMvc.perform(get("/api/v1/unavailabilities").header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '" + id + "')].startsAt").value("2026-12-05T00:00:00Z"));
		assertThat(jdbc.queryForObject("SELECT member_id FROM unavailability WHERE id = ?::uuid", UUID.class, id))
				.isEqualTo(MockTokens.MEMBER_ID);
	}

	@Test
	@DisplayName("TC-SCRUM-64-01 a time with an offset is stored as the same instant in UTC")
	void offsetsAreStoredInUtc() throws Exception {
		mockMvc.perform(declare(MockTokens.member(), "2027-05-01T08:00:00+01:00", "2027-05-01T20:00:00+01:00"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.startsAt").value("2027-05-01T07:00:00Z"))
				.andExpect(jsonPath("$.endsAt").value("2027-05-01T19:00:00Z"));
	}

	@Test
	@DisplayName("TC-SCRUM-64-02 an end before the start (or equal to it) is refused with 400 and nothing is saved")
	void endBeforeStartIsRefused() throws Exception {
		long before = count();

		mockMvc.perform(declare(MockTokens.member(), "2026-12-10T00:00:00Z", "2026-12-09T00:00:00Z"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("endsAt"))
				.andExpect(jsonPath("$.errors[0].code").value("ends_before_start"));
		mockMvc.perform(declare(MockTokens.member(), "2026-12-10T00:00:00Z", "2026-12-10T00:00:00Z"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].code").value("ends_before_start"));

		assertThat(count()).isEqualTo(before);
	}

	@Test
	@DisplayName("a period that has already ended, or that starts more than 6 months ahead, is refused with 400")
	void pastAndFarAheadAreRefused() throws Exception {
		long before = count();

		mockMvc.perform(declare(MockTokens.member(), "2026-11-20T00:00:00Z", "2026-11-22T10:00:00Z"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("endsAt"))
				.andExpect(jsonPath("$.errors[0].code").value("in_the_past"));
		mockMvc.perform(declare(MockTokens.member(), "2027-05-22T10:00:01Z", "2027-05-23T00:00:00Z"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("startsAt"))
				.andExpect(jsonPath("$.errors[0].code").value("too_far_ahead"));
		mockMvc.perform(post("/api/v1/unavailabilities")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.member())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].code").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("required"))));

		assertThat(count()).isEqualTo(before);
	}

	@Test
	@DisplayName("a period that started earlier and has not ended is accepted, and overlapping periods are kept as they are")
	void ongoingAndOverlappingPeriodsAreAccepted() throws Exception {
		mockMvc.perform(declare(MockTokens.supervisor(), "2026-11-21T00:00:00Z", "2026-11-25T00:00:00Z"))
				.andExpect(status().isCreated());
		mockMvc.perform(declare(MockTokens.supervisor(), "2026-11-24T00:00:00Z", "2026-11-27T00:00:00Z"))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/unavailabilities").header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	@DisplayName("TC-SCRUM-64-03 saving a period answers in under 500 ms (p95 of 30 requests, RNF02)")
	void savingAnswersInUnder500Ms() throws Exception {
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(declare(MockTokens.admin(), "2026-12-0" + (i + 1) + "T08:00:00Z", "2026-12-0" + (i + 1) + "T20:00:00Z"))
					.andExpect(status().isCreated());
		}
		List<Long> millis = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			long start = System.nanoTime();
			mockMvc.perform(declare(MockTokens.admin(), "2026-12-15T08:00:00Z", "2026-12-15T20:00:00Z"))
					.andExpect(status().isCreated());
			millis.add((System.nanoTime() - start) / 1_000_000);
		}
		millis.sort(null);
		long p95 = millis.get((int) Math.ceil(0.95 * millis.size()) - 1);

		assertThat(p95).as("p95 in ms of %s", millis).isLessThan(500);
	}

	@Test
	@DisplayName("TC-SCRUM-64-04 a member cannot read or delete another member's period: 404 and nothing changes")
	void anotherMembersPeriodIsNotFound() throws Exception {
		String body = mockMvc.perform(declare(MockTokens.bearer(OTHER_MEMBER_ID, "MEMBER"), "2026-12-20T00:00:00Z", "2026-12-21T00:00:00Z"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String id = objectMapper.readTree(body).path("id").asString();

		mockMvc.perform(get("/api/v1/unavailabilities/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/v1/unavailabilities/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isNotFound());
		String mine = mockMvc.perform(get("/api/v1/unavailabilities").header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(mine).doesNotContain(id);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM unavailability WHERE id = ?::uuid", Long.class, id)).isEqualTo(1);
	}

	@Test
	@DisplayName("a member reads and deletes their own period (204); changing it is delete and declare again")
	void memberDeletesTheirOwnPeriod() throws Exception {
		String body = mockMvc.perform(declare(MockTokens.member(), "2026-12-24T00:00:00Z", "2026-12-26T00:00:00Z"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String id = objectMapper.readTree(body).path("id").asString();

		mockMvc.perform(get("/api/v1/unavailabilities/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/v1/unavailabilities/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/unavailabilities/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("without a token the unavailability endpoints answer 401")
	void noTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/unavailabilities")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/unavailabilities").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("the daily job deletes the periods that ended more than 30 days ago, and keeps the others (09)")
	void retentionDeletesPeriodsEnded30DaysAgo() {
		UUID old = insertPeriod("2026-10-01T00:00:00Z", "2026-10-23T09:59:59Z");
		UUID recent = insertPeriod("2026-10-01T00:00:00Z", "2026-10-23T10:00:01Z");

		retention.deleteExpired();

		assertThat(exists(old)).isFalse();
		assertThat(exists(recent)).isTrue();
	}

	@Test
	@DisplayName("SCRUM-65 the supervisor reads every member's periods that overlap the week, with who and when only")
	void supervisorReadsTheTeamsPeriodsOfAWeek() throws Exception {
		// Week 2027-W02: Monday 11 to Sunday 17 January 2027 (Lisbon is on UTC in winter)
		insertPeriod("2027-01-12T08:00:00Z", "2027-01-12T20:00:00Z"); // inside the week
		insertPeriod(OTHER_MEMBER_ID, "2027-01-17T22:00:00Z", "2027-01-18T06:00:00Z"); // Sunday night into Monday
		insertPeriod(OTHER_MEMBER_ID, "2027-01-19T00:00:00Z", "2027-01-20T00:00:00Z"); // after the Monday that follows
		insertPeriod("2027-01-09T00:00:00Z", "2027-01-11T00:00:00Z"); // ends as the week starts

		mockMvc.perform(get("/api/v1/unavailabilities/team").param("week", "2027-W02")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].memberId").value(MockTokens.MEMBER_ID.toString()))
				.andExpect(jsonPath("$[0].startsAt").value("2027-01-12T08:00:00Z"))
				.andExpect(jsonPath("$[0].endsAt").value("2027-01-12T20:00:00Z"))
				.andExpect(jsonPath("$[1].memberId").value(OTHER_MEMBER_ID.toString()))
				.andExpect(jsonPath("$[1].startsAt").value("2027-01-17T22:00:00Z"))
				.andExpect(jsonPath("$[0].id").doesNotExist())
				.andExpect(jsonPath("$[0].source").doesNotExist());
	}

	@Test
	@DisplayName("SCRUM-65 only the supervisor reads the team's periods (403 for a member and the admin), for a week that exists (400)")
	void onlyTheSupervisorReadsTheTeamsPeriods() throws Exception {
		mockMvc.perform(get("/api/v1/unavailabilities/team").param("week", "2027-W02")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/unavailabilities/team").param("week", "2027-W02")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.admin()))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/unavailabilities/team").param("week", "2025-W54")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("week"));
		mockMvc.perform(get("/api/v1/unavailabilities/team").header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/unavailabilities/team").param("week", "2027-W02"))
				.andExpect(status().isUnauthorized());
	}

	private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder declare(String token,
			String startsAt, String endsAt) {
		return post("/api/v1/unavailabilities")
				.header(HttpHeaders.AUTHORIZATION, token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"startsAt\":\"" + startsAt + "\",\"endsAt\":\"" + endsAt + "\"}");
	}

	private UUID insertPeriod(String startsAt, String endsAt) {
		return insertPeriod(MockTokens.MEMBER_ID, startsAt, endsAt);
	}

	private UUID insertPeriod(UUID memberId, String startsAt, String endsAt) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO unavailability (id, member_id, starts_at, ends_at) VALUES (?, ?, ?::timestamptz, ?::timestamptz)",
				id, memberId, startsAt, endsAt);
		return id;
	}

	private boolean exists(UUID id) {
		return jdbc.queryForObject("SELECT count(*) FROM unavailability WHERE id = ?", Long.class, id) == 1;
	}

	private long count() {
		return jdbc.queryForObject("SELECT count(*) FROM unavailability", Long.class);
	}
}
