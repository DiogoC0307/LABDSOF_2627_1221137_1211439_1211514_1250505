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
 * A request to cover one slot whose member can't come (SCRUM-72; opened by SCRUM-71). OPEN until it is filled or its
 * shift starts. The supervisor is alerted at most once: when nobody is free and qualified, or when nobody accepts in
 * time.
 */
@Entity
@Table(name = "cover_request")
class CoverRequest {

	/** Why the slot opened: the member said they can't come, or did not answer in time (SCRUM-71 AC2). */
	enum Reason {
		CANNOT_COME,
		NO_ANSWER
	}

	enum Status {
		OPEN,
		FILLED,
		CLOSED
	}

	/** Why the supervisor was alerted (SCRUM-72 AC3 and AC4). */
	enum Escalation {
		NO_CANDIDATES,
		NO_ACCEPTANCE
	}

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID slotId;

	@Column(nullable = false)
	private UUID piqueteId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 11)
	private SlotRole role;

	@Column(nullable = false)
	private Instant startsAt;

	@Column(nullable = false)
	private Instant endsAt;

	private UUID vacatedMemberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 11)
	private Reason reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 6)
	private Status status;

	// When the slot opened: the start of the time to cover (SCRUM-73 AC3)
	@Column(nullable = false)
	private Instant openedAt;

	@Enumerated(EnumType.STRING)
	@Column(length = 13)
	private Escalation escalation;

	private Instant escalatedAt;

	protected CoverRequest() {
	}

	@SuppressWarnings("java:S107") // The slot, its shift, who held it, why and when it opened
	CoverRequest(UUID id, UUID slotId, UUID piqueteId, SlotRole role, Instant startsAt, Instant endsAt,
			UUID vacatedMemberId, Reason reason, Instant openedAt) {
		this.id = id;
		this.slotId = slotId;
		this.piqueteId = piqueteId;
		this.role = role;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.vacatedMemberId = vacatedMemberId;
		this.reason = reason;
		this.status = Status.OPEN;
		this.openedAt = openedAt;
	}

	UUID getId() {
		return id;
	}

	UUID getSlotId() {
		return slotId;
	}

	UUID getPiqueteId() {
		return piqueteId;
	}

	SlotRole getRole() {
		return role;
	}

	Instant getStartsAt() {
		return startsAt;
	}

	Instant getEndsAt() {
		return endsAt;
	}

	UUID getVacatedMemberId() {
		return vacatedMemberId;
	}

	Reason getReason() {
		return reason;
	}

	Status getStatus() {
		return status;
	}

	Instant getOpenedAt() {
		return openedAt;
	}

	Escalation getEscalation() {
		return escalation;
	}

	Instant getEscalatedAt() {
		return escalatedAt;
	}

	boolean isOpen() {
		return status == Status.OPEN;
	}

	/** Alerts the supervisor once; false when the request is no longer open or was already escalated. */
	boolean escalate(Escalation why, Instant at) {
		if (!isOpen() || escalation != null) {
			return false;
		}
		this.escalation = why;
		this.escalatedAt = at;
		return true;
	}

	/** The shift has started with nobody covering: the request is over. */
	void close() {
		this.status = Status.CLOSED;
	}
}
