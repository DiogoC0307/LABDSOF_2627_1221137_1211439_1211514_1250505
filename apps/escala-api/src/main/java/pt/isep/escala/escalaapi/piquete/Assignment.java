package pt.isep.escala.escalaapi.piquete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * A member is assigned to a slot in a shift
 */
@Entity
@Table(name = "assignment")
class Assignment {

	enum Status {
		PROPOSED,
		CONFIRMED,
		CANCELLED
	}

	enum EvaluationState {
		PENDING,
		EVALUATED,
		FAILED // In case the worker cannot evaluate after a timeout, or the answer is invalid
	}

	static final String ELIGIBLE = "ELIGIBLE";
	static final String INELIGIBLE = "INELIGIBLE";

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID publicationId;

	@Column(nullable = false)
	private UUID piqueteId;

	@Column(nullable = false)
	private UUID slotId;

	@Column(nullable = false)
	private UUID memberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 11)
	private SlotRole role;

	@Column(nullable = false)
	private Instant startsAt;

	@Column(nullable = false)
	private Instant endsAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 9)
	private Status status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 9)
	private EvaluationState evaluationState;

	@Column(length = 10)
	private String result;

	@Column(length = 255)
	private String violatedRules; // Codes of the violated rules

	@Column(length = 64)
	private String engineVersion;

	@Column(nullable = false)
	private Instant proposedAt;

	// When the answer (or the failure) came back
	@Column
	private Instant evaluatedAt;

	// When the week was published with this assignment
	@Column
	private Instant confirmedAt;

	protected Assignment() {
	}

	@SuppressWarnings("java:S107")
	Assignment(UUID id, UUID publicationId, UUID piqueteId, UUID slotId, UUID memberId, SlotRole role,
			Instant startsAt, Instant endsAt, Instant proposedAt) {
		this.id = id;
		this.publicationId = publicationId;
		this.piqueteId = piqueteId;
		this.slotId = slotId;
		this.memberId = memberId;
		this.role = role;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.status = Status.PROPOSED;
		this.evaluationState = EvaluationState.PENDING;
		this.proposedAt = proposedAt;
	}

	UUID getId() {
		return id;
	}

	UUID getPublicationId() {
		return publicationId;
	}

	UUID getPiqueteId() {
		return piqueteId;
	}

	UUID getSlotId() {
		return slotId;
	}

	UUID getMemberId() {
		return memberId;
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

	Status getStatus() {
		return status;
	}

	EvaluationState getEvaluationState() {
		return evaluationState;
	}

	String getResult() {
		return result;
	}

	String getEngineVersion() {
		return engineVersion;
	}

	List<String> getViolatedRules() {
		return violatedRules == null || violatedRules.isBlank() ? List.of() : Arrays.asList(violatedRules.split(","));
	}

	boolean isPending() {
		return evaluationState == EvaluationState.PENDING;
	}

	// Evaluation is refused if it failed or was evaluated as ineligible
	boolean isRefused() {
		return evaluationState == EvaluationState.FAILED || INELIGIBLE.equals(result);
	}

	// The worker's answer
	boolean evaluated(String answer, List<String> violated, String engine, Instant at) {
		if (!isPending() || !(ELIGIBLE.equals(answer) || INELIGIBLE.equals(answer))) {
			return false;
		}
		
		this.evaluationState = EvaluationState.EVALUATED;
		this.result = answer;
		this.violatedRules = String.join(",", violated);
		this.engineVersion = engine;
		this.evaluatedAt = at;

		return true;
	}

	boolean failed(Instant at) {
		if (!isPending()) {
			return false;
		}
		this.evaluationState = EvaluationState.FAILED;
		this.evaluatedAt = at;
		return true;
	}

	void confirm(Instant at) {
		this.status = Status.CONFIRMED;
		this.confirmedAt = at;
	}

	void cancel() {
		this.status = Status.CANCELLED;
	}
}
