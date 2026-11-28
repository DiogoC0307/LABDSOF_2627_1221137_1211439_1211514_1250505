package pt.isep.escala.elegibilidadeworker.avaliacao;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import pt.isep.escala.elegibilidadeworker.messaging.MalformedEventException;
import tools.jackson.databind.JsonNode;

/**
 * The eligibility rules (ADR 0007).
 *
 * RN01: only a member on the active roster can be scheduled.
 * RN05: a member is never on two shifts at the same time. A shift that starts exactly when the other ends does
 * not overlap it
 * RN06: a member is never scheduled during a period they said they can't come. A period that ends exactly when
 * the shift starts does not overlap it.
 */
final class EligibilityRules {

	// Stored with every evaluation and sent in AvaliacaoConcluida (a change to a rule requires a new rule version)
	static final String ENGINE_VERSION = "REGRAS_V1";

	static final String ELIGIBLE = "ELIGIBLE";
	static final String INELIGIBLE = "INELIGIBLE";

	static final String RN01 = "RN01";
	static final String RN05 = "RN05";
	static final String RN06 = "RN06";

	private EligibilityRules() {
	}

	// Returns a list of violated rules, empty if none
	static List<String> violated(JsonNode slot, JsonNode candidate) {
		Instant start = instant(slot.path("start"), "slot.start");
		Instant end = instant(slot.path("end"), "slot.end");
		List<String> codes = new ArrayList<>();

		if (!candidate.path("inActiveRoster").asBoolean(false)) {
			codes.add(RN01);
		}

		// Overlaps with other shifts
		if (anyOverlaps(candidate.path("otherAssignments"), start, end, "candidate.otherAssignments")) {
			codes.add(RN05);
		}

		if (anyOverlaps(candidate.path("unavailabilities"), start, end, "candidate.unavailabilities")) {
			codes.add(RN06);
		}

		return codes;
	}

	static String result(List<String> violated) {
		return violated.isEmpty() ? ELIGIBLE : INELIGIBLE;
	}

	// Wheter any of the periods overlaps with the given start/end
	private static boolean anyOverlaps(JsonNode periods, Instant start, Instant end, String field) {
		for (JsonNode period : periods) {
			Instant from = instant(period.path("start"), field + ".start");
			Instant to = instant(period.path("end"), field + ".end");
			
			if (from.isBefore(end) && start.isBefore(to)) {
				return true;
			}
		}
		return false;
	}

	private static Instant instant(JsonNode value, String field) {
		try {
			return Instant.parse(value.asString(""));
		} catch (DateTimeParseException e) {
			throw new MalformedEventException(field + " is not an instant");
		}
	}
}
