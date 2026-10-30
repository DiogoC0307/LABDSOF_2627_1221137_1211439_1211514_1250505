package pt.isep.escala.elegibilidadeworker.avaliacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evaluation")
class Evaluation {

	static final String ENGINE_SEMPRE_ELEGIVEL = "SEMPRE_ELEGIVEL";

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private UUID requestEventId;

	@Column(nullable = false)
	private UUID assignmentId;

	@Column(nullable = false)
	private String result;

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
			String engineVersion,
			String traceId,
			Instant createdAt) {
		this.id = id;
		this.requestEventId = requestEventId;
		this.assignmentId = assignmentId;
		this.result = result;
		this.engineVersion = engineVersion;
		this.traceId = traceId;
		this.createdAt = createdAt;
	}
}
