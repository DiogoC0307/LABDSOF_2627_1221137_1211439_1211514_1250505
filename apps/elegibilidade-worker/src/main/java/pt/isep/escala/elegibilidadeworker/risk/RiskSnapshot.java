package pt.isep.escala.elegibilidadeworker.risk;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The cached RCM for one municipality and what the latest poll did with it (ADR 0010, ADR 0013).
 * A poll never adds or removes slots.
 */
record RiskSnapshot(
		String dico,
		Integer rcm,
		LocalDate forecastDate,
		Instant validAt,
		boolean assumedHigher,
		RiskOutcome lastOutcome,
		Instant lastPolledAt,
		String source) {

	static RiskSnapshot empty(String dico, RiskOutcome outcome, Instant polledAt, String source) {
		return new RiskSnapshot(dico, null, null, null, true, outcome, polledAt, source);
	}

	boolean publishable() {
		return rcm != null && forecastDate != null && validAt != null;
	}
}
