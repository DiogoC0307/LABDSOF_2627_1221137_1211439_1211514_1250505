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
		this.id = UUID.randomUUID();
		this.memberId = memberId;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.source = SOURCE_FORM;
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
