package pt.isep.escala.elegibilidadeworker.avaliacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "evaluation")
class Evaluation {

	// The walking skeleton's engine, which answered ELIGIBLE to everything; kept for the evaluations it made.
	static final String ENGINE_SEMPRE_ELEGIVEL = "SEMPRE_ELEGIVEL";

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private UUID requestEventId;

	@Column(nullable = false)
	private UUID assignmentId;

	@Column(nullable = false)
	private String result;

	@Column(length = 255)
	private String violatedRules;

	@Column(nullable = false)
	private String engineVersion;

	@Column(nullable = false)
	private String traceId;

	@Column(nullable = false)
	private Instant createdAt;

	protected Evaluation() {
	}

	Evaluation(
			UUID id,
			UUID requestEventId,
			UUID assignmentId,
			String result,
			List<String> violatedRules,
			String engineVersion,
			String traceId,
			Instant createdAt) {
		this.id = id;
		this.requestEventId = requestEventId;
		this.assignmentId = assignmentId;
		this.result = result;
		this.violatedRules = String.join(",", violatedRules);
		this.engineVersion = engineVersion;
		this.traceId = traceId;
		this.createdAt = createdAt;
	}
}
