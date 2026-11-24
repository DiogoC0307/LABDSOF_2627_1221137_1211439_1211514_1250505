package pt.isep.escala.elegibilidadeworker.risk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "risk_observation")
class RiskObservation {

	@Id
	private String dico;

	private Integer rcm;

	private LocalDate forecastDate;

	private Instant validAt;

	@Column(nullable = false)
	private String source;

	@Column(nullable = false)
	private boolean assumedHigher;

	@Column(nullable = false)
	private String lastOutcome;

	@Column(nullable = false)
	private Instant lastPolledAt;

	protected RiskObservation() {
	}

	RiskObservation(RiskSnapshot snapshot) {
		copy(snapshot);
	}

	void copy(RiskSnapshot snapshot) {
		this.dico = snapshot.dico();
		this.rcm = snapshot.rcm();
		this.forecastDate = snapshot.forecastDate();
		this.validAt = snapshot.validAt();
		this.source = snapshot.source();
		this.assumedHigher = snapshot.assumedHigher();
		this.lastOutcome = snapshot.lastOutcome().name();
		this.lastPolledAt = snapshot.lastPolledAt();
	}

	RiskSnapshot snapshot() {
		return new RiskSnapshot(dico, rcm, forecastDate, validAt, assumedHigher, RiskOutcome.valueOf(lastOutcome),
				lastPolledAt, source);
	}
}
