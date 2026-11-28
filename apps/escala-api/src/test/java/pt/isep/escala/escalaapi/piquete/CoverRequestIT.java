package pt.isep.escala.escalaapi.piquete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;
import pt.isep.escala.escalaapi.contract.Contracts;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Cover requests (SCRUM-72, and AC1 of SCRUM-71): when a member can't come, their slot opens, a cover request is made,
 * and only the members with the slot's function that the eligibility rules accept are asked (ADR 0007). The supervisor
 * is told when the slot opens, when nobody is free and qualified, and when nobody accepts in time. Synthetic data only.
 * The deadline check runs every half second here, so AC4 is seen as it runs.
 */
@SpringBootTest(properties = {
		"spring.rabbitmq.dynamic=true",
		"escala.shift-confirmation.poll-ms=3600000",
		"escala.cover.check-ms=500"
})
@AutoConfigureMockMvc
@Testcontainers
class CoverRequestIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	private static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");

	/** A Saturday at least a week ahead: its 20:00 shift is far from the 2 h alert. */
	private static final LocalDate SATURDAY = LocalDate.now(LISBON).plusDays(7)
			.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

	private static final Instant SATURDAY_20 = SATURDAY.atTime(20, 0).atZone(LISBON).toInstant();

	private static final Instant SUNDAY_08 = SATURDAY.plusDays(1).atTime(8, 0).atZone(LISBON).toInstant();

	@Autowired
	MockMvc mockMvc;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	PiqueteRepository piquetes;

	@Autowired
	AssignmentRepository assignments;

	// A new member with these functions on the active roster, registered by the admin.
	private UUID member(String name, String... functions) throws Exception {
		String list = Arrays.stream(functions).map(each -> "\"" + each + "\"").collect(Collectors.joining(","));
		String licences = List.of(functions).contains("DRIVER")
				? "[{\"licenceClass\":\"C\",\"expiresOn\":\"2033-06-30\"}]"
				: "[]";
		String body = """
				{"fullName":"%s","rank":"FIREFIGHTER_1ST_CLASS","rosterStatus":"ACTIVE_ROSTER","withinAgeLimit":true,
				 "contact":null,"functions":[%s],"licences":%s,"certifications":[]}
				""".formatted(name, list, licences);
		String response = mockMvc.perform(post("/api/v1/members").header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(objectMapper.readTree(response).path("id").asString());
	}

	// A published shift with one slot of this role, from start to end.
	private Slot shift(SlotRole role, Instant start, Instant end) {
		LocalDate day = start.atZone(LISBON).toLocalDate();
		Piquete piquete = new Piquete(UUID.randomUUID(), day);
		piquete.schedule(day, ShiftPeriod.NIGHT, start, end, 3);
		Slot slot = new Slot(UUID.randomUUID(), role, SlotStatus.FILLED, UUID.randomUUID(), start);
		piquete.addSlot(slot);
		piquetes.save(piquete);
		return slot;
	}

	// The member holds the slot: a CONFIRMED assignment of a published week.
	private UUID holds(Slot slot, UUID memberId, Instant start, Instant end) {
		UUID publication = UUID.randomUUID();
		jdbc.update("INSERT INTO week_publication (id, week, status, requested_by, plan, created_at) "
				+ "VALUES (?, '2026-W49', 'PUBLISHED', ?, '{}', now())", publication, MockTokens.SUPERVISOR_ID);
		UUID piqueteId = jdbc.queryForObject("SELECT piquete_id FROM slot WHERE id = ?", UUID.class, slot.getId());
		Assignment assignment = new Assignment(UUID.randomUUID(), publication, piqueteId, slot.getId(), memberId,
				slot.getRole(), start, end, Instant.now());
		assignment.confirm(Instant.now());
		assignments.save(assignment);
		return assignment.getId();
	}

	private void unavailable(UUID memberId, Instant start, Instant end) throws Exception {
		mockMvc.perform(post("/api/v1/unavailabilities").header(HttpHeaders.AUTHORIZATION, MockTokens.bearer(memberId, "MEMBER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"startsAt\":\"%s\",\"endsAt\":\"%s\"}".formatted(start, end)))
				.andExpect(status().isCreated());
	}

	private void cannotCome(Slot slot) throws Exception {
		mockMvc.perform(post("/api/v1/slots/" + slot.getId() + "/presence")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.member())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\":\"CANNOT_COME\"}"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.slotFreed").value(true));
	}

	private Map<String, Object> coverOf(Slot slot) {
		return jdbc.queryForMap("SELECT * FROM cover_request WHERE slot_id = ?", slot.getId());
	}

	// The offers of a cover request, by member.
	private Map<UUID, UUID> offersOf(UUID coverId) {
		return jdbc.query("SELECT id, member_id FROM cover_offer WHERE cover_request_id = ?",
				(row, n) -> Map.entry(UUID.fromString(row.getString("member_id")), UUID.fromString(row.getString("id"))),
				coverId).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private String offerStatus(UUID offerId) {
		return jdbc.queryForObject("SELECT status FROM cover_offer WHERE id = ?", String.class, offerId);
	}

	// The AvaliacaoPedida queued for one offer.
	private String pedidoOf(UUID offerId) {
		return jdbc.queryForObject(
				"SELECT payload FROM outbox_event WHERE routing_key = 'avaliacao.pedida' AND payload LIKE ?", String.class,
				"%\"assignmentId\":\"" + offerId + "\"%");
	}

	private void answer(UUID offerId, String result, String... rules) {
		String violated = Arrays.stream(rules).map(code -> "\"" + code + "\"").collect(Collectors.joining(","));
		String body = """
				{"eventId":"%s","type":"AvaliacaoConcluida","schemaVersion":1,"occurredAt":"%s","traceId":"%s",
				 "requestEventId":"%s","assignmentId":"%s","result":"%s","violatedRules":[%s],"engineVersion":"REGRAS_V1"}
				""".formatted(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), UUID.randomUUID(), offerId, result,
				violated);
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbitTemplate.send("escala.events", "avaliacao.concluida",
				new Message(body.getBytes(StandardCharsets.UTF_8), properties));
	}

	// The member's notifications about this cover request, as GET /api/v1/notifications answers them.
	private List<JsonNode> notified(UUID memberId, String role, UUID coverId) throws Exception {
		JsonNode all = objectMapper.readTree(mockMvc.perform(get("/api/v1/notifications")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.bearer(memberId, role)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString());
		List<JsonNode> found = new ArrayList<>();
		all.forEach(each -> {
			if (coverId.toString().equals(each.path("coverRequestId").asString(""))) {
				found.add(each);
			}
		});
		return found;
	}

	private static List<String> types(List<JsonNode> notifications) {
		return notifications.stream().map(each -> each.path("type").asString()).toList();
	}

	/** A driver slot on Saturday at 20:00 that its driver can't come to, and four other members. */
	private record DriverCover(Slot slot, UUID coverId, Map<UUID, UUID> offers, UUID vacated, UUID free, UUID onDuty,
			UUID away, UUID notADriver) {
	}

	private DriverCover aDriverCantCome() throws Exception {
		UUID vacated = member("Cover Vacated Driver", "DRIVER");
		UUID free = member("Cover Free Driver", "DRIVER");
		UUID onDuty = member("Cover On Duty Driver", "DRIVER");
		UUID away = member("Cover Away Driver", "DRIVER");
		UUID notADriver = member("Cover Crew Member", "CREW_MEMBER");
		Slot slot = shift(SlotRole.DRIVER, SATURDAY_20, SUNDAY_08);
		holds(slot, vacated, SATURDAY_20, SUNDAY_08);
		Instant otherStart = SATURDAY.atTime(18, 0).atZone(LISBON).toInstant();
		Instant otherEnd = SATURDAY.atTime(22, 0).atZone(LISBON).toInstant();
		holds(shift(SlotRole.DRIVER, otherStart, otherEnd), onDuty, otherStart, otherEnd);
		unavailable(away, SATURDAY.atTime(19, 0).atZone(LISBON).toInstant(),
				SATURDAY.atTime(23, 0).atZone(LISBON).toInstant());

		cannotCome(slot);
		UUID coverId = (UUID) coverOf(slot).get("id");
		return new DriverCover(slot, coverId, offersOf(coverId), vacated, free, onDuty, away, notADriver);
	}

	@Test
	@DisplayName("TC-SCRUM-71-01 \"I can't come\" opens the slot: the member's assignment is cancelled, a cover request is made, and the supervisor is told")
	void cannotComeOpensACoverRequest() throws Exception {
		DriverCover cover = aDriverCantCome();

		Map<String, Object> request = coverOf(cover.slot());
		assertThat(request).containsEntry("status", "OPEN").containsEntry("reason", "CANNOT_COME")
				.containsEntry("role", "DRIVER").containsEntry("vacated_member_id", cover.vacated());
		assertThat(jdbc.queryForObject("SELECT status FROM assignment WHERE slot_id = ? AND member_id = ?", String.class,
				cover.slot().getId(), cover.vacated())).isEqualTo("CANCELLED");
		List<JsonNode> supervisor = notified(MockTokens.SUPERVISOR_ID, "SUPERVISOR", cover.coverId());
		assertThat(types(supervisor)).containsExactly("SLOT_OPENED");
		assertThat(supervisor.get(0).path("role").asString()).isEqualTo("DRIVER");
		assertThat(Instant.parse(supervisor.get(0).path("startsAt").asString())).isEqualTo(SATURDAY_20);

		// A second "I can't come" for the same slot makes no second request
		cannotCome(cover.slot());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM cover_request WHERE slot_id = ?", Integer.class,
				cover.slot().getId())).isOne();
	}

	@Test
	@DisplayName("TC-SCRUM-72-01 a driver slot on Saturday at 20:00: every other driver is checked with their own facts, and only those the rules accept are asked")
	void onlyFreeAndQualifiedDriversAreAsked() throws Exception {
		DriverCover cover = aDriverCantCome();
		Map<UUID, UUID> offers = cover.offers();

		assertThat(offers).containsKeys(cover.free(), cover.onDuty(), cover.away())
				.doesNotContainKeys(cover.vacated(), cover.notADriver());
		String onDuty = pedidoOf(offers.get(cover.onDuty()));
		assertThat(Contracts.eventErrors("avaliacao-pedida.schema.json", onDuty)).isEmpty();
		JsonNode pedido = objectMapper.readTree(onDuty);
		assertThat(pedido.path("purpose").asString()).isEqualTo("COVER");
		assertThat(pedido.path("slot").path("slotId").asString()).isEqualTo(cover.slot().getId().toString());
		assertThat(Instant.parse(pedido.path("slot").path("start").asString())).isEqualTo(SATURDAY_20);
		assertThat(Instant.parse(pedido.path("slot").path("end").asString())).isEqualTo(SUNDAY_08);
		assertThat(pedido.path("candidate").path("otherAssignments")).hasSize(1);
		JsonNode free = objectMapper.readTree(pedidoOf(offers.get(cover.free()))).path("candidate");
		assertThat(free.path("otherAssignments")).isEmpty();
		assertThat(free.path("unavailabilities")).isEmpty();

		answer(offers.get(cover.free()), "ELIGIBLE");
		answer(offers.get(cover.onDuty()), "INELIGIBLE", "RN05");
		answer(offers.get(cover.away()), "INELIGIBLE", "RN06");

		await().atMost(Duration.ofSeconds(10))
				.until(() -> types(notified(cover.free(), "MEMBER", cover.coverId())).contains("COVER_REQUESTED"));
		JsonNode asked = notified(cover.free(), "MEMBER", cover.coverId()).get(0);
		assertThat(asked.path("role").asString()).isEqualTo("DRIVER");
		assertThat(Instant.parse(asked.path("startsAt").asString())).isEqualTo(SATURDAY_20);
		assertThat(Instant.parse(asked.path("endsAt").asString())).isEqualTo(SUNDAY_08);
		await().atMost(Duration.ofSeconds(10)).until(() -> "NOT_OFFERED".equals(offerStatus(offers.get(cover.onDuty()))));
		assertThat(offerStatus(offers.get(cover.free()))).isEqualTo("OFFERED");
		assertThat(notified(cover.onDuty(), "MEMBER", cover.coverId())).isEmpty();
		assertThat(notified(cover.vacated(), "MEMBER", cover.coverId())).isEmpty();
		assertThat(notified(cover.notADriver(), "MEMBER", cover.coverId())).isEmpty();
	}

	@Test
	@DisplayName("TC-SCRUM-72-02 a member who said they can't come on Saturday is checked with that period and is not asked")
	void aMemberAwayOnSaturdayIsNotAsked() throws Exception {
		DriverCover cover = aDriverCantCome();
		UUID offer = cover.offers().get(cover.away());

		JsonNode away = objectMapper.readTree(pedidoOf(offer)).path("candidate").path("unavailabilities");
		assertThat(away).hasSize(1);
		assertThat(Instant.parse(away.get(0).path("start").asString()))
				.isEqualTo(SATURDAY.atTime(19, 0).atZone(LISBON).toInstant());

		answer(offer, "INELIGIBLE", "RN06");
		await().atMost(Duration.ofSeconds(10)).until(() -> "NOT_OFFERED".equals(offerStatus(offer)));
		assertThat(notified(cover.away(), "MEMBER", cover.coverId())).isEmpty();
	}

	@Test
	@DisplayName("TC-SCRUM-72-03 nobody is free and qualified: the supervisor is alerted as soon as the last answer is back")
	void nobodyFreeAndQualifiedAlertsTheSupervisor() throws Exception {
		DriverCover cover = aDriverCantCome();
		assertThat(types(notified(MockTokens.SUPERVISOR_ID, "SUPERVISOR", cover.coverId())))
				.doesNotContain("COVER_ESCALATED");

		// Every driver asked is refused by the rules, including those of the other tests
		cover.offers().values().forEach(offer -> answer(offer, "INELIGIBLE", "RN05"));

		await().atMost(Duration.ofSeconds(10)).until(() -> types(
				notified(MockTokens.SUPERVISOR_ID, "SUPERVISOR", cover.coverId())).contains("COVER_ESCALATED"));
		List<JsonNode> alerts = notified(MockTokens.SUPERVISOR_ID, "SUPERVISOR", cover.coverId()).stream()
				.filter(each -> "COVER_ESCALATED".equals(each.path("type").asString())).toList();
		assertThat(alerts).hasSize(1);
		assertThat(alerts.get(0).path("reason").asString()).isEqualTo("NO_CANDIDATES");
		assertThat(coverOf(cover.slot())).containsEntry("escalation", "NO_CANDIDATES");
	}

	@Test
	@DisplayName("TC-SCRUM-72-03 nobody else has the slot's function: the supervisor is alerted when the slot opens")
	void nobodyWithTheFunctionAlertsTheSupervisorAtOnce() throws Exception {
		// The synthetic supervisor is the only crew chief here, and is the one who can't come
		Slot slot = shift(SlotRole.CREW_CHIEF, SATURDAY_20, SUNDAY_08);
		holds(slot, MockTokens.SUPERVISOR_ID, SATURDAY_20, SUNDAY_08);

		cannotCome(slot);

		UUID coverId = (UUID) coverOf(slot).get("id");
		assertThat(offersOf(coverId)).isEmpty();
		assertThat(coverOf(slot)).containsEntry("escalation", "NO_CANDIDATES");
		assertThat(types(notified(MockTokens.SUPERVISOR_ID, "SUPERVISOR", coverId)))
				.containsExactlyInAnyOrder("SLOT_OPENED", "COVER_ESCALATED");
	}

	@Test
	@DisplayName("TC-SCRUM-72-04 nobody accepted: the supervisor is alerted 30 min after the request, or 2 h before the shift, whichever comes first")
	void nobodyAcceptedInTimeAlertsTheSupervisor() throws Exception {
		// 30 min after the request, with the shift still a week away
		DriverCover late = aDriverCantCome();
		jdbc.update("UPDATE cover_request SET opened_at = opened_at - interval '31 minutes' WHERE id = ?", late.coverId());

		// 2 h before the shift comes first: a request made now for a shift in 1 h 59 min
		Instant soon = Instant.now().plus(Duration.ofMinutes(119));
		Slot soonSlot = shift(SlotRole.CREW_MEMBER, soon, soon.plus(Duration.ofHours(12)));
		cannotCome(soonSlot);
		UUID soonId = (UUID) coverOf(soonSlot).get("id");

		// Neither deadline yet: a request made now for a shift a week away
		DriverCover early = aDriverCantCome();

		await().atMost(Duration.ofSeconds(10)).until(() -> coverOf(late.slot()).get("escalation") != null
				&& coverOf(soonSlot).get("escalation") != null);
		assertThat(coverOf(late.slot())).containsEntry("escalation", "NO_ACCEPTANCE");
		assertThat(coverOf(soonSlot)).containsEntry("escalation", "NO_ACCEPTANCE");
		assertThat(notified(MockTokens.SUPERVISOR_ID, "SUPERVISOR", soonId).stream()
				.filter(each -> "COVER_ESCALATED".equals(each.path("type").asString()))
				.map(each -> each.path("reason").asString()).toList()).containsExactly("NO_ACCEPTANCE");

		// The check keeps running, and does not alert twice nor too early
		Thread.sleep(1500);
		assertThat(coverOf(early.slot()).get("escalation")).isNull();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM notification WHERE cover_request_id = ? AND type = 'COVER_ESCALATED'",
				Integer.class, late.coverId())).isOne();
	}
}
