package pt.isep.escala.escalaapi.piquete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Weekly schedule publication attempt.
 * 
 * When publsihed, the week status remains as CHECKING while the eligibility rules evaluate its assignments. 
 * If the week is eligible, it becomes PUBLISHED (members see the week and are notified). 
 * If not, it becomes BLOCKED (the supervisor sees the conflicts; nothing changed).
 */
@Entity
@Table(name = "week_publication")
class WeekPublication {

	enum Status {
		CHECKING,
		PUBLISHED,
		BLOCKED,
		SUPERSEDED
	}

	@Id
	private UUID id;

	@Column(nullable = false, length = 8)
	private String week;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Status status;

	@Column(nullable = false)
	private UUID requestedBy;

	// The week as the supervisor sent it, as JSON ({@link PublicationPlan}).
	@Column(nullable = false, columnDefinition = "TEXT")
	private String plan;

	@Column(nullable = false)
	private Instant createdAt;

	// When it was published, blocked or superseded
	@Column
	private Instant completedAt;

	protected WeekPublication() {
	}

	WeekPublication(UUID id, String week, UUID requestedBy, String plan, Instant createdAt) {
		this.id = id;
		this.week = week;
		this.status = Status.CHECKING;
		this.requestedBy = requestedBy;
		this.plan = plan;
		this.createdAt = createdAt;
	}

	UUID getId() {
		return id;
	}

	String getWeek() {
		return week;
	}

	Status getStatus() {
		return status;
	}

	String getPlan() {
		return plan;
	}

	boolean isChecking() {
		return status == Status.CHECKING;
	}

	void published(Instant at) {
		this.status = Status.PUBLISHED;
		this.completedAt = at;
	}

	void blocked(Instant at) {
		this.status = Status.BLOCKED;
		this.completedAt = at;
	}

	void superseded(Instant at) {
		this.status = Status.SUPERSEDED;
		this.completedAt = at;
	}
}
