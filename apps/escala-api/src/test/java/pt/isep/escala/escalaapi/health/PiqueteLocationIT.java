package pt.isep.escala.escalaapi.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;

/** The Location of POST /api/v1/piquetes works (SCRUM-88). */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
class PiqueteLocationIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	MockMvc mockMvc;

	@Test
	@DisplayName("TC-SCRUM-88-05 the Location of a new piquete answers 200 with that piquete, for any role; an unknown ID answers 404")
	void locationOfANewPiqueteWorks() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/v1/piquetes")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dutyDate\":\"2026-12-12\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String location = created.getResponse().getHeader(HttpHeaders.LOCATION);

		for (String token : new String[] {MockTokens.member(), MockTokens.supervisor(), MockTokens.admin()}) {
			mockMvc.perform(get(location).header(HttpHeaders.AUTHORIZATION, token))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.dutyDate").value("2026-12-12"))
					.andExpect(jsonPath("$.slots.length()").value(1));
		}
		mockMvc.perform(get("/api/v1/piquetes/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(location)).andExpect(status().isUnauthorized());
	}
}
