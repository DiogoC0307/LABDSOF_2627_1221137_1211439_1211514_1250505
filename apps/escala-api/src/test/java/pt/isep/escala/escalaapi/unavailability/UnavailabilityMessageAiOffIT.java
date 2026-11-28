package pt.isep.escala.escalaapi.unavailability;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * AI turned off by the flag (SCRUM-89 AC2, ADR 0011): escala-api reads the same AI_ENABLED as the worker, here not
 * set, so off. The PWA asks GET settings and shows only the form; a free text sent anyway answers 409 ai_off and
 * nothing is stored. Through the real server, so the error body is the one a browser gets.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.rabbitmq.dynamic=true")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class UnavailabilityMessageAiOffIT {

	static final String PATH = "/api/v1/unavailability-messages";

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@LocalServerPort
	int port;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	JdbcTemplate jdbc;

	private final HttpClient http = HttpClient.newHttpClient();

	@BeforeEach
	void clean() {
		jdbc.update("DELETE FROM unavailability_message");
	}

	@Test
	@DisplayName("TC-SCRUM-89-02 with AI off, GET settings answers aiEnabled false to every role, and \"settings\" is not taken as a message ID by /{id}")
	void settingsWithAiOff() throws Exception {
		for (String token : List.of(MockTokens.member(), MockTokens.supervisor(), MockTokens.admin())) {
			HttpResponse<String> response = call("GET", PATH + "/settings", token, null);
			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(response.headers().firstValue(HttpHeaders.CONTENT_TYPE))
					.hasValueSatisfying(type -> assertThat(type).startsWith(MediaType.APPLICATION_JSON_VALUE));
			JsonNode body = objectMapper.readTree(response.body());
			assertThat(body.has("aiEnabled")).isTrue();
			assertThat(body.path("aiEnabled").asBoolean()).isFalse();
			assertThat(body.has("errors")).isFalse();
		}
	}

	@Test
	@DisplayName("TC-SCRUM-89-02 with AI off, a free text answers 409 with errors [{text, ai_off}] in the JSON error body, and nothing is stored nor logged")
	void aTextIsRefusedWithAiOff(CapturedOutput output) throws Exception {
		String marker = "MARKER-89-02";
		HttpResponse<String> response = call("POST", PATH, MockTokens.member(),
				objectMapper.writeValueAsString(java.util.Map.of("text", marker + " não posso sábado de manhã")));

		assertThat(response.statusCode()).isEqualTo(409);
		assertThat(response.headers().firstValue(HttpHeaders.CONTENT_TYPE))
				.hasValueSatisfying(type -> assertThat(type).startsWith(MediaType.APPLICATION_JSON_VALUE));
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.path("status").asInt()).isEqualTo(409);
		assertThat(body.path("error").asString()).isEqualTo("Conflict");
		assertThat(body.path("path").asString()).isEqualTo(PATH);
		assertThat(body.has("timestamp")).isTrue();
		assertThat(body.path("errors").get(0).path("field").asString()).isEqualTo("text");
		assertThat(body.path("errors").get(0).path("code").asString()).isEqualTo("ai_off");
		assertThat(response.body()).doesNotContain(marker);

		assertThat(jdbc.queryForObject("SELECT count(*) FROM unavailability_message", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_event WHERE payload LIKE ?", Integer.class,
				"%" + marker + "%")).isZero();
		assertThat(output.getAll()).doesNotContain(marker);
	}

	private HttpResponse<String> call(String method, String path, String token, String json) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header(HttpHeaders.AUTHORIZATION, token)
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		if (json == null) {
			request.method(method, HttpRequest.BodyPublishers.noBody());
		} else {
			request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.method(method, HttpRequest.BodyPublishers.ofString(json));
		}
		return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}
}
