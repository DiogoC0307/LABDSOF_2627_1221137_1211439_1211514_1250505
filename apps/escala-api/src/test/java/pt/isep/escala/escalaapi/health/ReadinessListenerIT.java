package pt.isep.escala.escalaapi.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;

/**
 * A stopped listener is visible in /actuator/health (503) but does not take the API out of the Service: readiness
 * stays 200 and requests are served (SCRUM-88).
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
class ReadinessListenerIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	RabbitListenerEndpointRegistry listeners;

	@Test
	@DisplayName("TC-SCRUM-88-03 a stopped listener container makes /actuator/health 503 while readiness and liveness stay 200 and POST /api/v1/piquetes answers 201; started again, /actuator/health is 200")
	void aStoppedListenerIsVisibleButTheApiKeepsServing() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

		listeners.getListenerContainers().iterator().next().stop();
		try {
			mockMvc.perform(get("/actuator/health")).andExpect(status().isServiceUnavailable());
			mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
			mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
			mockMvc.perform(post("/api/v1/piquetes")
							.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor())
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"dutyDate\":\"2026-12-12\"}"))
					.andExpect(status().isCreated());
		} finally {
			listeners.start();
		}
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}
}
