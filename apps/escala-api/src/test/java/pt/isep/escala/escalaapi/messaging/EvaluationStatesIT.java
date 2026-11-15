package pt.isep.escala.escalaapi.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** The evaluation states a client sees on GET /api/v1/piquetes: PENDING, then ELIGIBLE or EVALUATION_FAILED. */
@SpringBootTest(properties = {
		"spring.rabbitmq.dynamic=true",
		"escala.messaging.retry-stages=" + MessagingTestSupport.TEST_RETRY_STAGES})
@AutoConfigureMockMvc
@Testcontainers
class EvaluationStatesIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	static CachingConnectionFactory connection;

	@BeforeAll
	static void shortRetryQueues() {
		connection = MessagingTestSupport.adminConnection(rabbit);
		MessagingTestSupport.declareShortRetryQueues(new RabbitAdmin(connection), MessagingTestSupport.QUEUE);
	}

	@AfterAll
	static void closeBroker() {
		connection.destroy();
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ObjectMapper objectMapper;

	@Test
	@DisplayName("TC-SCRUM-95-03 a new slot is PENDING and becomes ELIGIBLE when the evaluation is ELIGIBLE")
	void pendingThenEvaluated() throws Exception {
		UUID slotId = createPiquete();
		assertThat(statusThroughApi(slotId)).isEqualTo("PENDING");

		MessagingTestSupport.sendConcluida(rabbitTemplate, UUID.randomUUID(), assignmentOf(slotId), "ELIGIBLE");

		await().atMost(Duration.ofSeconds(15)).until(() -> statusThroughApi(slotId).equals("ELIGIBLE"));
	}

	@Test
	@DisplayName("TC-SCRUM-95-03 a FAILED evaluation shows EVALUATION_FAILED and never makes the slot ELIGIBLE")
	void failedEvaluationIsVisibleAndNeverEligible() throws Exception {
		UUID slotId = createPiquete();

		MessagingTestSupport.sendConcluida(rabbitTemplate, UUID.randomUUID(), assignmentOf(slotId), "FAILED");

		await().atMost(Duration.ofSeconds(15)).until(() -> statusThroughApi(slotId).equals("EVALUATION_FAILED"));
	}

	@Test
	@DisplayName("TC-SCRUM-95-03 a FAILED evaluation does not change a slot that is already ELIGIBLE")
	void failedEvaluationDoesNotChangeAnEligibleSlot() throws Exception {
		UUID slotId = createPiquete();
		UUID assignmentId = assignmentOf(slotId);
		MessagingTestSupport.sendConcluida(rabbitTemplate, UUID.randomUUID(), assignmentId, "ELIGIBLE");
		await().atMost(Duration.ofSeconds(15)).until(() -> statusThroughApi(slotId).equals("ELIGIBLE"));

		UUID failedEventId = UUID.randomUUID();
		MessagingTestSupport.sendConcluida(rabbitTemplate, failedEventId, assignmentId, "FAILED");

		await().atMost(Duration.ofSeconds(15)).until(() -> jdbc.queryForObject(
				"SELECT count(*) FROM processed_event WHERE event_id = ?", Integer.class, failedEventId) == 1);
		assertThat(statusThroughApi(slotId)).isEqualTo("ELIGIBLE");
	}

	@Test
	@DisplayName("TC-SCRUM-95-03 when escala-api cannot apply an evaluation after the retries, the slot shows EVALUATION_FAILED")
	void exhaustedRetriesShowEvaluationFailed() throws Exception {
		UUID slotId = createPiquete();
		UUID assignmentId = assignmentOf(slotId);
		jdbc.execute("ALTER TABLE slot ADD CONSTRAINT tc_scrum_95_03 CHECK (assignment_id <> '" + assignmentId
				+ "'::uuid OR status <> 'ELIGIBLE')");
		try {
			MessagingTestSupport.sendConcluida(rabbitTemplate, UUID.randomUUID(), assignmentId, "ELIGIBLE");

			await().atMost(Duration.ofSeconds(30)).until(() -> statusThroughApi(slotId).equals("EVALUATION_FAILED"));
		} finally {
			jdbc.execute("ALTER TABLE slot DROP CONSTRAINT tc_scrum_95_03");
		}
	}

	private UUID createPiquete() throws Exception {
		String body = mockMvc.perform(post("/api/v1/piquetes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dutyDate\":\"2031-08-02\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(objectMapper.readTree(body).path("slots").path(0).path("id").asText());
	}

	private UUID assignmentOf(UUID slotId) {
		return jdbc.queryForObject("SELECT assignment_id FROM slot WHERE id = ?", UUID.class, slotId);
	}

	private String statusThroughApi(UUID slotId) throws Exception {
		String body = mockMvc.perform(get("/api/v1/piquetes")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		for (JsonNode piquete : objectMapper.readTree(body)) {
			for (JsonNode slot : piquete.path("slots")) {
				if (slot.path("id").asText().equals(slotId.toString())) {
					return slot.path("status").asText();
				}
			}
		}
		throw new AssertionError("slot not listed: " + slotId);
	}
}
