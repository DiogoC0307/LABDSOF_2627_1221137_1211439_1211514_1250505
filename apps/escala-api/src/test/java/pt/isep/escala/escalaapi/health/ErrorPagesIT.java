package pt.isep.escala.escalaapi.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Through a real server (MockMvc does not forward to /error): 400, 401, 403 and 404 answer the same JSON error body
 * whatever the Accept header, never the Whitelabel or Tomcat HTML page (SCRUM-88).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.rabbitmq.dynamic=true")
@Testcontainers
class ErrorPagesIT {

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

	private final HttpClient http = HttpClient.newHttpClient();

	@ParameterizedTest(name = "{0} with Accept {4}")
	@CsvSource(delimiter = '|', value = {
			"401 | GET  | /api/v1/piquetes             | none       | text/html",
			"403 | POST | /api/v1/piquetes             | member     | text/html",
			"404 | GET  | /api/v1/piquetes/{random}    | member     | text/html",
			"400 | POST | /api/v1/piquetes             | supervisor | text/html",
			"401 | GET  | /api/v1/piquetes             | none       | */*",
			"404 | GET  | /api/v1/unavailabilities/{random} | member | application/json"})
	@DisplayName("TC-SCRUM-88-06 400, 401, 403 and 404 answer the JSON error body whatever the Accept header, also text/html")
	void errorsAreAlwaysJson(int expected, String method, String path, String who, String accept) throws Exception {
		String url = "http://localhost:" + port + path.replace("{random}", UUID.randomUUID().toString());
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).header("Accept", accept);
		switch (who) {
			case "member" -> request.header("Authorization", MockTokens.member());
			case "supervisor" -> request.header("Authorization", MockTokens.supervisor());
			default -> {
				// no token: the 401 cases
			}
		}
		if ("POST".equals(method)) {
			request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}"));
		} else {
			request.GET();
		}

		HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(expected);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/json"));
		assertThat(response.body()).doesNotContainIgnoringCase("<html").doesNotContain("Whitelabel");
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.path("status").asInt()).isEqualTo(expected);
		assertThat(body.path("error").asString()).isNotBlank();
		assertThat(body.path("path").asString()).startsWith("/api/v1/");
		assertThat(body.has("timestamp")).isTrue();
		if (expected == 400) {
			assertThat(body.path("errors").get(0).path("field").asString()).isEqualTo("dutyDate");
			assertThat(body.path("errors").get(0).path("code").asString()).isEqualTo("required");
		}
	}
}
