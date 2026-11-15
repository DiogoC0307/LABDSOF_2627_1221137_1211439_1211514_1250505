package pt.isep.escala.escalaapi.piquete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

	@ManyToOne(optional = false)
	@JoinColumn(name = "piquete_id")
	private Piquete piquete;

	protected Slot() {
	}

	Slot(UUID id, SlotRole role, SlotStatus status, UUID assignmentId) {
		this.id = id;
		this.role = role;
		this.status = status;
		this.assignmentId = assignmentId;
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
