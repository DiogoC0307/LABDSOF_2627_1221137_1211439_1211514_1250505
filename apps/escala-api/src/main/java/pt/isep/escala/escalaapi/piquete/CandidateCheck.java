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
 * One member checked by the eligibility rules for a role on a shift the supervisor is building (ADR 0007).
 * 
 * PENDING until AvaliacaoConcluida answers, then ELIGIBLE or INELIGIBLE with the rules broken. FAILED when the
 * worker could not evaluate it.
 */
@Entity
@Table(name = "candidate_check")
class CandidateCheck {

	enum State {
		PENDING,
		EVALUATED,
		FAILED
	}

	static final String ELIGIBLE = "ELIGIBLE";
	static final String INELIGIBLE = "INELIGIBLE";

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID requestId;

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
	private State evaluationState;

	@Column(length = 10)
	private String result;

	// Codes of the broken rules
	@Column(length = 255)
	private String violatedRules;

	@Column(length = 64)
	private String engineVersion;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant evaluatedAt;

	protected CandidateCheck() {
	}

	@SuppressWarnings("java:S107") // The member, the role and the shift's times: all needed to check them
	CandidateCheck(UUID id, UUID requestId, UUID memberId, SlotRole role, Instant startsAt, Instant endsAt,
			Instant createdAt) {
		this.id = id;
		this.requestId = requestId;
		this.memberId = memberId;
		this.role = role;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.evaluationState = State.PENDING;
		this.createdAt = createdAt;
	}

	UUID getId() {
		return id;
	}

	UUID getRequestId() {
		return requestId;
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

	State getEvaluationState() {
		return evaluationState;
	}

	String getResult() {
		return result;
	}

	List<String> getViolatedRules() {
		return violatedRules == null || violatedRules.isBlank() ? List.of() : Arrays.asList(violatedRules.split(","));
	}

	boolean isPending() {
		return evaluationState == State.PENDING;
	}

	boolean evaluated(String answer, List<String> violated, String engine, Instant at) {
		if (!isPending() || !(ELIGIBLE.equals(answer) || INELIGIBLE.equals(answer))) {
			return false;
		}
		this.evaluationState = State.EVALUATED;
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
		this.evaluationState = State.FAILED;
		this.evaluatedAt = at;
		return true;
	}
}
