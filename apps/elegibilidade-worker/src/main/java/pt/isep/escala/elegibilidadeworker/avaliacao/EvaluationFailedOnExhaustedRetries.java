package pt.isep.escala.elegibilidadeworker.avaliacao;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.elegibilidadeworker.messaging.EventFields;
import pt.isep.escala.elegibilidadeworker.messaging.RetriesExhaustedHandler;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEvent;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * When the worker cannot evaluate a request after every retry, it answers AvaliacaoConcluida with result FAILED, so
 * escala-api shows EVALUATION_FAILED instead of PENDING for ever (RF24). The request goes to the DLQ and is not
 * marked as processed, so replaying it evaluates it again. If the worker's database is down, this answer cannot be
 * written either: the slot stays PENDING until the DLQ is replayed.
 */
@Component
class EvaluationFailedOnExhaustedRetries implements RetriesExhaustedHandler {

	private static final Logger log = LoggerFactory.getLogger(EvaluationFailedOnExhaustedRetries.class);

	private final ObjectMapper objectMapper;
	private final OutboxEventRepository outbox;

	EvaluationFailedOnExhaustedRetries(ObjectMapper objectMapper, OutboxEventRepository outbox) {
		this.objectMapper = objectMapper;
		this.outbox = outbox;
	}

	@Override
	public String queue() {
		return AvaliacaoPedidaListener.QUEUE;
	}

	@Override
	@Transactional
	public void handle(Message message) {
		JsonNode body = EventFields.body(objectMapper, message);
		UUID requestEventId = EventFields.uuid(body, "eventId");
		UUID assignmentId = EventFields.uuid(body, "assignmentId");
		String traceId = body.path("traceId").asText();
		UUID resultEventId = UUID.randomUUID();
		outbox.save(new OutboxEvent(
				resultEventId,
				AvaliacaoPedidaListener.ROUTING_AVALIACAO_CONCLUIDA,
				AvaliacaoConcluidaJson.of(objectMapper, resultEventId, requestEventId, assignmentId, traceId, "FAILED"),
				Instant.now()));
		log.warn("evaluation failed after the retries assignmentId={} resultEventId={}", assignmentId, resultEventId);
	}
}
