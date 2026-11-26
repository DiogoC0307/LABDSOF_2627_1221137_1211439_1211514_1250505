package pt.isep.escala.escalaapi.risk;

import java.time.Instant;

/** GET /api/v1/risk/today. Null fields mean there is no cached value yet. */
record TodayRisk(
		String dico,
		Integer rcm,
		String label,
		Instant fetchedAt,
		Double dataAgeHours,
		String source,
		boolean assumedHigher) {

	static TodayRisk none(String dico) {
		return new TodayRisk(dico, null, null, null, null, "IPMA", false);
	}
}
