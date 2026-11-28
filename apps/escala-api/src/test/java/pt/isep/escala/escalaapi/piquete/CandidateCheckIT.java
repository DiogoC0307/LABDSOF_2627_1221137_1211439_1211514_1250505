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
import java.util.ArrayList;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Who can take a role on a shift: every member with the function is checked by the eligibility rules.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
class CandidateCheckIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	// A day a week from now, to avoid clashes with other tests that use the current day.
	private static final String DAY = java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(7).toString();
	private static final String START = DAY + "T08:00:00Z";
	private static final String END = DAY + "T20:00:00Z";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ObjectMapper objectMapper;

	// A new member with these functions on that roster, registered by the admin.
	private UUID member(String name, String rosterStatus, String... functions) throws Exception {
		String list = java.util.Arrays.stream(functions).map(each -> "\"" + each + "\"").collect(Collectors.joining(","));
		String licences = List.of(functions).contains("DRIVER")
				? "[{\"licenceClass\":\"C\",\"expiresOn\":\"2033-06-30\"}]"
				: "[]";
		String body = """
				{"fullName":"%s","rank":"FIREFIGHTER_1ST_CLASS","rosterStatus":"%s","withinAgeLimit":true,
				 "contact":null,"functions":[%s],"licences":%s,"certifications":[]}
				""".formatted(name, rosterStatus, list, licences);
		String response = mockMvc.perform(post("/api/v1/members").header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(objectMapper.readTree(response).path("id").asString());
	}

	private static MockHttpServletRequestBuilder ask(String token, String body) {
		return post("/api/v1/candidate-checks").contentType(MediaType.APPLICATION_JSON).content(body)
				.header(HttpHeaders.AUTHORIZATION, token);
	}

	private static String request(String role, String otherShifts) {
		return """
				{"startsAt":"%s","endsAt":"%s","role":"%s","otherShifts":[%s]}""".formatted(START, END, role, otherShifts);
	}

	private UUID start(String body) throws Exception {
		String response = mockMvc.perform(ask(MockTokens.supervisor(), body))
				.andExpect(status().isAccepted())
				.andExpect(header().string(HttpHeaders.LOCATION,
						org.hamcrest.Matchers.startsWith("/api/v1/candidate-checks/")))
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(objectMapper.readTree(response).path("id").asString());
	}

	private JsonNode read(UUID id) throws Exception {
		return objectMapper.readTree(mockMvc.perform(get("/api/v1/candidate-checks/" + id)
						.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString());
	}

	// The check's rows, by member.
	private Map<UUID, UUID> checksOf(UUID requestId) {
		return jdbc.query("SELECT id, member_id FROM candidate_check WHERE request_id = ?",
				(row, n) -> Map.entry(UUID.fromString(row.getString("member_id")), UUID.fromString(row.getString("id"))),
				requestId).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	// The AvaliacaoPedida queued for one check.
	private JsonNode pedidoOf(UUID checkId) {
		return objectMapper.readTree(jdbc.queryForObject(
				"SELECT payload FROM outbox_event WHERE routing_key = 'avaliacao.pedida' AND payload LIKE ?", String.class,
				"%\"assignmentId\":\"" + checkId + "\"%"));
	}

	private void answer(UUID checkId, String result, String... rules) {
		String violated = java.util.Arrays.stream(rules).map(code -> "\"" + code + "\"").collect(Collectors.joining(","));
		String body = """
				{"eventId":"%s","type":"AvaliacaoConcluida","schemaVersion":1,"occurredAt":"%s","traceId":"%s",
				 "requestEventId":"%s","assignmentId":"%s","result":"%s","violatedRules":[%s],"engineVersion":"REGRAS_V1"}
				""".formatted(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), UUID.randomUUID(), checkId, result,
				violated);
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbitTemplate.send("escala.events", "avaliacao.concluida",
				new Message(body.getBytes(StandardCharsets.UTF_8), properties));
	}

	@Test
	@DisplayName("Every member with the role is checked with their own facts, and the answers are read once all are back")
	void everyMemberWithTheRoleIsCheckedByTheWorker() throws Exception {
		UUID free = member("Rita Livre", "ACTIVE_ROSTER", "CREW_CHIEF", "DRIVER");
		UUID busy = member("Tiago Ocupado", "ACTIVE_ROSTER", "DRIVER");
		UUID reserve = member("Ulisses Reserva", "RESERVE_ROSTER", "DRIVER");
		UUID away = member("Vera Ausente", "ACTIVE_ROSTER", "DRIVER");
		UUID notADriver = member("Xavier Equipa", "ACTIVE_ROSTER", "CREW_MEMBER");
		mockMvc.perform(post("/api/v1/unavailabilities").header(HttpHeaders.AUTHORIZATION, MockTokens.bearer(away, "MEMBER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"startsAt\":\"%sT12:00:00Z\",\"endsAt\":\"%sT13:00:00Z\"}".formatted(DAY, DAY)))
				.andExpect(status().isCreated());

		UUID id = start(request("DRIVER", """
				{"memberId":"%s","startsAt":"%sT06:00:00Z","endsAt":"%sT10:00:00Z"}""".formatted(busy, DAY, DAY)));

		Map<UUID, UUID> checks = checksOf(id);
		assertThat(checks).containsKeys(free, busy, reserve, away).doesNotContainKey(notADriver);
		JsonNode pedido = pedidoOf(checks.get(busy));
		assertThat(pedido.path("purpose").asString()).isEqualTo("CANDIDATE");
		assertThat(pedido.path("slot").path("start").asString()).isEqualTo(START);
		assertThat(pedido.path("candidate").path("otherAssignments")).hasSize(1);
		assertThat(pedidoOf(checks.get(free)).path("candidate").path("otherAssignments")).isEmpty();
		assertThat(pedidoOf(checks.get(reserve)).path("candidate").path("inActiveRoster").asBoolean()).isFalse();
		assertThat(pedidoOf(checks.get(away)).path("candidate").path("unavailabilities")).hasSize(1);
		assertThat(read(id).path("status").asString()).isEqualTo("CHECKING");
		assertThat(read(id).path("candidates").get(0).path("result").asString()).isEqualTo("PENDING");

		answer(checks.get(free), "ELIGIBLE");
		answer(checks.get(busy), "INELIGIBLE", "RN05");
		answer(checks.get(reserve), "INELIGIBLE", "RN01");
		answer(checks.get(away), "INELIGIBLE", "RN06");
		checks.entrySet().stream()
				.filter(entry -> !List.of(free, busy, reserve, away).contains(entry.getKey()))
				.forEach(entry -> answer(entry.getValue(), "FAILED"));

		JsonNode done = await().atMost(Duration.ofSeconds(15)).until(() -> read(id),
				body -> body.path("status").asString().equals("DONE"));
		Map<String, String> results = new java.util.HashMap<>();
		Map<String, List<String>> rules = new java.util.HashMap<>();
		done.path("candidates").forEach(candidate -> {
			results.put(candidate.path("memberId").asString(), candidate.path("result").asString());
			List<String> codes = new ArrayList<>();
			candidate.path("rules").forEach(code -> codes.add(code.asString()));
			rules.put(candidate.path("memberId").asString(), codes);
		});
		assertThat(results).containsEntry(free.toString(), "ELIGIBLE")
				.containsEntry(busy.toString(), "INELIGIBLE")
				.containsEntry(reserve.toString(), "INELIGIBLE")
				.containsEntry(away.toString(), "INELIGIBLE");
		assertThat(rules.get(busy.toString())).containsExactly("RN05");
		assertThat(rules.get(reserve.toString())).containsExactly("RN01");
		assertThat(rules.get(away.toString())).containsExactly("RN06");
		assertThat(rules.get(free.toString())).isEmpty();
	}

	@Test
	@DisplayName("A check the worker could not evaluate is NOT_CHECKED, and a late answer changes nothing")
	void aFailedCheckIsNotChecked() throws Exception {
		member("Zita Falha", "ACTIVE_ROSTER", "CREW_CHIEF");
		UUID id = start(request("CREW_CHIEF", ""));
		checksOf(id).values().forEach(check -> answer(check, "FAILED"));
		JsonNode done = await().atMost(Duration.ofSeconds(15)).until(() -> read(id),
				body -> body.path("status").asString().equals("DONE"));
		done.path("candidates").forEach(candidate ->
				assertThat(candidate.path("result").asString()).isEqualTo("NOT_CHECKED"));

		UUID one = checksOf(id).values().iterator().next();
		answer(one, "ELIGIBLE");
		await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5)).until(() -> true);
		assertThat(jdbc.queryForObject("SELECT evaluation_state FROM candidate_check WHERE id = ?", String.class, one))
				.isEqualTo("FAILED");
	}

	@Test
	@DisplayName("A shift that is not one is refused with a code per field, and nothing is checked")
	void anInvalidShiftIsRefused() throws Exception {
		long before = jdbc.queryForObject("SELECT count(*) FROM candidate_check", Long.class);
		mockMvc.perform(ask(MockTokens.supervisor(), """
						{"startsAt":"2031-03-05T20:00:00Z","endsAt":"2031-03-05T08:00:00Z",
						 "otherShifts":[{"memberId":null,"startsAt":"2031-03-05T08:00:00Z","endsAt":"2031-03-05T09:00:00Z"}]}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.field == 'role')].code").value("required"))
				.andExpect(jsonPath("$.errors[?(@.field == 'endsAt')].code").value("ends_before_start"))
				.andExpect(jsonPath("$.errors[?(@.field == 'otherShifts[0]')].code").value("invalid"));
		assertThat(jdbc.queryForObject("SELECT count(*) FROM candidate_check", Long.class)).isEqualTo(before);
	}

	@Test
	@DisplayName("Only the supervisor asks who can take a role")
	void onlyTheSupervisorAsks() throws Exception {
		member("Ana Supervisionada", "ACTIVE_ROSTER", "DRIVER");
		String body = request("DRIVER", "");
		mockMvc.perform(ask(MockTokens.member(), body)).andExpect(status().isForbidden());
		mockMvc.perform(ask(MockTokens.admin(), body)).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/candidate-checks").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
		UUID id = start(body);
		mockMvc.perform(get("/api/v1/candidate-checks/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/candidate-checks/" + UUID.randomUUID())
						.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isNotFound());
	}
}
