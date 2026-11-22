package pt.isep.escala.escalaapi.metrics;

import java.util.Set;

/**
 * The only tags the Escala metrics may carry, and the only values they may take (SCRUM-97). Every value comes from a
 * closed list, so a metric never holds a member, assignment, slot, event or trace ID, nor free text (threat model T-07),
 * and the number of time series stays small. Anything outside a list becomes {@code OTHER}.
 */
public final class MetricTags {

	public static final String PURPOSE = "purpose";
	public static final String RESULT = "result";
	public static final String APPLIED = "applied";
	public static final String QUEUE = "queue";

	/** Tag keys allowed on {@code escala.*} meters; {@link MetricsConfiguration} drops a meter that uses another. */
	public static final Set<String> ALLOWED_KEYS = Set.of(PURPOSE, RESULT, APPLIED, "source", QUEUE, "application");

	public static final String OTHER = "OTHER";

	/** {@code AvaliacaoPedida.purpose}; CANDIDATE is for SCRUM-65 (a candidate checked before it is proposed). */
	private static final Set<String> PURPOSES = Set.of("ASSIGNMENT", "SWAP", "COVER", "CANDIDATE");

	private static final Set<String> RESULTS = Set.of("ELIGIBLE", "INELIGIBLE", "FAILED");

	private MetricTags() {
	}

	public static String purpose(String value) {
		return value != null && PURPOSES.contains(value) ? value : OTHER;
	}

	/** A result of {@code AvaliacaoConcluida} ({@code contracts/events/avaliacao-concluida.schema.json}). */
	public static String result(String value) {
		return value != null && RESULTS.contains(value) ? value : OTHER;
	}
}
