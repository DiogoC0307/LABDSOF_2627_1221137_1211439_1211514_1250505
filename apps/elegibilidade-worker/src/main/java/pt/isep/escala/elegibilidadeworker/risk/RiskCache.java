package pt.isep.escala.elegibilidadeworker.risk;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Applies one poll to the cache (ADR 0013). A usable reading replaces the cache. An unusable reading keeps the last
 * valid RCM and never invents a lower one. The warning ({@code assumedHigher}) is raised when the value is at least
 * as old as the corps threshold, and immediately when the body is incomplete or invalid.
 */
final class RiskCache {

	private RiskCache() {
	}

	record Reading(RiskOutcome outcome, Integer rcm, LocalDate forecastDate, Instant fileAt) {
	}

	static RiskSnapshot apply(RiskSnapshot current, Reading reading, Instant now, Duration staleness, String source) {
		String dico = current == null ? null : current.dico();
		return switch (reading.outcome()) {
			case OK -> new RiskSnapshot(dico, reading.rcm(), reading.forecastDate(), now, false, RiskOutcome.OK, now,
					source);
			case DELAYED -> delayed(current, reading, now, staleness, source);
			case UNAVAILABLE -> keep(current, dico, RiskOutcome.UNAVAILABLE, now, staleness, source, false);
			case INCOMPLETE -> keep(current, dico, RiskOutcome.INCOMPLETE, now, staleness, source, true);
			case INVALID -> keep(current, dico, RiskOutcome.INVALID, now, staleness, source, true);
		};
	}

	private static RiskSnapshot delayed(RiskSnapshot current, Reading reading, Instant now, Duration staleness,
			String source) {
		RiskSnapshot shown = current;
		if (reading.rcm() != null && reading.fileAt() != null
				&& (current == null || current.validAt() == null || reading.fileAt().isAfter(current.validAt()))) {
			shown = new RiskSnapshot(current == null ? null : current.dico(), reading.rcm(), reading.forecastDate(),
					reading.fileAt(), false, RiskOutcome.DELAYED, now, source);
		}
		return keep(shown, shown == null ? null : shown.dico(), RiskOutcome.DELAYED, now, staleness, source, false);
	}

	/** Keeps the last valid value. {@code forceWarning} is the incomplete and invalid cases. */
	private static RiskSnapshot keep(RiskSnapshot current, String dico, RiskOutcome outcome, Instant now,
			Duration staleness, String source, boolean forceWarning) {
		if (current == null || current.rcm() == null || current.validAt() == null) {
			return RiskSnapshot.empty(dico, outcome, now, source);
		}
		boolean stale = !current.validAt().isAfter(now.minus(staleness));
		return new RiskSnapshot(current.dico(), current.rcm(), current.forecastDate(), current.validAt(),
				forceWarning || stale, outcome, now, source);
	}
}
