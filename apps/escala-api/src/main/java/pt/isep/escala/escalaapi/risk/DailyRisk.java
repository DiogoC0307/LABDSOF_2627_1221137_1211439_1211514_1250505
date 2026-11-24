package pt.isep.escala.escalaapi.risk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** Display-only fire risk for the corps municipality (ADR 0010). It does not size the crew. */
@Entity
@Table(name = "daily_risk")
class DailyRisk {

	@Id
	private String dico;

	@Column(nullable = false)
	private int rcm;

	@Column(nullable = false)
	private LocalDate forecastDate;

	@Column(nullable = false)
	private Instant fetchedAt;

	@Column(nullable = false)
	private String source;

	@Column(nullable = false)
	private boolean assumedHigher;

	@Column(nullable = false)
	private Instant updatedAt;

	protected DailyRisk() {
	}

	DailyRisk(String dico, int rcm, LocalDate forecastDate, Instant fetchedAt, String source, boolean assumedHigher,
			Instant updatedAt) {
		this.dico = dico;
		this.rcm = rcm;
		this.forecastDate = forecastDate;
		this.fetchedAt = fetchedAt;
		this.source = source;
		this.assumedHigher = assumedHigher;
		this.updatedAt = updatedAt;
	}

	/** Ignores an event older than the one already stored. */
	boolean apply(int rcm, LocalDate forecastDate, Instant fetchedAt, String source, boolean assumedHigher,
			Instant occurredAt) {
		if (updatedAt != null && occurredAt.isBefore(updatedAt)) {
			return false;
		}
		this.rcm = rcm;
		this.forecastDate = forecastDate;
		this.fetchedAt = fetchedAt;
		this.source = source;
		this.assumedHigher = assumedHigher;
		this.updatedAt = occurredAt;
		return true;
	}

	String getDico() {
		return dico;
	}

	int getRcm() {
		return rcm;
	}

	LocalDate getForecastDate() {
		return forecastDate;
	}

	Instant getFetchedAt() {
		return fetchedAt;
	}

	String getSource() {
		return source;
	}

	boolean isAssumedHigher() {
		return assumedHigher;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}
}
