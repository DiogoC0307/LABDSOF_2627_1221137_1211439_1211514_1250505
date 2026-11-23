package pt.isep.escala.escalaapi.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import pt.isep.escala.escalaapi.outbox.OutboxEventRepository;

/**
 * With RabbitMQ down the API keeps serving: readiness stays 200, /actuator/health answers 503 so the problem is
 * visible, and a new piquete is created with its event waiting in the outbox (SCRUM-88). This class stops its own
 * broker.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
class ReadinessBrokerIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	OutboxEventRepository outbox;

	@Test
	@DisplayName("TC-SCRUM-88-02 with RabbitMQ down, readiness and liveness stay 200, /actuator/health answers 503 within 5 s, and POST /api/v1/piquetes answers 201 with the event in the outbox")
	void theApiKeepsServingWithTheBrokerDown() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

		rabbit.stop();

		long start = System.nanoTime();
		mockMvc.perform(get("/actuator/health")).andExpect(status().isServiceUnavailable());
		assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
		mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());

		int waiting = outbox.findByPublishedAtIsNullOrderByCreatedAtAsc().size();
		mockMvc.perform(post("/api/v1/piquetes")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dutyDate\":\"2026-12-12\"}"))
				.andExpect(status().isCreated());
		assertThat(outbox.findByPublishedAtIsNullOrderByCreatedAtAsc()).hasSize(waiting + 1);
	}
}
