package pt.isep.escala.escalaapi.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * {@code escala.checks.waiting{purpose}} (SCRUM-98): the requests escala-api sent to elegibilidade-worker that still wait
 * for an answer, counted in escala-api's own database every 15 s, as the DLQ gauge is. It is not the size of the
 * worker's queues: it also counts a request the worker is retrying, and escala-api's broker user has no permission on
 * the {@code worker.*} queues on purpose (least privilege). With the worker down it grows; when the worker is back it
 * drains.
 *
 * <ul>
 * <li>{@code ASSIGNMENT}: slots still {@code PENDING} and week assignments whose evaluation is {@code PENDING};</li>
 * <li>{@code CANDIDATE}: "who can come" checks still {@code PENDING} (SCRUM-171);</li>
 * <li>{@code EXTRACTION}: free-text messages still {@code PENDING} (SCRUM-75).</li>
 * </ul>
 *
 * Each count reads only an index on (state, date) of V11, never the whole table. Covers and swaps get their own value
 * when they exist (SCRUM-68, 69, 72, 73).
 */
@Component
public class WaitingChecksMetrics {

	static final String ASSIGNMENT = "ASSIGNMENT";
	static final String CANDIDATE = "CANDIDATE";
	static final String EXTRACTION = "EXTRACTION";

	/** The only values of {@code purpose} on this gauge (T-07). */
	static final List<String> PURPOSES = List.of(ASSIGNMENT, CANDIDATE, EXTRACTION);

	static final String COUNTS = """
			select (select count(*) from slot where status = 'PENDING')
			     + (select count(*) from assignment where evaluation_state = 'PENDING') as assignment,
			       (select count(*) from candidate_check where evaluation_state = 'PENDING') as candidate,
			       (select count(*) from unavailability_message where status = 'PENDING') as extraction
			""";

	private static final Logger log = LoggerFactory.getLogger(WaitingChecksMetrics.class);

	private final JdbcTemplate jdbc;
	private final Map<String, AtomicLong> waiting = new LinkedHashMap<>();

	public WaitingChecksMetrics(MeterRegistry registry, JdbcTemplate jdbc) {
		this.jdbc = jdbc;
		// every purpose shows 0 from the start, so the panel has a line before the first request
		for (String purpose : PURPOSES) {
			AtomicLong value = new AtomicLong();
			waiting.put(purpose, value);
			Gauge.builder("escala.checks.waiting", value, AtomicLong::get)
					.description("Requests sent to elegibilidade-worker still waiting for an answer, retries included")
					.tag(MetricTags.PURPOSE, purpose).register(registry);
		}
	}

	@Scheduled(fixedDelay = 15_000, initialDelay = 5_000)
	void refresh() {
		try {
			jdbc.query(COUNTS, rs -> {
				waiting.get(ASSIGNMENT).set(rs.getLong("assignment"));
				waiting.get(CANDIDATE).set(rs.getLong("candidate"));
				waiting.get(EXTRACTION).set(rs.getLong("extraction"));
			});
		}
		catch (RuntimeException e) {
			// the database is down: keep the last values; readiness already reports it
			log.debug("waiting checks metric not refreshed exception={}", e.getClass().getSimpleName());
		}
	}

	long waiting(String purpose) {
		return waiting.get(purpose).get();
	}
}
