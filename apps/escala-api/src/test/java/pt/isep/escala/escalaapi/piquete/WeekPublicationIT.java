package pt.isep.escala.escalaapi.piquete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Publishing weekly schedule integration tests
 * 
 * The supervisor sends the week, each member on it is checked by the eligibility rules, and only then is the
 * week published, with its members notified, or blocked with its conflicts.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
class WeekPublicationIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	// Each test takes the next week from four weeks ahead, so they never share a week
	private static final AtomicInteger NEXT_WEEK = new AtomicInteger(4);

	@Autowired
	MockMvc mockMvc;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ObjectMapper objectMapper;

	// The week of a test: its Wednesday and Thursday, both in the future
	private record Week(String iso, LocalDate wednesday, LocalDate thursday) {
	}

	private static Week nextWeek() {
		LocalDate monday = LocalDate.now(PiqueteService.LISBON)
				.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
				.plusWeeks(NEXT_WEEK.getAndIncrement());
		String iso = "%d-W%02d".formatted(monday.get(IsoFields.WEEK_BASED_YEAR),
				monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
		return new Week(iso, monday.plusDays(2), monday.plusDays(3));
	}

	private static String at(LocalDate day, String time) {
		return ZonedDateTime.of(day, LocalTime.parse(time), PiqueteService.LISBON).toOffsetDateTime().toString();
	}

	private static Instant instant(LocalDate day, String time) {
		return ZonedDateTime.of(day, LocalTime.parse(time), PiqueteService.LISBON).toInstant();
	}

	/** A shift as the PWA sends it: from and to on that day, a crew, and who is on it as "ROLE:memberId". */
	private static String shift(String key, UUID id, LocalDate day, String from, String to, String crew,
			String... assigned) {
		String members = java.util.Arrays.stream(assigned)
				.map(each -> each.split(":"))
				.map(parts -> "{\"role\":\"%s\",\"memberId\":\"%s\"}".formatted(parts[0], parts[1]))
				.collect(Collectors.joining(","));
		return """
				{"key":"%s","id":%s,"dutyDate":"%s","period":"DAY","startsAt":"%s","endsAt":"%s","crew":%s,"assigned":[%s]}"""
				.formatted(key, id == null ? "null" : "\"" + id + "\"", day, at(day, from), at(day, to), crew, members);
	}

	private static String crew(int crewChiefs, int drivers, int crewMembers) {
		return """
				[{"role":"CREW_CHIEF","count":%d},{"role":"DRIVER","count":%d},{"role":"CREW_MEMBER","count":%d}]"""
				.formatted(crewChiefs, drivers, crewMembers);
	}

	private static String week(Week week, String... shifts) {
		return "{\"week\":\"%s\",\"shifts\":[%s]}".formatted(week.iso(), String.join(",", shifts));
	}

	private static MockHttpServletRequestBuilder publishAs(String token, String body) {
		return post("/api/v1/schedule-publications").contentType(MediaType.APPLICATION_JSON).content(body)
				.header(HttpHeaders.AUTHORIZATION, token);
	}

	/** Sends the week and returns the publication's ID. */
	private UUID publish(String body) throws Exception {
		String response = mockMvc.perform(publishAs(MockTokens.supervisor(), body))
				.andExpect(status().isAccepted())
				.andExpect(header().string(HttpHeaders.LOCATION,
						org.hamcrest.Matchers.startsWith("/api/v1/schedule-publications/")))
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(objectMapper.readTree(response).path("id").asString());
	}

	private JsonNode publication(UUID id) throws Exception {
		return objectMapper.readTree(mockMvc.perform(get("/api/v1/schedule-publications/" + id)
						.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString());
	}

	private JsonNode published(UUID id, String status) {
		return await().atMost(Duration.ofSeconds(15)).until(() -> publication(id),
				body -> body.path("status").asString().equals(status));
	}

	/** The publication's assignments, by the member on each. */
	private Map<UUID, UUID> assignmentsOf(UUID publicationId) {
		return jdbc.query("SELECT id, member_id FROM assignment WHERE publication_id = ?",
				(row, n) -> Map.entry(UUID.fromString(row.getString("member_id")), UUID.fromString(row.getString("id"))),
				publicationId).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/** The worker's answer for an assignment, through the broker. */
	private void answer(UUID assignmentId, String result, String... rules) {
		String violated = java.util.Arrays.stream(rules).map(code -> "\"" + code + "\"").collect(Collectors.joining(","));
		String body = """
				{"eventId":"%s","type":"AvaliacaoConcluida","schemaVersion":1,"occurredAt":"%s","traceId":"%s",
				 "requestEventId":"%s","assignmentId":"%s","result":"%s","violatedRules":[%s],"engineVersion":"REGRAS_V1"}
				""".formatted(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), UUID.randomUUID(), assignmentId,
				result, violated);
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbitTemplate.send("escala.events", "avaliacao.concluida",
				new Message(body.getBytes(StandardCharsets.UTF_8), properties));
	}

	private void everyoneEligible(UUID publicationId) {
		assignmentsOf(publicationId).values().forEach(assignment -> answer(assignment, "ELIGIBLE"));
	}

	/** A new member on the active roster, registered by the admin. */
	private UUID member(String name) throws Exception {
		String body = """
				{"fullName":"%s","rank":"FIREFIGHTER_1ST_CLASS","rosterStatus":"ACTIVE_ROSTER","withinAgeLimit":true,
				 "contact":null,"functions":["CREW_CHIEF","DRIVER","CREW_MEMBER"],
				 "licences":[{"licenceClass":"C","expiresOn":"2030-06-30"}],"certifications":[]}
				""".formatted(name);
		String response = mockMvc.perform(post("/api/v1/members").header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(objectMapper.readTree(response).path("id").asString());
	}

	/** The member's notifications, newest first, as "TYPE" or "TYPE shiftCount". */
	private List<JsonNode> notificationsOf(UUID memberId) throws Exception {
		JsonNode list = objectMapper.readTree(mockMvc.perform(get("/api/v1/notifications")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.bearer(memberId, "MEMBER")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString());
		List<JsonNode> each = new ArrayList<>();
		list.forEach(each::add);
		return each;
	}

	private List<String> typesOf(UUID memberId) throws Exception {
		return notificationsOf(memberId).stream().map(each -> each.path("type").asString()).toList();
	}

	private JsonNode weekAsMembersSeeIt(Week week) throws Exception {
		return objectMapper.readTree(mockMvc.perform(get("/api/v1/piquetes").param("week", week.iso())
						.header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString());
	}

	private UUID onlyShiftId(Week week, LocalDate day) throws Exception {
		for (JsonNode piquete : weekAsMembersSeeIt(week)) {
			if (piquete.path("dutyDate").asString().equals(day.toString())) {
				return UUID.fromString(piquete.path("id").asString());
			}
		}
		throw new AssertionError("no shift on " + day);
	}

	@Test
	@DisplayName("TC-SCRUM-66-01 a week with no conflicts is published once every member is checked, and every member with a shift is notified")
	void aWeekWithNoConflictsIsPublishedAndItsMembersNotified() throws Exception {
		Week week = nextWeek();
		UUID chief = member("Ana Chefe");
		UUID crewMember = member("Bruno Equipa");
		UUID driver = member("Carla Condutora");
		UUID id = publish(week(week,
				shift("new-1", null, week.wednesday(), "08:00", "20:00", crew(1, 0, 1),
						"CREW_CHIEF:" + chief, "CREW_MEMBER:" + crewMember),
				shift("new-2", null, week.thursday(), "08:00", "20:00", crew(0, 1, 0), "DRIVER:" + driver)));

		// While the members are checked, nothing changes for anyone
		assertThat(publication(id).path("status").asString()).isEqualTo("CHECKING");
		assertThat(weekAsMembersSeeIt(week)).isEmpty();
		assertThat(assignmentsOf(id)).containsOnlyKeys(chief, crewMember, driver);

		everyoneEligible(id);
		assertThat(published(id, "PUBLISHED").path("conflicts")).isEmpty();

		JsonNode shifts = weekAsMembersSeeIt(week);
		assertThat(shifts).hasSize(2);
		List<String> holders = new ArrayList<>();
		shifts.forEach(piquete -> piquete.path("slots").forEach(slot -> {
			assertThat(slot.path("status").asString()).isEqualTo("FILLED");
			holders.add(slot.path("memberId").asString());
		}));
		assertThat(holders).containsExactlyInAnyOrder(chief.toString(), crewMember.toString(), driver.toString());

		for (UUID member : List.of(chief, crewMember, driver)) {
			List<JsonNode> told = notificationsOf(member);
			assertThat(told).hasSize(1);
			assertThat(told.getFirst().path("type").asString()).isEqualTo("WEEKLY_SCHEDULE_PUBLISHED");
			assertThat(told.getFirst().path("week").asString()).isEqualTo(week.iso());
			assertThat(told.getFirst().path("shiftCount").asInt()).isEqualTo(1);
			assertThat(told.getFirst().path("read").asBoolean()).isFalse();
		}
		mockMvc.perform(get("/api/v1/notifications").header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	@DisplayName("TC-SCRUM-66-02 a week with a conflict is not published: the conflicts are listed with their rules and nothing changes")
	void aWeekWithAConflictIsBlockedWithItsConflicts() throws Exception {
		Week week = nextWeek();
		UUID refused = member("Diana Ocupada");
		UUID fine = member("Eduardo Livre");
		UUID id = publish(week(week,
				shift("wed", null, week.wednesday(), "08:00", "20:00", crew(0, 0, 2),
						"CREW_MEMBER:" + refused, "CREW_MEMBER:" + fine),
				shift("thu", null, week.thursday(), "08:00", "20:00", crew(0, 0, 1))));

		// The shift with nobody on it is a conflict too, and the worker refuses one member (RN05)
		Map<UUID, UUID> assignments = assignmentsOf(id);
		answer(assignments.get(refused), "INELIGIBLE", "RN05");
		answer(assignments.get(fine), "ELIGIBLE");

		JsonNode conflicts = published(id, "BLOCKED").path("conflicts");
		assertThat(conflicts).hasSize(2);
		assertThat(conflicts.get(0).path("type").asString()).isEqualTo("EMPTY_SHIFT");
		assertThat(conflicts.get(0).path("shiftKey").asString()).isEqualTo("thu");
		assertThat(conflicts.get(0).path("dutyDate").asString()).isEqualTo(week.thursday().toString());
		assertThat(conflicts.get(1).path("type").asString()).isEqualTo("INELIGIBLE");
		assertThat(conflicts.get(1).path("shiftKey").asString()).isEqualTo("wed");
		assertThat(conflicts.get(1).path("memberId").asString()).isEqualTo(refused.toString());
		assertThat(conflicts.get(1).path("role").asString()).isEqualTo("CREW_MEMBER");
		assertThat(conflicts.get(1).path("rules").get(0).asString()).isEqualTo("RN05");

		assertThat(weekAsMembersSeeIt(week)).isEmpty();
		assertThat(notificationsOf(refused)).isEmpty();
		assertThat(notificationsOf(fine)).isEmpty();
		assertThat(jdbc.queryForList("SELECT DISTINCT status FROM assignment WHERE publication_id = ?", String.class, id))
				.containsExactly("CANCELLED");
	}

	@Test
	@DisplayName("TC-SCRUM-66-02 a week whose only problem is a shift with nobody on it is blocked at once, without checks")
	void aWeekWithOnlyAnEmptyShiftIsBlockedAtOnce() throws Exception {
		Week week = nextWeek();
		mockMvc.perform(publishAs(MockTokens.supervisor(),
						week(week, shift("empty", null, week.wednesday(), "08:00", "20:00", crew(1, 1, 0)))))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.status").value("BLOCKED"))
				.andExpect(jsonPath("$.conflicts[0].type").value("EMPTY_SHIFT"))
				.andExpect(jsonPath("$.conflicts[0].shiftKey").value("empty"));
		assertThat(weekAsMembersSeeIt(week)).isEmpty();
	}

	@Test
	@DisplayName("A shift short of people is published: covers fill it later")
	void aShortShiftIsPublished() throws Exception {
		Week week = nextWeek();
		UUID alone = member("Fernanda Sozinha");
		UUID id = publish(week(week,
				shift("short", null, week.wednesday(), "08:00", "20:00", crew(1, 1, 2), "DRIVER:" + alone)));
		everyoneEligible(id);
		published(id, "PUBLISHED");

		JsonNode slots = weekAsMembersSeeIt(week).get(0).path("slots");
		assertThat(slots).hasSize(4);
		List<String> statuses = new ArrayList<>();
		slots.forEach(slot -> statuses.add(slot.path("status").asString()));
		assertThat(statuses).containsExactlyInAnyOrder("FILLED", "OPEN", "OPEN", "OPEN");
	}

	@Test
	@DisplayName("Each member on a shift is checked with their other shifts of the week and their unavailabilities (ADR 0008)")
	void eachCheckCarriesTheMembersOtherShiftsAndUnavailabilities() throws Exception {
		Week week = nextWeek();
		UUID twice = member("Gil Duplo");
		mockMvc.perform(post("/api/v1/unavailabilities").header(HttpHeaders.AUTHORIZATION, MockTokens.bearer(twice, "MEMBER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"startsAt\":\"%s\",\"endsAt\":\"%s\"}".formatted(at(week.wednesday(), "12:00"),
								at(week.wednesday(), "13:00"))))
				.andExpect(status().isCreated());
		UUID id = publish(week(week,
				shift("a", null, week.wednesday(), "08:00", "20:00", crew(0, 0, 1), "CREW_MEMBER:" + twice),
				shift("b", null, week.wednesday(), "14:00", "22:00", crew(0, 0, 1), "CREW_MEMBER:" + twice)));

		assertThat(jdbc.queryForObject("SELECT count(*) FROM assignment WHERE publication_id = ? AND member_id = ?",
				Long.class, id, twice)).isEqualTo(2);
		List<JsonNode> pedidos = jdbc.queryForList(
						"SELECT payload FROM outbox_event WHERE routing_key = 'avaliacao.pedida' AND payload LIKE ?",
						String.class, "%" + twice + "%").stream()
				.map(objectMapper::readTree)
				.toList();
		assertThat(pedidos).hasSize(2);
		for (JsonNode pedido : pedidos) {
			JsonNode candidate = pedido.path("candidate");
			assertThat(candidate.path("inActiveRoster").asBoolean()).isTrue();
			assertThat(candidate.path("otherAssignments")).hasSize(1);
			assertThat(candidate.path("unavailabilities")).hasSize(1);
			assertThat(candidate.path("unavailabilities").get(0).path("start").asString())
					.isEqualTo(instant(week.wednesday(), "12:00").toString());
		}
	}

	/** Publishes the week and waits until every member on it is checked and it is published. */
	private void publishedAfterChecks(String body) throws Exception {
		UUID id = publish(body);
		everyoneEligible(id);
		published(id, "PUBLISHED");
	}

	@Test
	@DisplayName("A later publication tells only the members whose own shifts changed: added, removed or times changed")
	void aLaterPublicationTellsOnlyTheMembersItChangesFor() throws Exception {
		Week week = nextWeek();
		UUID stays = member("Helena Fica");
		UUID removed = member("Ivo Sai");
		UUID untouched = member("Joana Igual");
		UUID added = member("Luis Entra");
		publishedAfterChecks(week(week,
				shift("w", null, week.wednesday(), "08:00", "20:00", crew(0, 0, 2),
						"CREW_MEMBER:" + stays, "CREW_MEMBER:" + removed),
				shift("t", null, week.thursday(), "08:00", "20:00", crew(0, 0, 2), "CREW_MEMBER:" + untouched)));
		UUID wednesday = onlyShiftId(week, week.wednesday());
		UUID thursday = onlyShiftId(week, week.thursday());

		// Wednesday now starts at 09:00 without Ivo; Luis joins Joana on Thursday
		publishedAfterChecks(week(week,
				shift("w", wednesday, week.wednesday(), "09:00", "20:00", crew(0, 0, 2), "CREW_MEMBER:" + stays),
				shift("t", thursday, week.thursday(), "08:00", "20:00", crew(0, 0, 2),
						"CREW_MEMBER:" + untouched, "CREW_MEMBER:" + added)));

		assertThat(Map.of("stays", typesOf(stays), "removed", typesOf(removed), "added", typesOf(added),
				"untouched", typesOf(untouched))).isEqualTo(Map.of(
						"stays", List.of("SHIFT_TIMES_CHANGED", "WEEKLY_SCHEDULE_PUBLISHED"),
						"removed", List.of("REMOVED_FROM_SHIFT", "WEEKLY_SCHEDULE_PUBLISHED"),
						"added", List.of("ADDED_TO_SHIFT"),
						"untouched", List.of("WEEKLY_SCHEDULE_PUBLISHED")));
		JsonNode changed = notificationsOf(stays).getFirst();
		assertThat(List.of(changed.path("shiftId").asString(), changed.path("previousStartsAt").asString(),
				changed.path("startsAt").asString())).containsExactly(wednesday.toString(),
						instant(week.wednesday(), "08:00").toString(), instant(week.wednesday(), "09:00").toString());
	}

	@Test
	@DisplayName("A later publication that deletes a shift tells its members it is cancelled, and nobody else")
	void aDeletedShiftIsCancelledForItsMembers() throws Exception {
		Week week = nextWeek();
		UUID stays = member("Paula Fica");
		UUID first = member("Quim Quinta");
		UUID second = member("Sara Quinta");
		publishedAfterChecks(week(week,
				shift("w", null, week.wednesday(), "08:00", "20:00", crew(0, 0, 1), "CREW_MEMBER:" + stays),
				shift("t", null, week.thursday(), "08:00", "20:00", crew(0, 0, 2),
						"CREW_MEMBER:" + first, "CREW_MEMBER:" + second)));
		UUID wednesday = onlyShiftId(week, week.wednesday());

		// Thursday is deleted; Wednesday is sent as it was
		publishedAfterChecks(week(week,
				shift("w", wednesday, week.wednesday(), "08:00", "20:00", crew(0, 0, 1), "CREW_MEMBER:" + stays)));

		assertThat(Map.of("stays", typesOf(stays), "first", typesOf(first), "second", typesOf(second)))
				.isEqualTo(Map.of(
						"stays", List.of("WEEKLY_SCHEDULE_PUBLISHED"),
						"first", List.of("SHIFT_CANCELLED", "WEEKLY_SCHEDULE_PUBLISHED"),
						"second", List.of("SHIFT_CANCELLED", "WEEKLY_SCHEDULE_PUBLISHED")));
		assertThat(weekAsMembersSeeIt(week)).hasSize(1);
	}

	@Test
	@DisplayName("A member reads only their own notifications and marks them read")
	void aMemberMarksTheirNotificationsRead() throws Exception {
		Week week = nextWeek();
		UUID reader = member("Marta Le");
		UUID id = publish(week(week,
				shift("w", null, week.wednesday(), "08:00", "20:00", crew(0, 0, 1), "CREW_MEMBER:" + reader)));
		everyoneEligible(id);
		published(id, "PUBLISHED");

		mockMvc.perform(post("/api/v1/notifications/read").header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isNoContent());
		assertThat(notificationsOf(reader).getFirst().path("read").asBoolean()).isFalse();
		mockMvc.perform(post("/api/v1/notifications/read")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.bearer(reader, "MEMBER")))
				.andExpect(status().isNoContent());
		assertThat(notificationsOf(reader).getFirst().path("read").asBoolean()).isTrue();
		mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("A member marks one of their own notifications read, and not someone else's")
	void aMemberMarksOneNotificationRead() throws Exception {
		Week week = nextWeek();
		UUID reader = member("Rita Uma");
		UUID id = publish(week(week,
				shift("w", null, week.wednesday(), "08:00", "20:00", crew(0, 0, 1), "CREW_MEMBER:" + reader)));
		everyoneEligible(id);
		published(id, "PUBLISHED");
		String notification = notificationsOf(reader).getFirst().path("id").asString();

		mockMvc.perform(post("/api/v1/notifications/{id}/read", notification)
						.header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isNotFound());
		assertThat(notificationsOf(reader).getFirst().path("read").asBoolean()).isFalse();
		for (int time = 0; time < 2; time++) {
			mockMvc.perform(post("/api/v1/notifications/{id}/read", notification)
							.header(HttpHeaders.AUTHORIZATION, MockTokens.bearer(reader, "MEMBER")))
					.andExpect(status().isNoContent());
		}
		assertThat(notificationsOf(reader).getFirst().path("read").asBoolean()).isTrue();
		mockMvc.perform(post("/api/v1/notifications/{id}/read", notification)).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("A publication of the same week while one is checked supersedes it, and the late answers change nothing")
	void aNewPublicationSupersedesOneStillChecking() throws Exception {
		Week week = nextWeek();
		UUID someone = member("Nuno Primeiro");
		UUID older = publish(week(week,
				shift("w", null, week.wednesday(), "08:00", "20:00", crew(0, 0, 1), "CREW_MEMBER:" + someone)));
		UUID newer = publish(week(week,
				shift("w", null, week.wednesday(), "10:00", "20:00", crew(0, 0, 1), "CREW_MEMBER:" + someone)));
		assertThat(publication(older).path("status").asString()).isEqualTo("SUPERSEDED");

		everyoneEligible(older);
		everyoneEligible(newer);
		published(newer, "PUBLISHED");
		assertThat(publication(older).path("status").asString()).isEqualTo("SUPERSEDED");
		JsonNode shifts = weekAsMembersSeeIt(week);
		assertThat(shifts).hasSize(1);
		assertThat(shifts.get(0).path("startsAt").asString()).isEqualTo(instant(week.wednesday(), "10:00").toString());
	}

	@Test
	@DisplayName("An invalid week is refused with a code per field, and nothing is checked")
	void anInvalidWeekIsRefused() throws Exception {
		Week week = nextWeek();
		UUID someone = member("Olga Invalida");
		Week other = nextWeek();
		mockMvc.perform(publishAs(MockTokens.supervisor(), week(week,
						shift("a", null, week.wednesday(), "08:00", "20:00", crew(0, 0, 1),
								"CREW_MEMBER:" + someone, "CREW_MEMBER:" + UUID.randomUUID()),
						shift("a", UUID.randomUUID(), other.wednesday(), "08:00", "20:00", crew(0, 1, 0),
								"CREW_MEMBER:" + someone))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.field == 'shifts[0].assigned[1].memberId')].code").value("unknown_member"))
				.andExpect(jsonPath("$.errors[?(@.field == 'shifts[1].key')].code").value("duplicate"))
				.andExpect(jsonPath("$.errors[?(@.field == 'shifts[1].dutyDate')].code").value("not_in_week"))
				.andExpect(jsonPath("$.errors[?(@.field == 'shifts[1].id')].code").value("unknown_shift"))
				.andExpect(jsonPath("$.errors[?(@.field == 'shifts[1].assigned[0].role')].code").value("too_many"));
		mockMvc.perform(publishAs(MockTokens.supervisor(), "{\"week\":\"2026-W99\",\"shifts\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("week"));
		assertThat(jdbc.queryForObject("SELECT count(*) FROM week_publication WHERE week = ?", Long.class, week.iso()))
				.isZero();
	}

	@Test
	@DisplayName("Only the supervisor publishes a week and follows its checks")
	void onlyTheSupervisorPublishes() throws Exception {
		Week week = nextWeek();
		String body = week(week);
		mockMvc.perform(publishAs(MockTokens.member(), body)).andExpect(status().isForbidden());
		mockMvc.perform(publishAs(MockTokens.admin(), body)).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/schedule-publications").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
		UUID id = publish(body);
		mockMvc.perform(get("/api/v1/schedule-publications/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/schedule-publications/" + UUID.randomUUID())
						.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isNotFound());
	}
}
