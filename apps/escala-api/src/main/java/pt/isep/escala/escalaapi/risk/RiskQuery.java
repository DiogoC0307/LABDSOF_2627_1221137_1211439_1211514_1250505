package pt.isep.escala.escalaapi.risk;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Today's fire risk for the supervisor. Age is computed when it is read, so it stays honest between polls. */
@Service
class RiskQuery {

	private final DailyRiskRepository risks;
	private final Clock clock;
	private final String dico;
	private final Duration staleness;

	RiskQuery(
			DailyRiskRepository risks,
			Clock clock,
			@Value("${escala.risk.dico:1312}") String dico,
			@Value("${escala.risk.staleness:3h}") Duration staleness) {
		this.risks = risks;
		this.clock = clock;
		this.dico = dico;
		this.staleness = staleness;
	}

	TodayRisk today() {
		return risks.findById(dico).map(this::from).orElseGet(() -> TodayRisk.none(dico));
	}

	private TodayRisk from(DailyRisk risk) {
		Instant now = clock.instant();
		double ageHours = Math.max(0, Duration.between(risk.getFetchedAt(), now).toMillis() / 3_600_000d);
		boolean assumedHigher = risk.isAssumedHigher() || !risk.getFetchedAt().isAfter(now.minus(staleness));
		return new TodayRisk(risk.getDico(), risk.getRcm(), label(risk.getRcm()), risk.getFetchedAt(), ageHours,
				risk.getSource(), assumedHigher);
	}

	static String label(int rcm) {
		return switch (rcm) {
			case 1 -> "low";
			case 2 -> "moderate";
			case 3 -> "high";
			case 4 -> "very high";
			case 5 -> "maximum";
			default -> null;
		};
	}
}
