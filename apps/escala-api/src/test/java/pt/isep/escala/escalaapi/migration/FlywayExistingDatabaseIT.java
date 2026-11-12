package pt.isep.escala.escalaapi.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
class FlywayExistingDatabaseIT {

	// A database as release 0.3.0 left it: created by ddl-auto=update, no flyway_schema_history, one piquete.
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
			.withInitScript("db/schema-0.3.0-ddl-auto.sql");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MockMvc mockMvc;

	@Test
	@DisplayName("TC-SCRUM-87-02 a database created before Flyway is baselined at version 1 and keeps its data")
	void databaseCreatedByDdlAutoIsBaselinedAndKeepsItsData() throws Exception {
		Map<String, Object> first = jdbc.queryForMap(
				"SELECT version, type FROM flyway_schema_history ORDER BY installed_rank LIMIT 1");
		assertThat(first).containsEntry("version", "1").containsEntry("type", "BASELINE");
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM flyway_schema_history WHERE type = 'SQL' AND version = '1'", Integer.class))
				.isZero();

		mockMvc.perform(get("/api/v1/piquetes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("11111111-1111-4111-8111-111111111111"))
				.andExpect(jsonPath("$[0].slots[0].status").value("ELIGIBLE"));
	}
}
