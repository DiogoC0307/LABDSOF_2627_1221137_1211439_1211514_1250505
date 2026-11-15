package pt.isep.escala.escalaapi.piquete;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.messaging.EventFields;
import pt.isep.escala.escalaapi.outbox.ProcessedEvent;
import pt.isep.escala.escalaapi.outbox.ProcessedEventRepository;

@Component
class AvaliacaoConcluidaListener {

	static final String QUEUE = "api.avaliacao-concluida";

	private static final Logger log = LoggerFactory.getLogger(AvaliacaoConcluidaListener.class);

	private final ObjectMapper objectMapper;
	private final ProcessedEventRepository processed;
	private final SlotRepository slots;

	AvaliacaoConcluidaListener(
			ObjectMapper objectMapper, ProcessedEventRepository processed, SlotRepository slots) {
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
			slots.findByAssignmentId(assignmentId).ifPresent(slot -> {
				boolean changed = switch (result) {
					case "ELIGIBLE" -> slot.markEligible();
					case "FAILED" -> slot.markEvaluationFailed();
					default -> false;
				};
				if (changed) {
					slots.save(slot);
				}
			});
			processed.save(new ProcessedEvent(eventId, Instant.now()));
			log.info("AvaliacaoConcluida applied assignmentId={} result={} traceId={}", assignmentId, result, traceId);
		} finally {
			MDC.remove("trace_id");
		}
	}
}
