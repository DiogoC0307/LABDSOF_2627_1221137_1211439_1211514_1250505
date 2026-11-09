package pt.isep.escala.elegibilidadeworker.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@Testcontainers
class FlywayFreshDatabaseIT {

	static final String COLUMNS = """
			SELECT table_name, column_name, data_type, character_maximum_length, datetime_precision, is_nullable
			FROM information_schema.columns
			WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
			ORDER BY table_name, column_name
			""";

	static final String CONSTRAINTS = """
			SELECT conrelid::regclass::text AS table_name, conname, pg_get_constraintdef(oid) AS definition
			FROM pg_constraint
			WHERE connamespace = 'public'::regnamespace AND conrelid::regclass::text <> 'flyway_schema_history'
			ORDER BY 1, 2
			""";

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	JdbcTemplate jdbc;

	@Test
	@DisplayName("TC-SCRUM-87-01 an empty database is created by the migrations and matches the 0.3.0 schema")
	void emptyDatabaseIsMigratedAndMatchesTheSchemaHibernateCreated() throws Exception {
		assertThat(jdbc.queryForList(
				"SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class))
				.startsWith("1");

		jdbc.execute("CREATE DATABASE legacy");
		String legacyUrl = postgres.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/legacy$1");
		try (Connection connection = DriverManager.getConnection(legacyUrl, postgres.getUsername(), postgres.getPassword());
				Statement statement = connection.createStatement()) {
			statement.execute(new ClassPathResource("db/schema-0.3.0-ddl-auto.sql")
					.getContentAsString(StandardCharsets.UTF_8));
			JdbcTemplate legacy = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

			assertThat(rows(jdbc, COLUMNS)).isEqualTo(rows(legacy, COLUMNS));
			assertThat(rows(jdbc, CONSTRAINTS)).isEqualTo(rows(legacy, CONSTRAINTS));
		}
	}

	static List<String> rows(JdbcTemplate template, String sql) {
		return template.queryForList(sql).stream().map(Object::toString).toList();
	}
}
