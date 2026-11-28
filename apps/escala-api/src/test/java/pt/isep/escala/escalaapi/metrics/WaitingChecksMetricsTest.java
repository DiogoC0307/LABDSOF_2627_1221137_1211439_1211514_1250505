package pt.isep.escala.escalaapi.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

/** {@code escala.checks.waiting} without a database (SCRUM-98). */
class WaitingChecksMetricsTest {

	/** Answers the counts with fixed numbers, or fails as a database that is down. */
	static class FakeJdbc extends JdbcTemplate {
		boolean down;
		long assignment;

		@Override
		public void query(String sql, RowCallbackHandler handler) {
			if (down) {
				throw new DataAccessResourceFailureException("database down");
			}
			ResultSet rs = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] { ResultSet.class }, (proxy, method, args) -> switch (String.valueOf(args[0])) {
						case "assignment" -> assignment;
						case "candidate" -> 2L;
						default -> 3L;
					});
			try {
				handler.processRow(rs);
			}
			catch (SQLException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	@Test
	@DisplayName("TC-SCRUM-98-05 purpose is only ASSIGNMENT, CANDIDATE or EXTRACTION, each at 0 from the start; with the database down the last values stay")
	void closedPurposesAndDatabaseDown() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		FakeJdbc jdbc = new FakeJdbc();
		WaitingChecksMetrics metrics = new WaitingChecksMetrics(registry, jdbc);

		assertThat(registry.find("escala.checks.waiting").gauges())
				.extracting(gauge -> gauge.getId().getTag("purpose"))
				.containsExactlyInAnyOrder("ASSIGNMENT", "CANDIDATE", "EXTRACTION");
		assertThat(registry.find("escala.checks.waiting").gauges()).allMatch(gauge -> gauge.value() == 0);

		jdbc.assignment = 4;
		metrics.refresh();
		assertThat(registry.get("escala.checks.waiting").tag("purpose", "ASSIGNMENT").gauge().value()).isEqualTo(4);
		assertThat(registry.get("escala.checks.waiting").tag("purpose", "CANDIDATE").gauge().value()).isEqualTo(2);
		assertThat(registry.get("escala.checks.waiting").tag("purpose", "EXTRACTION").gauge().value()).isEqualTo(3);

		jdbc.down = true;
		jdbc.assignment = 9;
		metrics.refresh();
		assertThat(registry.get("escala.checks.waiting").tag("purpose", "ASSIGNMENT").gauge().value()).isEqualTo(4);
	}
}
