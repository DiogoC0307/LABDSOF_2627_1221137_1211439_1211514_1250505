package pt.isep.escala.escalaapi.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 * Every 400 has one format: Spring Boot's error fields plus errors: [{field, code}], never the rejected value, in
 * the answer or in the log (SCRUM-88; the format of SCRUM-77 and SCRUM-64).
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class ErrorFormatIT {

	static final String MARKER = "MARKER-88-06";

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

	static Stream<Arguments> badRequests() {
		return Stream.of(
				Arguments.of("dutyDate missing", piquete("{}"), "dutyDate", "required"),
				Arguments.of("dutyDate null", piquete("{\"dutyDate\":null}"), "dutyDate", "required"),
				Arguments.of("dutyDate not a date", piquete("{\"dutyDate\":\"" + MARKER + "\"}"), "dutyDate", "invalid"),
				Arguments.of("dutyDate impossible", piquete("{\"dutyDate\":\"2026-13-40\"}"), "dutyDate", "invalid"),
				Arguments.of("body absent", post("/api/v1/piquetes").header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor())
						.contentType(MediaType.APPLICATION_JSON), "body", "not_readable"),
				Arguments.of("body not JSON", piquete("{\"dutyDate\": " + MARKER), "body", "not_readable"),
				Arguments.of("enum value unknown", post("/api/v1/members").header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fullName\":\"Synthetic\",\"rank\":\"" + MARKER + "\"}"), "rank", "invalid"),
				Arguments.of("path ID not a UUID", get("/api/v1/unavailabilities/" + MARKER)
						.header(HttpHeaders.AUTHORIZATION, MockTokens.member()), "id", "invalid"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("badRequests")
	@DisplayName("TC-SCRUM-88-06 every 400 answers errors: [{field, code}] without the value, and the value is not logged")
	void everyBadRequestHasOneFormat(String name, MockHttpServletRequestBuilder request, String field, String code,
			CapturedOutput output) throws Exception {
		String body = mockMvc.perform(request)
				.andExpect(status().isBadRequest())
				.andReturn().getResponse().getContentAsString();
		JsonNode error = objectMapper.readTree(body);

		assertThat(error.path("status").asInt()).isEqualTo(400);
		assertThat(error.path("error").asString()).isEqualTo("Bad Request");
		assertThat(error.path("path").asString()).startsWith("/api/v1/");
		assertThat(error.path("errors").get(0).path("field").asString()).isEqualTo(field);
		assertThat(error.path("errors").get(0).path("code").asString()).isEqualTo(code);
		// "path" is the requested URL, which every error body carries; for a bad path ID it holds the ID the client
		// sent. Everything else, the errors included, never has the rejected value.
		((tools.jackson.databind.node.ObjectNode) error).remove("path");
		assertThat(error.toString()).doesNotContain(MARKER);
		assertThat(output.getAll()).doesNotContain(MARKER);
	}

	private static MockHttpServletRequestBuilder piquete(String json) {
		return post("/api/v1/piquetes")
				.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json);
	}
}
