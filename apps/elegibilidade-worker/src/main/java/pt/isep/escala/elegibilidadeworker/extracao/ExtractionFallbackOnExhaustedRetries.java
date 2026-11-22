package pt.isep.escala.elegibilidadeworker.extracao;

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
 * When an ExtracaoPedida still fails after every retry, the worker answers ExtracaoConcluida with {@code FALLBACK} and
 * no periods, so escala-api shows the form instead of waiting (ADR 0011, RF35). The request goes to the DLQ and is not
 * marked as processed, as for AvaliacaoPedida (SCRUM-95). The free text is not read here.
 */
@Component
class ExtractionFallbackOnExhaustedRetries implements RetriesExhaustedHandler {

	private static final Logger log = LoggerFactory.getLogger(ExtractionFallbackOnExhaustedRetries.class);

	private final ObjectMapper objectMapper;
	private final OutboxEventRepository outbox;

	ExtractionFallbackOnExhaustedRetries(ObjectMapper objectMapper, OutboxEventRepository outbox) {
		this.objectMapper = objectMapper;
		this.outbox = outbox;
	}

	@Override
	public String queue() {
		return ExtracaoPedidaListener.QUEUE;
	}

	@Override
	@Transactional
	public void handle(Message message) {
		JsonNode body = EventFields.body(objectMapper, message);
		UUID requestEventId = EventFields.uuid(body, "eventId");
		UUID unavailabilityRequestId = EventFields.uuid(body, "unavailabilityRequestId");
		String traceId = body.path("traceId").asString("");
		UUID resultEventId = UUID.randomUUID();
		outbox.save(new OutboxEvent(resultEventId, ExtracaoPedidaListener.ROUTING_EXTRACAO_CONCLUIDA,
				ExtracaoConcluidaJson.of(objectMapper, resultEventId, requestEventId, unavailabilityRequestId, traceId,
						UnavailabilityProposer.Proposal.fallback(null, null, "RETRIES_EXHAUSTED")),
				Instant.now()));
		log.warn("extraction failed after the retries unavailabilityRequestId={} resultEventId={}",
				unavailabilityRequestId, resultEventId);
	}
}
