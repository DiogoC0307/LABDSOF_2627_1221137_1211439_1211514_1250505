package pt.isep.escala.escalaapi.unavailability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A period in which a member cannot serve, [startsAt, endsAt) in UTC (SCRUM-64, ADR 0012). Dates and times only,
 * never a reason (09).
 */
@Entity
@Table(name = "unavailability")
class Unavailability {

	static final String SOURCE_FORM = "FORM";
	static final String SOURCE_FREE_TEXT = "FREE_TEXT";

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID memberId;

	@Column(nullable = false)
	private Instant startsAt;

	@Column(nullable = false)
	private Instant endsAt;

	@Column(nullable = false)
	private String source;

	@Column(nullable = false)
	private Instant createdAt;

	protected Unavailability() {
	}

	Unavailability(UUID memberId, Instant startsAt, Instant endsAt, Instant createdAt) {
		this(memberId, startsAt, endsAt, SOURCE_FORM, createdAt);
	}

	/** {@code source}: FORM (SCRUM-64) or FREE_TEXT, confirmed from a message (SCRUM-75). */
	Unavailability(UUID memberId, Instant startsAt, Instant endsAt, String source, Instant createdAt) {
		this.id = UUID.randomUUID();
		this.memberId = memberId;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.source = source;
		this.createdAt = createdAt;
	}

	UUID getId() {
		return id;
	}

	UUID getMemberId() {
		return memberId;
	}

	Instant getStartsAt() {
		return startsAt;
	}

	Instant getEndsAt() {
		return endsAt;
	}

	String getSource() {
		return source;
	}
}
