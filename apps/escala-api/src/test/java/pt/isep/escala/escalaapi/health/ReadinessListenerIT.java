package pt.isep.escala.escalaapi.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/** A stopped listener makes readiness DOWN, so the pod is not Ready and the problem is visible (SCRUM-88). */
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
	@DisplayName("TC-SCRUM-88-03 a stopped listener container makes readiness 503 and liveness stays 200; started again, readiness is 200")
	void readinessFollowsTheListeners() throws Exception {
		mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());

		listeners.getListenerContainers().iterator().next().stop();
		try {
			mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isServiceUnavailable());
			mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
		} finally {
			listeners.start();
		}
		mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
	}
}
