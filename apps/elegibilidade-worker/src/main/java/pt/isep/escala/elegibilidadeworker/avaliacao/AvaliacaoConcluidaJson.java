package pt.isep.escala.elegibilidadeworker.avaliacao;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** The AvaliacaoConcluida event (contracts/events/avaliacao-concluida.schema.json). */
final class AvaliacaoConcluidaJson {

	private AvaliacaoConcluidaJson() {
	}

	static String of(ObjectMapper objectMapper, UUID eventId, UUID requestEventId, UUID assignmentId, String traceId,
			String result) {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("eventId", eventId.toString());
		root.put("type", "AvaliacaoConcluida");
		root.put("schemaVersion", 1);
		root.put("occurredAt", Instant.now().toString());
		root.put("traceId", traceId);
		root.put("requestEventId", requestEventId.toString());
		root.put("assignmentId", assignmentId.toString());
		root.put("result", result);
		root.set("violatedRules", objectMapper.createArrayNode());
		root.put("engineVersion", Evaluation.ENGINE_SEMPRE_ELEGIVEL);
		return objectMapper.writeValueAsString(root);
	}
}
