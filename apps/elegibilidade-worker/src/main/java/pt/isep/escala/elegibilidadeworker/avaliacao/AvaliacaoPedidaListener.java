package pt.isep.escala.elegibilidadeworker.avaliacao;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.elegibilidadeworker.messaging.EventFields;
import pt.isep.escala.elegibilidadeworker.metrics.EvaluationMetrics;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEvent;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEventRepository;
import pt.isep.escala.elegibilidadeworker.outbox.ProcessedEvent;
import pt.isep.escala.elegibilidadeworker.outbox.ProcessedEventRepository;

@Component
class AvaliacaoPedidaListener {

	static final String QUEUE = "worker.avaliacao-pedida";
	static final String ROUTING_AVALIACAO_CONCLUIDA = "avaliacao.concluida";

	private static final Logger log = LoggerFactory.getLogger(AvaliacaoPedidaListener.class);

	private final ObjectMapper objectMapper;
	private final ProcessedEventRepository processed;
	private final EvaluationRepository evaluations;
	private final OutboxEventRepository outbox;
	private final EvaluationMetrics metrics;

	AvaliacaoPedidaListener(
			ObjectMapper objectMapper,
			ProcessedEventRepository processed,
			EvaluationRepository evaluations,
			OutboxEventRepository outbox,
			EvaluationMetrics metrics) {
		this.metrics = metrics;
		this.objectMapper = objectMapper;
		this.processed = processed;
		this.evaluations = evaluations;
		this.outbox = outbox;
	}

	@RabbitListener(queues = QUEUE)
	@Transactional
	void onPedido(Message message) {
		long started = System.nanoTime();
		JsonNode body = EventFields.body(objectMapper, message);
		String traceId = body.path("traceId").asText();
		MDC.put("trace_id", traceId);
		try {
			UUID requestEventId = EventFields.uuid(body, "eventId");
			if (processed.existsById(requestEventId)) {
				log.info("AvaliacaoPedida duplicate ignored eventId={} traceId={}", requestEventId, traceId);
				return;
			}
			UUID assignmentId = EventFields.uuid(body, "assignmentId");
			List<String> violated = EligibilityRules.violated(body.path("slot"), body.path("candidate"));
			String result = EligibilityRules.result(violated);
			evaluations.save(new Evaluation(
					UUID.randomUUID(),
					requestEventId,
					assignmentId,
					result,
					violated,
					EligibilityRules.ENGINE_VERSION,
					traceId,
					Instant.now()));
			UUID resultEventId = UUID.randomUUID();
			outbox.save(new OutboxEvent(
					resultEventId,
					ROUTING_AVALIACAO_CONCLUIDA,
					AvaliacaoConcluidaJson.of(objectMapper, resultEventId, requestEventId, assignmentId, traceId, result,
							violated),
					Instant.now()));
			processed.save(new ProcessedEvent(requestEventId, Instant.now()));
			metrics.completed(result, EvaluationMetrics.LISTENER);
			metrics.processed(Duration.ofNanos(System.nanoTime() - started));
			// Rule codes only: no member, slot times nor periods in the log (T-07)
			log.info(
					"evaluation written requestEventId={} assignmentId={} result={} violatedRules={} resultEventId={} traceId={}",
					requestEventId,
					assignmentId,
					result,
					violated,
					resultEventId,
					traceId);
		} finally {
			MDC.remove("trace_id");
		}
	}
}
