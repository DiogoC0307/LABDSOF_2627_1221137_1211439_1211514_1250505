package pt.isep.escala.elegibilidadeworker.extracao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import pt.isep.escala.elegibilidadeworker.messaging.EventFields;
import pt.isep.escala.elegibilidadeworker.messaging.MalformedEventException;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEvent;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEventRepository;
import pt.isep.escala.elegibilidadeworker.outbox.ProcessedEvent;
import pt.isep.escala.elegibilidadeworker.outbox.ProcessedEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The worker side of SCRUM-75: consumes ExtracaoPedida, asks {@link UnavailabilityProposer} for a proposal and
 * publishes ExtracaoConcluida v2 through the worker's outbox (ADR 0009), once per request (the eventId is the
 * idempotency key, RNF05).
 *
 * <p><b>The free text is never stored and never logged by the worker.</b> It is read from the message, given to the
 * extractor and dropped; the outbox row, the processed-event row and the log lines carry IDs, the method, the number
 * of periods and the latency only. The model call (up to 10 s) runs outside the database transaction, so no
 * connection is held while the model answers.
 */
@Component
class ExtracaoPedidaListener {

	static final String QUEUE = "worker.extracao-pedida";
	static final String ROUTING_EXTRACAO_CONCLUIDA = "extracao.concluida";

	private static final Logger log = LoggerFactory.getLogger(ExtracaoPedidaListener.class);

	private final ObjectMapper objectMapper;
	private final UnavailabilityProposer proposer;
	private final ProcessedEventRepository processed;
	private final OutboxEventRepository outbox;
	private final TransactionTemplate transaction;

	ExtracaoPedidaListener(ObjectMapper objectMapper, UnavailabilityProposer proposer,
			ProcessedEventRepository processed, OutboxEventRepository outbox, TransactionTemplate transaction) {
		this.objectMapper = objectMapper;
		this.proposer = proposer;
		this.processed = processed;
		this.outbox = outbox;
		this.transaction = transaction;
	}

	@RabbitListener(queues = QUEUE)
	void onPedido(Message message) {
		JsonNode body = EventFields.body(objectMapper, message);
		String traceId = body.path("traceId").asText("");
		MDC.put("trace_id", traceId);
		try {
			UUID requestEventId = EventFields.uuid(body, "eventId");
			if (processed.existsById(requestEventId)) {
				log.info("ExtracaoPedida duplicate ignored eventId={} traceId={}", requestEventId, traceId);
				return;
			}
			UUID unavailabilityRequestId = EventFields.uuid(body, "unavailabilityRequestId");
			LocalDate referenceDate = referenceDate(body);
			JsonNode freeText = body.get("freeText");
			if (freeText == null || !freeText.isString()) {
				throw new MalformedEventException("freeText is not a string");
			}
			UnavailabilityProposer.Proposal proposal = proposer.propose(freeText.asString(), referenceDate);
			UUID resultEventId = UUID.randomUUID();
			String result = ExtracaoConcluidaJson.of(objectMapper, resultEventId, requestEventId,
					unavailabilityRequestId, traceId, proposal);
			boolean written = Boolean.TRUE.equals(transaction.execute(status -> {
				if (processed.existsById(requestEventId)) {
					return false;
				}
				outbox.save(new OutboxEvent(resultEventId, ROUTING_EXTRACAO_CONCLUIDA, result, Instant.now()));
				processed.save(new ProcessedEvent(requestEventId, Instant.now()));
				return true;
			}));
			if (written) {
				log.info("ExtracaoConcluida written requestEventId={} unavailabilityRequestId={} resultEventId={} "
						+ "method={} periods={} outcome={} reason={} latencyMs={} traceId={}", requestEventId,
						unavailabilityRequestId, resultEventId, proposal.method(), proposal.periods().size(),
						proposal.outcome(), proposal.reason(), proposal.latencyMs(), traceId);
			}
			else {
				log.info("ExtracaoPedida duplicate ignored eventId={} traceId={}", requestEventId, traceId);
			}
		}
		finally {
			MDC.remove("trace_id");
		}
	}

	private static LocalDate referenceDate(JsonNode body) {
		try {
			return LocalDate.parse(body.path("referenceDate").asText(""));
		}
		catch (DateTimeParseException e) {
			throw new MalformedEventException("referenceDate is not a date");
		}
	}
}
