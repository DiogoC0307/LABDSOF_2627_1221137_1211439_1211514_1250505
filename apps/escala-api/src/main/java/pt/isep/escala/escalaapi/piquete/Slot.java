package pt.isep.escala.escalaapi.piquete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "slot")
class Slot {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SlotRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SlotStatus status;

	@Column(nullable = false, unique = true)
	private UUID assignmentId;

	@Column(nullable = false)
	private Instant shiftStartsAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PresenceStatus presenceStatus = PresenceStatus.UNCONFIRMED;

	@Column
	private Instant confirmationPromptSentAt;

	@ManyToOne(optional = false)
	@JoinColumn(name = "piquete_id")
	private Piquete piquete;

	protected Slot() {
	}

	Slot(UUID id, SlotRole role, SlotStatus status, UUID assignmentId) {
		this(id, role, status, assignmentId, Instant.now());
	}

	Slot(UUID id, SlotRole role, SlotStatus status, UUID assignmentId, Instant shiftStartsAt) {
		this.id = id;
		this.role = role;
		this.status = status;
		this.assignmentId = assignmentId;
		this.shiftStartsAt = shiftStartsAt;
		this.presenceStatus = PresenceStatus.UNCONFIRMED;
	}

	UUID getId() {
		return id;
	}

	SlotRole getRole() {
		return role;
	}

	SlotStatus getStatus() {
		return status;
	}

	UUID getAssignmentId() {
		return assignmentId;
	}

	Instant getShiftStartsAt() {
		return shiftStartsAt;
	}

	void setShiftStartsAt(Instant shiftStartsAt) {
		this.shiftStartsAt = shiftStartsAt;
	}

	PresenceStatus getPresenceStatus() {
		return presenceStatus;
	}

	Instant getConfirmationPromptSentAt() {
		return confirmationPromptSentAt;
	}

	boolean shouldPromptConfirmation(Instant now, long advanceHours) {
		if (confirmationPromptSentAt != null || presenceStatus != PresenceStatus.UNCONFIRMED) {
			return false;
		}
		Instant triggerAt = shiftStartsAt.minusSeconds(advanceHours * 3600L);
		return !now.isBefore(triggerAt) && now.isBefore(shiftStartsAt);
	}

	void markConfirmationPromptSent(Instant sentAt) {
		this.confirmationPromptSentAt = sentAt;
	}

	String confirmationPromptText(ZoneId zoneId) {
		var localTime = shiftStartsAt.atZone(zoneId).toLocalTime();
		return "Confirm your shift tomorrow at %02d:%02d: I'm coming / I can't come"
				.formatted(localTime.getHour(), localTime.getMinute());
	}

	/** Records member presence; returns true if the slot was freed because the member cannot come (AC3 / SCRUM-71). */
	boolean recordPresence(PresenceRequest.PresenceDecision decision) {
		if (decision == PresenceRequest.PresenceDecision.COMING) {
			this.presenceStatus = PresenceStatus.COMING;
			return false;
		}
		this.presenceStatus = PresenceStatus.CANNOT_COME;
		this.status = SlotStatus.OPEN;
		return true;
	}

	/** An ELIGIBLE evaluation applies to a slot waiting for it, or to one whose evaluation failed and was replayed. */
	boolean markEligible() {
		if (status != SlotStatus.PENDING && status != SlotStatus.EVALUATION_FAILED) {
			return false;
		}
		this.status = SlotStatus.ELIGIBLE;
		return true;
	}

	/** A failed evaluation never applies a change (RF24): it only marks a slot that is still waiting for one. */
	boolean markEvaluationFailed() {
		if (status != SlotStatus.PENDING) {
			return false;
		}
		this.status = SlotStatus.EVALUATION_FAILED;
		return true;
	}

	void setPiquete(Piquete piquete) {
		this.piquete = piquete;
	}
}