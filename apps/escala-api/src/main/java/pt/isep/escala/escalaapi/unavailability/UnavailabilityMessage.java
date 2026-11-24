package pt.isep.escala.escalaapi.unavailability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A member's unavailability in their own words (SCRUM-75). The free text lives only until the member confirms or
 * discards the proposal, or for 24 hours (RNF09); then it is deleted and the row stays with dates and the method
 * only.
 */
@Entity
@Table(name = "unavailability_message")
class UnavailabilityMessage {

	static final String PENDING = "PENDING";
	static final String PROPOSED = "PROPOSED";
	static final String CONFIRMED = "CONFIRMED";
	static final String DISCARDED = "DISCARDED";
	static final String EXPIRED = "EXPIRED";

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID memberId;

	@Column(length = 500)
	private String freeText;

	@Column(nullable = false)
	private LocalDate referenceDate;

	@Column(nullable = false)
	private String status;

	@Column(nullable = false)
	private UUID requestEventId;

	@Column(columnDefinition = "TEXT")
	private String proposal;

	private String method;

	private String model;

	private Integer latencyMs;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant answeredAt;

	private Instant closedAt;

	protected UnavailabilityMessage() {
	}

	UnavailabilityMessage(UUID memberId, String freeText, LocalDate referenceDate, UUID requestEventId, Instant now) {
		this.id = UUID.randomUUID();
		this.memberId = memberId;
		this.freeText = freeText;
		this.referenceDate = referenceDate;
		this.status = PENDING;
		this.requestEventId = requestEventId;
		this.createdAt = now;
	}

	boolean isPending() {
		return PENDING.equals(status);
	}

	boolean isProposed() {
		return PROPOSED.equals(status);
	}

	boolean isOpen() {
		return isPending() || isProposed();
	}

	/** The worker's answer: the periods as JSON in the ExtracaoConcluida v2 format (maybe none). */
	void propose(String periodsJson, String method, String model, Integer latencyMs, Instant now) {
		this.proposal = periodsJson;
		this.method = method;
		this.model = model;
		this.latencyMs = latencyMs;
		this.answeredAt = now;
		this.status = PROPOSED;
	}

	void confirm(Instant now) {
		close(CONFIRMED, now);
	}

	void discard(Instant now) {
		close(DISCARDED, now);
	}

	void expire(Instant now) {
		close(EXPIRED, now);
	}

	private void close(String to, Instant now) {
		this.status = to;
		this.freeText = null;
		this.closedAt = now;
	}

	UUID getId() {
		return id;
	}

	UUID getMemberId() {
		return memberId;
	}

	String getFreeText() {
		return freeText;
	}

	LocalDate getReferenceDate() {
		return referenceDate;
	}

	String getStatus() {
		return status;
	}

	UUID getRequestEventId() {
		return requestEventId;
	}

	String getProposal() {
		return proposal;
	}

	String getMethod() {
		return method;
	}

	String getModel() {
		return model;
	}

	Integer getLatencyMs() {
		return latencyMs;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getAnsweredAt() {
		return answeredAt;
	}

	Instant getClosedAt() {
		return closedAt;
	}
}
