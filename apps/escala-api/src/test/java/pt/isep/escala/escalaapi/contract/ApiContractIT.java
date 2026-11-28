package pt.isep.escala.escalaapi.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;
import tools.jackson.databind.ObjectMapper;

/**
 * TC-SCRUM-96-01 to 06, 16 and 19: the real HTTP responses of escala-api, error bodies included, against
 * {@code contracts/openapi.yaml}. Every call checks that the status is documented for the operation and that the body
 * matches its schema; an error must have a body, which the spec describes (ErrorResponse, SCRUM-88). The request bodies
 * sent are checked against the spec too, except the invalid ones sent on purpose.
 * A real server (random port), not MockMvc, so the error bodies are the ones Spring Boot really writes.
 * TC-SCRUM-89-02: the settings of the free text, the same way.
 * AI is on, so that free text is accepted (SCRUM-89); with AI off it answers 409, see UnavailabilityMessageAiOffIT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "spring.rabbitmq.dynamic=true", "escala.ai.enabled=true" })
@Testcontainers
class ApiContractIT {

	/** Sunday 2026-11-22, 10:00 UTC, as in UnavailabilityIT: the unavailability window is relative to it. */
	static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

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

	private static final HttpClient HTTP = HttpClient.newHttpClient();

	@LocalServerPort
	int port;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	RequestMappingHandlerMapping mappings;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	JdbcTemplate jdbc;

	/** A call checked against the spec: {@code template} is the path as the spec writes it. */
	private HttpResponse<String> call(String method, String template, String uri, String token, String body,
			boolean validRequest) throws Exception {
		if (body != null && validRequest) {
			assertThat(Contracts.requestErrors(method, template, body)).as("request of " + method + " " + uri)
					.isEmpty();
		}
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + uri))
				.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
						: HttpRequest.BodyPublishers.ofString(body));
		if (body != null) {
			request.header("Content-Type", "application/json");
		}
		if (token != null) {
			request.header("Authorization", token);
		}
		HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
		String contentType = response.headers().firstValue("Content-Type").orElse(null);
		List<String> errors = Contracts.responseErrors(method, template, response.statusCode(), contentType,
				response.body());
		assertThat(errors).as(method + " " + uri + " answered " + response.statusCode() + " " + response.body())
				.isEmpty();
		return response;
	}

	private HttpResponse<String> call(String method, String template, String uri, String token, String body)
			throws Exception {
		return call(method, template, uri, token, body, true);
	}

	private String id(HttpResponse<String> response) {
		return objectMapper.readTree(response.body()).path("id").asString();
	}

	/** Every (method, path) escala-api serves through its controllers, as {@code "GET /api/v1/members"}. */
	private Set<String> served() {
		Set<String> served = new TreeSet<>();
		for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
			for (String path : info.getPatternValues()) {
				if (!path.startsWith("/error")) {
					info.getMethodsCondition().getMethods()
							.forEach(m -> served.add(m.name().toUpperCase(Locale.ROOT) + " " + path));
				}
			}
		}
		return served;
	}

	@Test
	@DisplayName("TC-SCRUM-96-01 every endpoint escala-api serves is in contracts/openapi.yaml, with its method")
	void everyEndpointIsInTheSpec() {
		Set<String> served = served();
		assertThat(served).isNotEmpty();
		assertThat(Contracts.operations()).containsAll(served).contains("GET /actuator/health",
				"GET /actuator/health/liveness", "GET /actuator/health/readiness");
	}

	@Test
	@DisplayName("TC-SCRUM-96-16 every error the spec documents for an endpoint escala-api serves has a body (ErrorResponse, SCRUM-88)")
	void everyServedErrorHasABody() {
		List<String> withoutBody = new ArrayList<>();
		for (String operation : served()) {
			String[] parts = operation.split(" ", 2);
			withoutBody.addAll(Contracts.errorResponsesWithoutBody(parts[0], parts[1]));
		}
		assertThat(withoutBody).isEmpty();
	}

	@Test
	@DisplayName("TC-SCRUM-96-02 /api/v1/piquetes answers as the spec says: 201, 200, 400, 401 and 403")
	void piquetes() throws Exception {
		String piquetes = "/api/v1/piquetes";
		assertThat(call("POST", piquetes, piquetes, MockTokens.supervisor(), "{\"dutyDate\":\"2026-12-01\"}")
				.statusCode()).isEqualTo(201);
		assertThat(call("GET", piquetes, piquetes, MockTokens.member(), null).statusCode()).isEqualTo(200);
		assertThat(call("POST", piquetes, piquetes, MockTokens.supervisor(), "{}", false).statusCode())
				.isEqualTo(400);
		assertThat(call("GET", piquetes, piquetes, null, null).statusCode()).isEqualTo(401);
		assertThat(call("POST", piquetes, piquetes, MockTokens.member(), "{\"dutyDate\":\"2026-12-01\"}")
				.statusCode()).isEqualTo(403);
	}

	@Test
	@DisplayName("SCRUM-171 the candidate checks answer as the spec says: 202, 200, 400, 401, 403 and 404")
	void candidateChecks() throws Exception {
		String checks = "/api/v1/candidate-checks";
		String body = """
				{"startsAt":"2026-12-09T08:00:00Z","endsAt":"2026-12-09T20:00:00Z","role":"CREW_CHIEF",
				 "otherShifts":[{"memberId":"%s","startsAt":"2026-12-09T06:00:00Z","endsAt":"2026-12-09T10:00:00Z"}]}
				""".formatted(MockTokens.ADMIN_ID);
		HttpResponse<String> started = call("POST", checks, checks, MockTokens.supervisor(), body);
		assertThat(started.statusCode()).isEqualTo(202);
		assertThat(call("GET", checks + "/{id}", checks + "/" + id(started), MockTokens.supervisor(), null).statusCode())
				.isEqualTo(200);
		assertThat(call("POST", checks, checks, MockTokens.supervisor(), "{}", false).statusCode()).isEqualTo(400);
		assertThat(call("POST", checks, checks, MockTokens.member(), body).statusCode()).isEqualTo(403);
		assertThat(call("POST", checks, checks, null, body).statusCode()).isEqualTo(401);
		assertThat(call("GET", checks + "/{id}", checks + "/" + UUID.randomUUID(), MockTokens.supervisor(), null)
				.statusCode()).isEqualTo(404);
	}

	@Test
	@DisplayName("SCRUM-66 publishing a week and the member's notifications answer as the spec says: 202, 200, 204, 400, 401, 403 and 404")
	void publicationsAndNotifications() throws Exception {
		String publications = "/api/v1/schedule-publications";
		String shift = """
				{"key":"%s","id":null,"dutyDate":"2026-12-09","period":"DAY","startsAt":"2026-12-09T08:00:00Z",
				 "endsAt":"2026-12-09T20:00:00Z","crew":[{"role":"CREW_MEMBER","count":1}],"assigned":[%s]}""";
		String checking = "{\"week\":\"2026-W50\",\"shifts\":[" + shift.formatted("with-someone",
				"{\"role\":\"CREW_MEMBER\",\"memberId\":\"" + MockTokens.MEMBER_ID + "\"}") + "]}";
		HttpResponse<String> started = call("POST", publications, publications, MockTokens.supervisor(), checking);
		assertThat(started.statusCode()).isEqualTo(202);
		assertThat(call("GET", publications + "/{id}", publications + "/" + id(started), MockTokens.supervisor(), null)
				.statusCode()).isEqualTo(200);

		// A shift with nobody on it is blocked at once, with its conflict
		String empty = "{\"week\":\"2026-W51\",\"shifts\":[" + shift.formatted("nobody", "")
				.replace("2026-12-09", "2026-12-16") + "]}";
		HttpResponse<String> blocked = call("POST", publications, publications, MockTokens.supervisor(), empty);
		assertThat(objectMapper.readTree(blocked.body()).path("status").asString()).isEqualTo("BLOCKED");

		assertThat(call("POST", publications, publications, MockTokens.supervisor(), "{}", false).statusCode())
				.isEqualTo(400);
		assertThat(call("POST", publications, publications, MockTokens.member(), checking).statusCode()).isEqualTo(403);
		assertThat(call("POST", publications, publications, null, checking).statusCode()).isEqualTo(401);
		assertThat(call("GET", publications + "/{id}", publications + "/" + UUID.randomUUID(), MockTokens.supervisor(),
				null).statusCode()).isEqualTo(404);

		String notifications = "/api/v1/notifications";
		assertThat(call("GET", notifications, notifications, MockTokens.member(), null).statusCode()).isEqualTo(200);
		assertThat(call("POST", notifications + "/read", notifications + "/read", MockTokens.member(), null)
				.statusCode()).isEqualTo(204);
		assertThat(call("POST", notifications + "/{id}/read", notifications + "/" + UUID.randomUUID() + "/read",
				MockTokens.member(), null).statusCode()).isEqualTo(404);
		assertThat(call("GET", notifications, notifications, null, null).statusCode()).isEqualTo(401);
	}

	@Test
	@DisplayName("TC-SCRUM-96-03 the members and certification types endpoints answer as the spec says")
	void members() throws Exception {
		String member = """
				{"fullName":"Synthetic Contract Member","rank":"FIREFIGHTER_1ST_CLASS","rosterStatus":"ACTIVE_ROSTER",
				 "withinAgeLimit":true,"contact":"910000096","functions":["DRIVER"],
				 "licences":[{"licenceClass":"C","expiresOn":"2030-06-30"}],
				 "certifications":[{"type":"TAS","issuedOn":"2024-04-01","expiresOn":"2027-03-31"}]}
				""";
		HttpResponse<String> created = call("POST", "/api/v1/members", "/api/v1/members", MockTokens.admin(), member);
		assertThat(created.statusCode()).isEqualTo(201);
		String one = "/api/v1/members/" + id(created);

		assertThat(call("GET", "/api/v1/members", "/api/v1/members", MockTokens.admin(), null).statusCode())
				.isEqualTo(200);
		assertThat(call("GET", "/api/v1/members/{id}", one, MockTokens.admin(), null).statusCode()).isEqualTo(200);
		assertThat(call("PUT", "/api/v1/members/{id}", one, MockTokens.admin(), member).statusCode()).isEqualTo(200);
		assertThat(call("PUT", "/api/v1/members/{id}/certifications", one + "/certifications", MockTokens.admin(), """
				{"licences":[{"licenceClass":"C","expiresOn":"2030-06-30"}],
				 "certifications":[{"type":"TAS","issuedOn":"2025-04-01","expiresOn":"2028-03-31"}]}
				""").statusCode()).isEqualTo(200);
		assertThat(call("GET", "/api/v1/certification-types", "/api/v1/certification-types", MockTokens.admin(), null)
				.statusCode()).isEqualTo(200);

		assertThat(call("POST", "/api/v1/members", "/api/v1/members", MockTokens.admin(), "{}", false).statusCode())
				.isEqualTo(400);
		assertThat(call("GET", "/api/v1/members", "/api/v1/members", null, null).statusCode()).isEqualTo(401);
		assertThat(call("GET", "/api/v1/members/{id}", "/api/v1/members/" + UUID.randomUUID(), MockTokens.admin(),
				null).statusCode()).isEqualTo(404);
		assertThat(call("POST", "/api/v1/members", "/api/v1/members", MockTokens.member(), member).statusCode())
				.isEqualTo(403);
	}

	@Test
	@DisplayName("TC-SCRUM-96-04 the member's own unavailability endpoints answer as the spec says")
	void unavailabilities() throws Exception {
		String all = "/api/v1/unavailabilities";
		HttpResponse<String> created = call("POST", all, all, MockTokens.member(),
				"{\"startsAt\":\"2026-12-05T00:00:00Z\",\"endsAt\":\"2026-12-08T00:00:00Z\"}");
		assertThat(created.statusCode()).isEqualTo(201);
		String one = all + "/" + id(created);

		assertThat(call("GET", all, all, MockTokens.member(), null).statusCode()).isEqualTo(200);
		assertThat(call("GET", all + "/{id}", one, MockTokens.member(), null).statusCode()).isEqualTo(200);
		assertThat(call("POST", all, all, MockTokens.member(), "{}", false).statusCode()).isEqualTo(400);
		assertThat(call("GET", all + "/{id}", all + "/" + UUID.randomUUID(), MockTokens.member(), null).statusCode())
				.isEqualTo(404);
		assertThat(call("DELETE", all + "/{id}", one, MockTokens.member(), null).statusCode()).isEqualTo(204);
		assertThat(call("DELETE", all + "/{id}", one, MockTokens.member(), null).statusCode()).as("already removed")
				.isEqualTo(404);
		assertThat(call("GET", all, all, null, null).statusCode()).isEqualTo(401);

		// SCRUM-65: every member's periods of a week, for the supervisor
		String team = all + "/team";
		assertThat(call("GET", team, team + "?week=2026-W49", MockTokens.supervisor(), null).statusCode()).isEqualTo(200);
		assertThat(call("GET", team, team + "?week=2025-W54", MockTokens.supervisor(), null).statusCode()).isEqualTo(400);
		assertThat(call("GET", team, team + "?week=2026-W49", MockTokens.member(), null).statusCode()).isEqualTo(403);
		assertThat(call("GET", team, team + "?week=2026-W49", null, null).statusCode()).isEqualTo(401);
	}

	@Test
	@DisplayName("TC-SCRUM-96-19 the free-text unavailability messages answer as the spec says: 202, 200, 204, 400, 401, 404, 409 and 429")
	void unavailabilityMessages() throws Exception {
		String all = "/api/v1/unavailability-messages";
		String oneTemplate = all + "/{id}";
		String confirmTemplate = oneTemplate + "/confirm";
		HttpResponse<String> sent = call("POST", all, all, MockTokens.member(), "{\"text\":\"Synthetic text 96-19\"}");
		assertThat(sent.statusCode()).isEqualTo(202);
		String id = id(sent);
		String one = all + "/" + id;
		assertThat(call("GET", oneTemplate, one, MockTokens.member(), null).statusCode()).as("PENDING").isEqualTo(200);
		assertThat(call("POST", confirmTemplate, one + "/confirm", MockTokens.member(), periods()).statusCode())
				.as("no proposal yet").isEqualTo(409);

		propose(UUID.fromString(id));
		await().atMost(Duration.ofSeconds(15)).until(() -> objectMapper.readTree(
				call("GET", oneTemplate, one, MockTokens.member(), null).body()).path("status").asString(),
				"PROPOSED"::equals);
		assertThat(call("POST", confirmTemplate, one + "/confirm", MockTokens.member(),
				"{\"periods\":[{\"startDate\":\"2026-11-28\"}]}", false).statusCode()).isEqualTo(400);
		assertThat(call("POST", confirmTemplate, one + "/confirm", MockTokens.member(), periods()).statusCode())
				.isEqualTo(200);

		String discarded = all + "/" + id(call("POST", all, all, MockTokens.member(), "{\"text\":\"Synthetic 96-19 b\"}"));
		assertThat(call("DELETE", oneTemplate, discarded, MockTokens.member(), null).statusCode()).isEqualTo(204);
		assertThat(call("GET", oneTemplate, one, MockTokens.supervisor(), null).statusCode()).as("another member's")
				.isEqualTo(404);
		assertThat(call("DELETE", oneTemplate, one, MockTokens.supervisor(), null).statusCode()).isEqualTo(404);
		assertThat(call("POST", all, all, MockTokens.member(), "{\"text\":\"\"}", false).statusCode()).isEqualTo(400);
		assertThat(call("POST", all, all, null, "{\"text\":\"Synthetic 96-19 c\"}").statusCode()).isEqualTo(401);

		// at most 10 messages per member in an hour (the clock is fixed); the loop stops at the first that is refused
		int status = 202;
		for (int i = 0; i < 11 && status == 202; i++) {
			status = call("POST", all, all, MockTokens.member(), "{\"text\":\"Synthetic 96-19 rate\"}").statusCode();
		}
		assertThat(status).as("the 11th message within the hour").isEqualTo(429);
	}

	@Test
	@DisplayName("TC-SCRUM-89-02 the free-text settings answer as the spec says: 200, and 401 without a token")
	void unavailabilityMessageSettings() throws Exception {
		String settings = "/api/v1/unavailability-messages/settings";
		assertThat(call("GET", settings, settings, MockTokens.member(), null).statusCode()).isEqualTo(200);
		assertThat(call("GET", settings, settings, null, null).statusCode()).isEqualTo(401);
	}

	/** One Saturday morning after the fixed clock, as the member confirms it. */
	private static String periods() {
		return "{\"periods\":[{\"startDate\":\"2026-11-28\",\"endDate\":\"2026-11-28\",\"startTime\":\"08:00\",\"endTime\":\"13:00\"}]}";
	}

	/** The worker's answer, ExtracaoConcluida v2, for the message: the message becomes PROPOSED. */
	private void propose(UUID messageId) {
		UUID requestEventId = jdbc.queryForObject("SELECT request_event_id FROM unavailability_message WHERE id = ?",
				UUID.class, messageId);
		String answer = """
				{"eventId":"%s","type":"ExtracaoConcluida","schemaVersion":2,"occurredAt":"%s","traceId":"contract-96-19",
				 "requestEventId":"%s","unavailabilityRequestId":"%s","method":"MODEL","model":"synthetic-model",
				 "periods":[{"startDate":"2026-11-28","endDate":"2026-11-28","startTime":"08:00","endTime":"13:00"}]}
				""".formatted(UUID.randomUUID(), NOW, requestEventId, messageId);
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbitTemplate.send("escala.events", "extracao.concluida",
				new Message(answer.getBytes(StandardCharsets.UTF_8), properties));
	}

	@Test
	@DisplayName("TC-SCRUM-96-05 the mocked sign-in endpoints answer as the spec says: 200, and 401 for an unknown member")
	void mockSignIn() throws Exception {
		assertThat(call("GET", "/api/v1/auth/mock-users", "/api/v1/auth/mock-users", null, null).statusCode())
				.isEqualTo(200);
		String signIn = "/api/v1/auth/mock-sign-in";
		assertThat(call("POST", signIn, signIn, null, "{\"memberId\":\"" + MockTokens.SUPERVISOR_ID + "\"}")
				.statusCode()).isEqualTo(200);
		assertThat(call("POST", signIn, signIn, null, "{\"memberId\":\"" + UUID.randomUUID() + "\"}").statusCode())
				.isEqualTo(401);
	}

	@Test
	@DisplayName("TC-SCRUM-96-06 the three Actuator health endpoints answer as the spec says")
	void health() throws Exception {
		for (String path : new String[] { "/actuator/health", "/actuator/health/liveness",
				"/actuator/health/readiness" }) {
			assertThat(call("GET", path, path, null, null).statusCode()).as(path).isEqualTo(200);
		}
	}
}
