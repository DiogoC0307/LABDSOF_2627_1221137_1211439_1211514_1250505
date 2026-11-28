package pt.isep.escala.escalaapi.piquete;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.messaging.EventFields;
import pt.isep.escala.escalaapi.metrics.EvaluationMetrics;
import pt.isep.escala.escalaapi.outbox.ProcessedEvent;
import pt.isep.escala.escalaapi.outbox.ProcessedEventRepository;

@Component
class AvaliacaoConcluidaListener {

	static final String QUEUE = "api.avaliacao-concluida";

	private static final Logger log = LoggerFactory.getLogger(AvaliacaoConcluidaListener.class);

	private final ObjectMapper objectMapper;
	private final ProcessedEventRepository processed;
	private final SlotRepository slots;
	private final EvaluationMetrics metrics;
	private final CandidateCheckService candidateChecks;

	AvaliacaoConcluidaListener(
			ObjectMapper objectMapper, ProcessedEventRepository processed, SlotRepository slots,
			EvaluationMetrics metrics, CandidateCheckService candidateChecks) {
		this.metrics = metrics;
		this.candidateChecks = candidateChecks;
		this.objectMapper = objectMapper;
		this.processed = processed;
		this.slots = slots;
	}

	@RabbitListener(queues = QUEUE)
	@Transactional
	void onConcluida(Message message) {
		JsonNode body = EventFields.body(objectMapper, message);
		String traceId = body.path("traceId").asText();
		MDC.put("trace_id", traceId);
		try {
			UUID eventId = EventFields.uuid(body, "eventId");
			if (processed.existsById(eventId)) {
				log.info("AvaliacaoConcluida duplicate ignored eventId={} traceId={}", eventId, traceId);
				return;
			}
			UUID assignmentId = EventFields.uuid(body, "assignmentId");
			String result = body.path("result").asText();
			// A walking-skeleton check of a new slot, or a member checked for a shift being built (SCRUM-171)
			boolean applied = slots.findByAssignmentId(assignmentId).map(slot -> {
				boolean changed = switch (result) {
					case "ELIGIBLE" -> slot.markEligible();
					case "FAILED" -> slot.markEvaluationFailed();
					default -> false;
				};
				if (changed) {
					slots.save(slot);
				}
				return changed;
			}).orElseGet(() -> candidateChecks.onEvaluated(assignmentId, result, violatedRules(body),
					body.path("engineVersion").asString("")));
			processed.save(new ProcessedEvent(eventId, Instant.now()));
			metrics.completed(requestEventId(body), result, applied, Instant.now());
			log.info("AvaliacaoConcluida applied assignmentId={} result={} traceId={}", assignmentId, result, traceId);
		} finally {
			MDC.remove("trace_id");
		}
	}

	// List of codes of the rules that were violated. Empty when ELIGIBLE.
	private static List<String> violatedRules(JsonNode body) {
		List<String> codes = new ArrayList<>();
		body.path("violatedRules").forEach(code -> {
			if (code.isString()) {
				codes.add(code.asString());
			}
		});
		return codes;
	}

	/** The AvaliacaoPedida this answers, for SLI 1; a missing or broken one only means no latency is recorded. */
	private static UUID requestEventId(JsonNode body) {
		try {
			return UUID.fromString(body.path("requestEventId").asString(""));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
