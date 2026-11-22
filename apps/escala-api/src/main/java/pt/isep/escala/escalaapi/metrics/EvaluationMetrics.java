package pt.isep.escala.escalaapi.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The evaluation metrics of escala-api (SCRUM-97):
 *
 * <ul>
 * <li>{@code escala.evaluations.requested{purpose}}: AvaliacaoPedida written to the outbox;</li>
 * <li>{@code escala.evaluations.completed{result, applied}}: AvaliacaoConcluida applied, by result, and whether it
 * changed the slot;</li>
 * <li>{@code escala.evaluation.latency{result}}: <b>SLI 1</b> (07 · Technical Design, RNF01, p95 &lt; 5 s): from the
 * moment the AvaliacaoPedida was written to the outbox to the moment its AvaliacaoConcluida is applied. The start is
 * the {@code created_at} of the outbox row whose id is the {@code requestEventId} of the answer, so the event contract
 * does not change. It has a bucket at exactly 5 s, so the share within the target is exact.</li>
 * </ul>
 */
@Component
public class EvaluationMetrics {

	/** SLI 1 target (RNF01); also the bucket that makes "within 5 s" exact. */
	public static final Duration SLI_1_TARGET = Duration.ofSeconds(5);

	private final MeterRegistry registry;
	private final JdbcTemplate jdbc;

	public EvaluationMetrics(MeterRegistry registry, JdbcTemplate jdbc) {
		this.registry = registry;
		this.jdbc = jdbc;
		for (String result : List.of("ELIGIBLE", "INELIGIBLE", "FAILED")) {
			latency(result);
		}
	}

	public void requested(String purpose) {
		Counter.builder("escala.evaluations.requested").description("AvaliacaoPedida written to the outbox")
				.tag(MetricTags.PURPOSE, MetricTags.purpose(purpose)).register(registry).increment();
	}

	/**
	 * An AvaliacaoConcluida applied for the first time (not a duplicate). The latency is recorded when the request is
	 * still in this service's outbox, which is always the case unless the outbox rows were cleaned.
	 */
	public void completed(UUID requestEventId, String result, boolean applied, Instant appliedAt) {
		String tag = MetricTags.result(result);
		Optional<Instant> start = requestedAt(requestEventId);
		afterCommit(() -> {
			Counter.builder("escala.evaluations.completed").description("AvaliacaoConcluida applied by escala-api")
					.tag(MetricTags.RESULT, tag).tag(MetricTags.APPLIED, Boolean.toString(applied)).register(registry)
					.increment();
			start.ifPresent(requested -> latency(tag).record(Duration.between(requested, appliedAt)));
		});
	}

	private Timer latency(String result) {
		return Timer.builder("escala.evaluation.latency")
				.description("From AvaliacaoPedida in the outbox to AvaliacaoConcluida applied (SLI 1)")
				.tag(MetricTags.RESULT, result).publishPercentileHistogram()
				.serviceLevelObjectives(Duration.ofSeconds(1), Duration.ofSeconds(2), SLI_1_TARGET,
						Duration.ofSeconds(10), Duration.ofSeconds(30))
				.register(registry);
	}

	private Optional<Instant> requestedAt(UUID requestEventId) {
		if (requestEventId == null) {
			return Optional.empty();
		}
		List<OffsetDateTime> created = jdbc.queryForList("select created_at from outbox_event where id = ?",
				OffsetDateTime.class, requestEventId);
		return created.stream().findFirst().map(OffsetDateTime::toInstant);
	}

	/** Inside a transaction, after it commits (a rolled-back delivery is retried and must not count twice); else now. */
	static void afterCommit(Runnable record) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					record.run();
				}
			});
		}
		else {
			record.run();
		}
	}
}
