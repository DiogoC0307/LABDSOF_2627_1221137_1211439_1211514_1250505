package pt.isep.escala.elegibilidadeworker.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The evaluation metrics of the worker (SCRUM-97):
 *
 * <ul>
 * <li>{@code escala.evaluations.completed{result, source}}: evaluations answered, by result (ELIGIBLE, INELIGIBLE,
 * FAILED) and by where the answer came from ({@code listener}, or {@code retries_exhausted});</li>
 * <li>{@code escala.evaluation.processing}: from reading an {@code AvaliacaoPedida} to writing its answer in the
 * outbox. Part of SCRUM-97's SLI 1, which escala-api measures end to end.</li>
 * </ul>
 */
@Component
public class EvaluationMetrics {

	public static final String LISTENER = "listener";
	public static final String RETRIES_EXHAUSTED = "retries_exhausted";

	private final MeterRegistry registry;
	private final Timer processing;

	public EvaluationMetrics(MeterRegistry registry) {
		this.registry = registry;
		this.processing = Timer.builder("escala.evaluation.processing")
				.description("From reading an AvaliacaoPedida to writing its answer in the outbox")
				.publishPercentileHistogram().register(registry);
		// every series exists at 0 from the start, so rates and the dashboard have data before the first evaluation
		for (String result : MetricTags.RESULTS) {
			completedCounter(result, LISTENER);
			completedCounter(result, RETRIES_EXHAUSTED);
		}
	}

	public void completed(String result, String source) {
		Counter counter = completedCounter(MetricTags.result(result),
				RETRIES_EXHAUSTED.equals(source) ? RETRIES_EXHAUSTED : LISTENER);
		afterCommit(counter::increment);
	}

	private Counter completedCounter(String result, String source) {
		return Counter.builder("escala.evaluations.completed").description("Evaluations answered by the worker")
				.tag(MetricTags.RESULT, result).tag(MetricTags.SOURCE, source).register(registry);
	}

	public void processed(Duration duration) {
		afterCommit(() -> processing.record(duration));
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
