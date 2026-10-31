package pt.isep.escala.elegibilidadeworker.avaliacao;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEvent;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEventRepository;
import pt.isep.escala.elegibilidadeworker.outbox.ProcessedEvent;
import pt.isep.escala.elegibilidadeworker.outbox.ProcessedEventRepository;

@Component
class AvaliacaoPedidaListener {

	static final String ROUTING_AVALIACAO_CONCLUIDA = "avaliacao.concluida";

	private static final Logger log = LoggerFactory.getLogger(AvaliacaoPedidaListener.class);

	private final ObjectMapper objectMapper;
	private final ProcessedEventRepository processed;
	private final EvaluationRepository evaluations;
	private final OutboxEventRepository outbox;

	AvaliacaoPedidaListener(
			ObjectMapper objectMapper,
			ProcessedEventRepository processed,
			EvaluationRepository evaluations,
			OutboxEventRepository outbox) {
		this.objectMapper = objectMapper;
		this.processed = processed;
		this.evaluations = evaluations;
		this.outbox = outbox;
	}

	@RabbitListener(queues = "worker.avaliacao-pedida")
	@Transactional
	void onPedido(Message message) throws Exception {
		JsonNode body = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
		String traceId = body.path("traceId").asText();
		MDC.put("trace_id", traceId);
		try {
			UUID requestEventId = UUID.fromString(body.path("eventId").asText());
			if (processed.existsById(requestEventId)) {
				log.info("AvaliacaoPedida duplicate ignored eventId={} traceId={}", requestEventId, traceId);
				return;
			}
			UUID assignmentId = UUID.fromString(body.path("assignmentId").asText());
			evaluations.save(new Evaluation(
					UUID.randomUUID(),
					requestEventId,
					assignmentId,
					"ELIGIBLE",
					Evaluation.ENGINE_SEMPRE_ELEGIVEL,
					traceId,
					Instant.now()));
			UUID resultEventId = UUID.randomUUID();
			outbox.save(new OutboxEvent(
					resultEventId, ROUTING_AVALIACAO_CONCLUIDA, concluidaJson(resultEventId, requestEventId, assignmentId, traceId), Instant.now()));
			processed.save(new ProcessedEvent(requestEventId, Instant.now()));
			log.info(
					"SEMPRE_ELEGIVEL written requestEventId={} assignmentId={} resultEventId={} traceId={}",
					requestEventId,
					assignmentId,
					resultEventId,
					traceId);
		} finally {
			MDC.remove("trace_id");
		}
	}

	private String concluidaJson(UUID eventId, UUID requestEventId, UUID assignmentId, String traceId)
			throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("eventId", eventId.toString());
		root.put("type", "AvaliacaoConcluida");
		root.put("schemaVersion", 1);
		root.put("occurredAt", Instant.now().toString());
		root.put("traceId", traceId);
		root.put("requestEventId", requestEventId.toString());
		root.put("assignmentId", assignmentId.toString());
		root.put("result", "ELIGIBLE");
		ArrayNode violated = objectMapper.createArrayNode();
		root.set("violatedRules", violated);
		root.put("engineVersion", Evaluation.ENGINE_SEMPRE_ELEGIVEL);
		return objectMapper.writeValueAsString(root);
	}
}
