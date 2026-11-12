package pt.isep.escala.elegibilidadeworker.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@Testcontainers
class FlywayExistingDatabaseIT {

	// A database as release 0.3.0 left it: created by ddl-auto=update, no flyway_schema_history, one evaluation.
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
			.withInitScript("db/schema-0.3.0-ddl-auto.sql");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	JdbcTemplate jdbc;

	@Test
	@DisplayName("TC-SCRUM-87-02 a database created before Flyway is baselined at version 1 and keeps its data")
	void databaseCreatedByDdlAutoIsBaselinedAndKeepsItsData() {
		Map<String, Object> first = jdbc.queryForMap(
				"SELECT version, type FROM flyway_schema_history ORDER BY installed_rank LIMIT 1");
		assertThat(first).containsEntry("version", "1").containsEntry("type", "BASELINE");
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM flyway_schema_history WHERE type = 'SQL' AND version = '1'", Integer.class))
				.isZero();

		assertThat(jdbc.queryForObject(
				"SELECT trace_id FROM evaluation WHERE id = '44444444-4444-4444-8444-444444444444'", String.class))
				.isEqualTo("trace-0.3.0");
	}
}
