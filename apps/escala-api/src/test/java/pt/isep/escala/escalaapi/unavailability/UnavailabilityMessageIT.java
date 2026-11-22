package pt.isep.escala.escalaapi.unavailability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * A member says in their own words when they cannot serve, sees what was read automatically and confirms it
 * (SCRUM-75, escala-api side: RF13, RF31 to RF33, RNF09, 09 T-11). The worker is replaced by ExtracaoConcluida
 * messages sent by the test. The clock is fixed and moved by the tests.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class UnavailabilityMessageIT {

	/** Sunday 2026-11-22, 10:00 UTC (10:00 in Lisbon). */
	static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

	static final String PATH = "/api/v1/unavailability-messages";

	static final String SATURDAY_MORNING = """
			[{"startDate":"2026-11-28","endDate":"2026-11-28","startTime":"08:00","endTime":"13:00"}]""";

	static final UUID OTHER_MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000075");

	static final MovableClock CLOCK = new MovableClock(NOW);

	@TestConfiguration
	static class Clocks {

		@Bean
		@Primary
		Clock movableClock() {
			return CLOCK;
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
	RabbitTemplate rabbitTemplate;

	@Autowired
	UnavailabilityMessageRetention retention;

	@BeforeEach
	void clean() {
		CLOCK.set(NOW);
		jdbc.update("DELETE FROM unavailability_message");
		jdbc.update("DELETE FROM unavailability");
		jdbc.update("""
				INSERT INTO member (id, full_name, access_role) VALUES (?, 'Synthetic Other Member', 'MEMBER')
				ON CONFLICT (id) DO NOTHING
				""", OTHER_MEMBER_ID);
	}

	@Test
	@DisplayName("TC-SCRUM-75-01 \"não posso sábado de manhã\" answers 202, the proposal (Saturday, morning) comes back to the member, and nothing is saved until they confirm")
	void theProposalIsShownAndNothingIsSavedUntilConfirmed() throws Exception {
		UUID id = sendText(MockTokens.member(), "não posso sábado de manhã");
		mockMvc.perform(get(PATH + "/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.periods.length()").value(0));

		answer(id, SATURDAY_MORNING, "MODEL");

		mockMvc.perform(get(PATH + "/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PROPOSED"))
				.andExpect(jsonPath("$.method").value("MODEL"))
				.andExpect(jsonPath("$.referenceDate").value("2026-11-22"))
				.andExpect(jsonPath("$.periods[0].startDate").value("2026-11-28"))
				.andExpect(jsonPath("$.periods[0].endDate").value("2026-11-28"))
				.andExpect(jsonPath("$.periods[0].startTime").value("08:00"))
				.andExpect(jsonPath("$.periods[0].endTime").value("13:00"))
				.andExpect(jsonPath("$.text").doesNotExist());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM unavailability", Integer.class)).isZero();
	}

	@Test
	@DisplayName("TC-SCRUM-75-02 ExtracaoPedida goes through the outbox with the fields of its schema: the text and the reference date in Lisbon, never the member")
	void theRequestGoesThroughTheOutbox() throws Exception {
		CLOCK.set(Instant.parse("2027-06-30T23:30:00Z")); // already 1 July in Lisbon (UTC+1 in summer)
		UUID id = sendText(MockTokens.member(), "Synthetic text 75-02");

		JsonNode event = objectMapper.readTree(jdbc.queryForObject(
				"SELECT o.payload FROM outbox_event o JOIN unavailability_message m ON o.id = m.request_event_id WHERE m.id = ?",
				String.class, id));
		assertThat(jdbc.queryForObject(
				"SELECT o.routing_key FROM outbox_event o JOIN unavailability_message m ON o.id = m.request_event_id WHERE m.id = ?",
				String.class, id)).isEqualTo("extracao.pedida");
		assertThat(fieldNames(event)).containsExactlyInAnyOrder("eventId", "type", "schemaVersion", "occurredAt",
				"traceId", "unavailabilityRequestId", "freeText", "referenceDate");
		assertThat(event.path("type").asString()).isEqualTo("ExtracaoPedida");
		assertThat(event.path("schemaVersion").asInt()).isEqualTo(1);
		assertThat(event.path("unavailabilityRequestId").asString()).isEqualTo(id.toString());
		assertThat(event.path("freeText").asString()).isEqualTo("Synthetic text 75-02");
		assertThat(event.path("referenceDate").asString()).isEqualTo("2027-07-01");
		assertThat(event.toString()).doesNotContain(MockTokens.MEMBER_ID.toString());
	}

	@Test
	@DisplayName("TC-SCRUM-75-03 no text, blank text or more than 500 characters answers 400 with errors: [{field, code}] and nothing is stored")
	void theTextIsValidated() throws Exception {
		postText(MockTokens.member(), "{}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("text"))
				.andExpect(jsonPath("$.errors[0].code").value("required"));
		postText(MockTokens.member(), "{\"text\":\"   \"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].code").value("required"));
		String tooLong = "x".repeat(501);
		String body = postText(MockTokens.member(), "{\"text\":\"" + tooLong + "\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("text"))
				.andExpect(jsonPath("$.errors[0].code").value("too_long"))
				.andReturn().getResponse().getContentAsString();
		assertThat(body).doesNotContain(tooLong);
		postText(MockTokens.member(), "{\"text\":\"" + "x".repeat(500) + "\"}").andExpect(status().isAccepted());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM unavailability_message", Integer.class)).isEqualTo(1);
	}

	@Test
	@DisplayName("TC-SCRUM-75-04 on confirmation the period is saved, and the free text is deleted from the message and from the outbox in the same transaction")
	void confirmingDeletesTheFreeText() throws Exception {
		String marker = "MARKER-75-04 não posso sábado de manhã";
		UUID id = sendText(MockTokens.member(), marker);
		answer(id, SATURDAY_MORNING, "MODEL");

		confirm(MockTokens.member(), id, SATURDAY_MORNING)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].startsAt").value("2026-11-28T08:00:00Z"))
				.andExpect(jsonPath("$[0].endsAt").value("2026-11-28T13:00:00Z"))
				.andExpect(jsonPath("$[0].source").value("FREE_TEXT"));

		Map<String, Object> row = jdbc.queryForMap("SELECT free_text, status FROM unavailability_message WHERE id = ?", id);
		assertThat(row).containsEntry("free_text", null).containsEntry("status", "CONFIRMED");
		assertThat(tablesContaining("MARKER-75-04")).isEmpty();
		mockMvc.perform(get("/api/v1/unavailabilities").header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(jsonPath("$[0].source").value("FREE_TEXT"));
	}

	@Test
	@DisplayName("TC-SCRUM-75-05 only the author reads, confirms or discards a message: another member gets 404, no token 401")
	void onlyTheAuthor() throws Exception {
		UUID id = sendText(MockTokens.member(), "Synthetic text 75-05");
		answer(id, SATURDAY_MORNING, "MODEL");
		String other = MockTokens.bearer(OTHER_MEMBER_ID, "MEMBER");

		mockMvc.perform(get(PATH + "/" + id).header(HttpHeaders.AUTHORIZATION, other)).andExpect(status().isNotFound());
		confirm(other, id, SATURDAY_MORNING).andExpect(status().isNotFound());
		mockMvc.perform(delete(PATH + "/" + id).header(HttpHeaders.AUTHORIZATION, other)).andExpect(status().isNotFound());
		mockMvc.perform(get(PATH + "/" + id)).andExpect(status().isUnauthorized());
		mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"x\"}"))
				.andExpect(status().isUnauthorized());

		assertThat(jdbc.queryForObject("SELECT status FROM unavailability_message WHERE id = ?", String.class, id))
				.isEqualTo("PROPOSED");
		for (String token : new String[] {MockTokens.supervisor(), MockTokens.admin()}) {
			postText(token, "{\"text\":\"Synthetic text 75-05 other role\"}").andExpect(status().isAccepted());
		}
	}

	@Test
	@DisplayName("TC-SCRUM-75-06 the 11th message within an hour answers 429 with Retry-After, and an hour after the first one a message is accepted again")
	void tenMessagesPerHour() throws Exception {
		for (int i = 0; i < 10; i++) {
			sendText(MockTokens.member(), "Synthetic text 75-06 " + i);
			CLOCK.move(Duration.ofMinutes(5));
		}
		// 10:50: the first message was at 10:00.
		postText(MockTokens.member(), "{\"text\":\"Synthetic text 75-06 eleventh\"}")
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, "600"))
				.andExpect(jsonPath("$.status").value(429));
		postText(MockTokens.supervisor(), "{\"text\":\"Synthetic text 75-06 another member\"}")
				.andExpect(status().isAccepted());

		CLOCK.set(NOW.plus(Duration.ofHours(1)));
		postText(MockTokens.member(), "{\"text\":\"Synthetic text 75-06 an hour later\"}").andExpect(status().isAccepted());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM unavailability_message WHERE member_id = ?",
				Integer.class, MockTokens.MEMBER_ID)).isEqualTo(11);
	}

	@Test
	@DisplayName("TC-SCRUM-75-07 the job deletes the free text of messages not confirmed within 24 hours, with their outbox rows, and logs only the count")
	void unconfirmedTextIsDeletedAfter24Hours(CapturedOutput output) throws Exception {
		UUID old = sendText(MockTokens.member(), "MARKER-75-07 old pending");
		UUID oldProposed = sendText(MockTokens.member(), "MARKER-75-07 old proposed");
		answer(oldProposed, SATURDAY_MORNING, "MODEL");
		CLOCK.move(Duration.ofHours(23));
		UUID recent = sendText(MockTokens.member(), "Synthetic text 75-07 recent");

		CLOCK.move(Duration.ofHours(1).plusSeconds(1)); // 24 h and 1 s after the first two
		retention.expire();

		for (UUID id : List.of(old, oldProposed)) {
			Map<String, Object> row = jdbc.queryForMap("SELECT free_text, status FROM unavailability_message WHERE id = ?", id);
			assertThat(row).containsEntry("free_text", null).containsEntry("status", "EXPIRED");
		}
		assertThat(jdbc.queryForObject("SELECT free_text FROM unavailability_message WHERE id = ?", String.class, recent))
				.isEqualTo("Synthetic text 75-07 recent");
		assertThat(tablesContaining("MARKER-75-07")).isEmpty();
		assertThat(output.getAll()).contains("unavailability messages expired count=2").doesNotContain("MARKER-75-07");
		confirm(MockTokens.member(), oldProposed, SATURDAY_MORNING).andExpect(status().isConflict());
	}

	@Test
	@DisplayName("TC-SCRUM-75-08 discarding answers 204, deletes the free text and its outbox row, and the message can no longer be confirmed")
	void discarding() throws Exception {
		UUID pending = sendText(MockTokens.member(), "MARKER-75-08 pending");
		UUID proposed = sendText(MockTokens.member(), "MARKER-75-08 proposed");
		answer(proposed, SATURDAY_MORNING, "MODEL");

		for (UUID id : List.of(pending, proposed)) {
			mockMvc.perform(delete(PATH + "/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
					.andExpect(status().isNoContent());
			assertThat(jdbc.queryForObject("SELECT status FROM unavailability_message WHERE id = ?", String.class, id))
					.isEqualTo("DISCARDED");
		}
		assertThat(tablesContaining("MARKER-75-08")).isEmpty();
		confirm(MockTokens.member(), proposed, SATURDAY_MORNING).andExpect(status().isConflict());
		mockMvc.perform(delete(PATH + "/" + proposed).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isNoContent());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM unavailability", Integer.class)).isZero();
	}

	@Test
	@DisplayName("TC-SCRUM-75-09 the same ExtracaoConcluida twice is applied once, and a later answer does not replace the proposal")
	void theAnswerIsAppliedOnce() throws Exception {
		UUID id = sendText(MockTokens.member(), "Synthetic text 75-09");
		UUID eventId = UUID.randomUUID();
		sendConcluida(eventId, requestEventId(id), id, SATURDAY_MORNING, "MODEL");
		awaitStatus(id, "PROPOSED");
		sendConcluida(eventId, requestEventId(id), id, "[]", "FALLBACK");
		sendConcluida(UUID.randomUUID(), requestEventId(id), id, "[]", "FALLBACK");

		await().pollDelay(Duration.ofSeconds(2)).until(() -> true);
		assertThat(jdbc.queryForObject("SELECT method FROM unavailability_message WHERE id = ?", String.class, id))
				.isEqualTo("MODEL");
		assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_event WHERE event_id = ?", Integer.class, eventId))
				.isEqualTo(1);
	}

	@Test
	@DisplayName("TC-SCRUM-75-10 an answer for an unknown or discarded message is ignored; a malformed one goes to the DLQ")
	void unknownAndMalformedAnswers() throws Exception {
		UUID discarded = sendText(MockTokens.member(), "Synthetic text 75-10");
		mockMvc.perform(delete(PATH + "/" + discarded).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isNoContent());
		UUID late = UUID.randomUUID();
		sendConcluida(late, requestEventId(discarded), discarded, SATURDAY_MORNING, "MODEL");
		sendConcluida(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SATURDAY_MORNING, "MODEL");
		await().atMost(Duration.ofSeconds(15)).until(() -> jdbc.queryForObject(
				"SELECT count(*) FROM processed_event WHERE event_id = ?", Integer.class, late) == 1);
		assertThat(jdbc.queryForObject("SELECT status FROM unavailability_message WHERE id = ?", String.class, discarded))
				.isEqualTo("DISCARDED");

		UUID wrong = sendText(MockTokens.member(), "Synthetic text 75-10 malformed");
		send("{\"type\":\"ExtracaoConcluida\",\"unavailabilityRequestId\":\"" + wrong + "\"}");
		Message dead = await().atMost(Duration.ofSeconds(15)).ignoreExceptions()
				.until(() -> rabbitTemplate.receive("api.extracao-concluida.dlq", 200), java.util.Objects::nonNull);
		assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).contains(wrong.toString());
		sendConcluida(UUID.randomUUID(), requestEventId(wrong), wrong, "{\"not\":\"a list\"}", "MODEL");
		await().atMost(Duration.ofSeconds(15)).ignoreExceptions()
				.until(() -> rabbitTemplate.receive("api.extracao-concluida.dlq", 200), java.util.Objects::nonNull);
		sendConcluida(UUID.randomUUID(), requestEventId(wrong), wrong, "[]", "GUESS");
		await().atMost(Duration.ofSeconds(15)).ignoreExceptions()
				.until(() -> rabbitTemplate.receive("api.extracao-concluida.dlq", 200), java.util.Objects::nonNull);
		assertThat(jdbc.queryForObject("SELECT status FROM unavailability_message WHERE id = ?", String.class, wrong))
				.isEqualTo("PENDING");
	}

	@Test
	@DisplayName("TC-SCRUM-75-13 periods that cannot be read answer 400 with the field of each period, and nothing is saved")
	void confirmedPeriodsAreValidated() throws Exception {
		UUID id = proposed("Synthetic text 75-13");
		expectInvalid(id, "[]", "periods", "required");
		expectInvalid(id, "[{\"startDate\":\"2026-11-30\",\"endDate\":\"2026-11-29\"}]", "periods[0].endDate", "ends_before_start");
		expectInvalid(id, "[{\"endDate\":\"2026-11-29\"}]", "periods[0].startDate", "required");
		expectInvalid(id, "[{\"startDate\":\"2026-11-29\",\"endDate\":\"2026-11-29\",\"startTime\":\"08:00\"}]",
				"periods[0].endTime", "required");
		expectInvalid(id, "[{\"startDate\":\"2026-11-29\",\"endDate\":\"2026-11-29\",\"startTime\":\"08:00\",\"endTime\":\"08:00\"}]",
				"periods[0].endTime", "ends_before_start");
		expectInvalid(id, "[{\"startDate\":\"2026-11-29\",\"endDate\":\"2026-11-29\",\"startTime\":\"8h\",\"endTime\":\"13:00\"}]",
				"periods[0].startTime", "invalid");
		expectInvalid(id, "[{\"startDate\":\"2026-11-29\",\"endDate\":\"2026-12-05\",\"weekdays\":[\"SUNDAY\"]}]",
				"periods[0].weekdays", "invalid");
		expectInvalid(id, "[{\"startDate\":\"2026-11-30\",\"endDate\":\"2026-12-04\",\"weekdays\":[\"SUN\"]}]",
				"periods[0].weekdays", "no_days");
		String eleven = "[" + String.join(",", java.util.Collections.nCopies(11,
				"{\"startDate\":\"2026-11-29\",\"endDate\":\"2026-11-29\"}")) + "]";
		expectInvalid(id, eleven, "periods", "too_many");
		assertThat(jdbc.queryForObject("SELECT count(*) FROM unavailability", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT status FROM unavailability_message WHERE id = ?", String.class, id))
				.isEqualTo("PROPOSED");
	}

	@Test
	@DisplayName("TC-SCRUM-75-14 a period that has ended or reaches more than 6 months ahead rejects the whole confirmation; a period running now keeps only what has not ended")
	void theWindowIsChecked() throws Exception {
		UUID id = proposed("Synthetic text 75-14");
		String ended = "{\"startDate\":\"2026-11-20\",\"endDate\":\"2026-11-21\"}";
		String fine = "{\"startDate\":\"2026-11-28\",\"endDate\":\"2026-11-28\"}";
		expectInvalid(id, "[" + fine + "," + ended + "]", "periods[1].endDate", "in_the_past");
		expectInvalid(id, "[{\"startDate\":\"2027-05-23\",\"endDate\":\"2027-05-23\"}]", "periods[0].startDate", "too_far_ahead");
		expectInvalid(id, "[{\"startDate\":\"2027-05-01\",\"endDate\":\"2027-06-01\"}]", "periods[0].endDate", "too_far_ahead");
		assertThat(jdbc.queryForObject("SELECT count(*) FROM unavailability", Integer.class)).isZero();

		// Every day from Friday 20th to Tuesday 24th, 08:00 to 12:00 in Lisbon; it is Sunday 22nd, 10:00.
		confirm(MockTokens.member(), id,
				"[{\"startDate\":\"2026-11-20\",\"endDate\":\"2026-11-24\",\"startTime\":\"08:00\",\"endTime\":\"12:00\"}]")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].startsAt").value("2026-11-22T08:00:00Z"))
				.andExpect(jsonPath("$[2].endsAt").value("2026-11-24T12:00:00Z"));
		UUID future = proposed("Synthetic text 75-14 limit");
		confirm(MockTokens.member(), future, "[{\"startDate\":\"2027-05-22\",\"endDate\":\"2027-05-22\"}]")
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("TC-SCRUM-75-15 confirming a message that has no proposal yet, or one already confirmed, answers 409 and saves nothing more")
	void confirmOnlyAProposal() throws Exception {
		UUID pending = sendText(MockTokens.member(), "Synthetic text 75-15");
		confirm(MockTokens.member(), pending, SATURDAY_MORNING).andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
		answer(pending, SATURDAY_MORNING, "MODEL");
		confirm(MockTokens.member(), pending, SATURDAY_MORNING).andExpect(status().isOk());
		confirm(MockTokens.member(), pending, SATURDAY_MORNING).andExpect(status().isConflict());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM unavailability", Integer.class)).isEqualTo(1);
	}

	@Test
	@DisplayName("TC-SCRUM-75-16 an empty FALLBACK answer shows the message as answered with no periods, so the PWA shows the form")
	void anEmptyAnswer() throws Exception {
		UUID id = sendText(MockTokens.member(), "Synthetic text 75-16");
		answer(id, "[]", "FALLBACK");
		mockMvc.perform(get(PATH + "/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(jsonPath("$.status").value("PROPOSED"))
				.andExpect(jsonPath("$.method").value("FALLBACK"))
				.andExpect(jsonPath("$.periods.length()").value(0));
	}

	@Test
	@DisplayName("TC-SCRUM-75-17 the free text is never logged: not on sending, on the answer, on confirming nor on a bad request")
	void theTextIsNeverLogged(CapturedOutput output) throws Exception {
		String marker = "MARKER-75-17";
		UUID id = sendText(MockTokens.member(), marker + " não posso domingo");
		answer(id, SATURDAY_MORNING, "MODEL");
		confirm(MockTokens.member(), id, SATURDAY_MORNING).andExpect(status().isOk());
		postText(MockTokens.member(), "{\"text\":\"" + marker + "x".repeat(500) + "\"}").andExpect(status().isBadRequest());
		postText(MockTokens.member(), "{\"text\": \"" + marker).andExpect(status().isBadRequest());
		await().pollDelay(Duration.ofSeconds(1)).until(() -> true);
		assertThat(output.getAll()).doesNotContain(marker).contains("unavailability message confirmed");
	}

	private UUID sendText(String token, String text) throws Exception {
		String body = postText(token, objectMapper.writeValueAsString(Map.of("text", text)))
				.andExpect(status().isAccepted())
				.andExpect(header().string(HttpHeaders.LOCATION, startsWith(PATH + "/")))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(objectMapper.readTree(body).path("id").asString());
	}

	private ResultActions postText(String token, String json) throws Exception {
		return mockMvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, token)
				.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions confirm(String token, UUID id, String periods) throws Exception {
		return mockMvc.perform(post(PATH + "/" + id + "/confirm").header(HttpHeaders.AUTHORIZATION, token)
				.contentType(MediaType.APPLICATION_JSON).content("{\"periods\":" + periods + "}"));
	}

	private void expectInvalid(UUID id, String periods, String field, String code) throws Exception {
		confirm(MockTokens.member(), id, periods)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value(field))
				.andExpect(jsonPath("$.errors[0].code").value(code));
	}

	private UUID proposed(String text) throws Exception {
		UUID id = sendText(MockTokens.member(), text);
		answer(id, SATURDAY_MORNING, "MODEL");
		return id;
	}

	private void answer(UUID id, String periods, String method) {
		sendConcluida(UUID.randomUUID(), requestEventId(id), id, periods, method);
		awaitStatus(id, "PROPOSED");
	}

	private void awaitStatus(UUID id, String expected) {
		await().atMost(Duration.ofSeconds(15)).until(() -> expected.equals(
				jdbc.queryForObject("SELECT status FROM unavailability_message WHERE id = ?", String.class, id)));
	}

	private UUID requestEventId(UUID id) {
		return jdbc.queryForObject("SELECT request_event_id FROM unavailability_message WHERE id = ?", UUID.class, id);
	}

	private void sendConcluida(UUID eventId, UUID requestEventId, UUID messageId, String periods, String method) {
		send("""
				{"eventId":"%s","type":"ExtracaoConcluida","schemaVersion":2,"occurredAt":"%s","traceId":"%s",
				 "requestEventId":"%s","unavailabilityRequestId":"%s","periods":%s,"method":"%s",
				 "model":"synthetic-model","latencyMs":1200}
				""".formatted(eventId, Instant.now(), UUID.randomUUID(), requestEventId, messageId, periods, method));
	}

	private void send(String json) {
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbitTemplate.send("escala.events", "extracao.concluida",
				new Message(json.getBytes(StandardCharsets.UTF_8), properties));
	}

	private static List<String> fieldNames(JsonNode node) {
		List<String> names = new java.util.ArrayList<>();
		node.propertyNames().forEach(names::add);
		return names;
	}

	/** Every table and text column of the database that holds {@code part}. */
	private List<String> tablesContaining(String part) {
		List<String> found = new java.util.ArrayList<>();
		for (Map<String, Object> column : jdbc.queryForList("""
				SELECT table_name, column_name FROM information_schema.columns
				WHERE table_schema = 'public' AND data_type IN ('text', 'character varying', 'jsonb', 'json')
				""")) {
			String sql = "SELECT count(*) FROM \"%s\" WHERE \"%s\"::text LIKE ?"
					.formatted(column.get("table_name"), column.get("column_name"));
			if (jdbc.queryForObject(sql, Integer.class, "%" + part + "%") > 0) {
				found.add(column.get("table_name") + "." + column.get("column_name"));
			}
		}
		return found;
	}

	/** A clock the tests move. */
	static final class MovableClock extends Clock {

		private volatile Instant instant;

		MovableClock(Instant instant) {
			this.instant = instant;
		}

		void set(Instant to) {
			instant = to;
		}

		void move(Duration by) {
			instant = instant.plus(by);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
