package pt.isep.escala.escalaapi.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;
import tools.jackson.databind.ObjectMapper;

/**
 * TC-SCRUM-96-01 to 06 and 16: the real HTTP responses of escala-api, error bodies included, against
 * {@code contracts/openapi.yaml}. Every call checks that the status is documented for the operation and that the body
 * matches its schema; an error must have a body, which the spec describes (ErrorResponse, SCRUM-88). The request bodies
 * sent are checked against the spec too, except the invalid ones sent on purpose.
 * A real server (random port), not MockMvc, so the error bodies are the ones Spring Boot really writes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.rabbitmq.dynamic=true")
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
