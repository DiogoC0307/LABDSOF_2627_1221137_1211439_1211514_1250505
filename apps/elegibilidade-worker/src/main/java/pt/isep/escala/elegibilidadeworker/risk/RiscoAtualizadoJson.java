package pt.isep.escala.elegibilidadeworker.risk;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** The RiscoAtualizado event (contracts/events/risco-atualizado.schema.json). */
final class RiscoAtualizadoJson {

	private RiscoAtualizadoJson() {
	}

	static String of(ObjectMapper objectMapper, UUID eventId, RiskSnapshot snapshot, Instant occurredAt, String traceId) {
		double ageHours = Math.max(0, Duration.between(snapshot.validAt(), occurredAt).toMillis() / 3_600_000d);
		ObjectNode root = objectMapper.createObjectNode();
		root.put("eventId", eventId.toString());
		root.put("type", "RiscoAtualizado");
		root.put("schemaVersion", 1);
		root.put("occurredAt", occurredAt.toString());
		root.put("traceId", traceId);
		root.put("dico", snapshot.dico());
		root.put("forecastDate", snapshot.forecastDate().toString());
		root.put("rcm", snapshot.rcm());
		root.put("fetchedAt", snapshot.validAt().toString());
		root.put("source", snapshot.source());
		root.put("assumedHigher", snapshot.assumedHigher());
		root.put("dataAgeHours", ageHours);
		return objectMapper.writeValueAsString(root);
	}
}
