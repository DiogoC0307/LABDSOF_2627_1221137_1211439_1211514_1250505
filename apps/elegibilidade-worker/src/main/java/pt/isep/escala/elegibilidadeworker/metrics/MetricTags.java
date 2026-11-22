package pt.isep.escala.elegibilidadeworker.metrics;

import java.util.Set;

/**
 * The only tags the Escala metrics may carry, and the only values they may take (SCRUM-97). Every value comes from a
 * closed list, so a metric never holds a member, assignment, slot, event or trace ID, nor free text (threat model T-07),
 * and the number of time series stays small. Anything outside a list becomes {@code OTHER}.
 */
public final class MetricTags {

	public static final String RESULT = "result";
	public static final String SOURCE = "source";
	public static final String ROUTING_KEY = "routing_key";
	public static final String QUEUE = "queue";

	/** Tag keys allowed on {@code escala.*} meters; {@link MetricsConfiguration} drops a meter that uses another. */
	public static final Set<String> ALLOWED_KEYS = Set.of("purpose", RESULT, "applied", SOURCE, ROUTING_KEY, QUEUE,
			"application");

	public static final String OTHER = "OTHER";

	private static final Set<String> RESULTS = Set.of("ELIGIBLE", "INELIGIBLE", "FAILED");

	private MetricTags() {
	}

	/** A result of {@code AvaliacaoConcluida} ({@code contracts/events/avaliacao-concluida.schema.json}). */
	public static String result(String value) {
		return value != null && RESULTS.contains(value) ? value : OTHER;
	}
}
