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
 * One member checked for a cover request (SCRUM-72). Its id is the assignmentId of AvaliacaoPedida (purpose COVER).
 * The member is asked (OFFERED) only when the eligibility rules accept them (ADR 0007); a refusal, or a check that could
 * not be made, leaves them NOT_OFFERED.
 */
@Entity
@Table(name = "cover_offer")
class CoverOffer {

	enum Status {
		CHECKING,
		OFFERED,
		NOT_OFFERED
	}

	enum EvaluationState {
		PENDING,
		EVALUATED,
		FAILED
	}

	static final String ELIGIBLE = "ELIGIBLE";
	static final String INELIGIBLE = "INELIGIBLE";

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID coverRequestId;

	@Column(nullable = false)
	private UUID memberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 11)
	private Status status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 9)
	private EvaluationState evaluationState;

	@Column(length = 10)
	private String result;

	@Column(length = 255)
	private String violatedRules;

	@Column(length = 64)
	private String engineVersion;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant evaluatedAt;

	private Instant offeredAt;

	protected CoverOffer() {
	}

	CoverOffer(UUID id, UUID coverRequestId, UUID memberId, Instant createdAt) {
		this.id = id;
		this.coverRequestId = coverRequestId;
		this.memberId = memberId;
		this.status = Status.CHECKING;
		this.evaluationState = EvaluationState.PENDING;
		this.createdAt = createdAt;
	}

	UUID getId() {
		return id;
	}

	UUID getCoverRequestId() {
		return coverRequestId;
	}

	UUID getMemberId() {
		return memberId;
	}

	Status getStatus() {
		return status;
	}

	// When the answer (or the failure) came back
	Instant getEvaluatedAt() {
		return evaluatedAt;
	}

	// When the member was asked: the start of their time to answer (SCRUM-73)
	Instant getOfferedAt() {
		return offeredAt;
	}

	boolean isChecking() {
		return status == Status.CHECKING;
	}

	/**
	 * The worker's answer. The member is asked only when the rules accept them and the request is still open; false
	 * when the offer was already answered or the answer is not one.
	 */
	boolean evaluated(String answer, String violated, String engine, Instant at, boolean requestOpen) {
		if (evaluationState != EvaluationState.PENDING || !(ELIGIBLE.equals(answer) || INELIGIBLE.equals(answer))) {
			return false;
		}
		this.evaluationState = EvaluationState.EVALUATED;
		this.result = answer;
		this.violatedRules = violated;
		this.engineVersion = engine;
		this.evaluatedAt = at;
		if (ELIGIBLE.equals(answer) && requestOpen) {
			this.status = Status.OFFERED;
			this.offeredAt = at;
		} else {
			this.status = Status.NOT_OFFERED;
		}
		return true;
	}

	/** The check could not be made (RF24): the member is not asked. */
	boolean failed(Instant at) {
		if (evaluationState != EvaluationState.PENDING) {
			return false;
		}
		this.evaluationState = EvaluationState.FAILED;
		this.evaluatedAt = at;
		this.status = Status.NOT_OFFERED;
		return true;
	}

	boolean isOffered() {
		return status == Status.OFFERED;
	}
}
