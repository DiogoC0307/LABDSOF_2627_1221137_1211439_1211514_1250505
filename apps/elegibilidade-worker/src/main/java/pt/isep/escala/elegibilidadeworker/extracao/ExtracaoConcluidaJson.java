package pt.isep.escala.elegibilidadeworker.extracao;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The ExtracaoConcluida event, version 2 ({@code contracts/events/extracao-concluida.schema.json}, SCRUM-75): the
 * request it answers, the proposed periods, the method, the model and the latency. It never carries the free text.
 */
final class ExtracaoConcluidaJson {

	static final String TYPE = "ExtracaoConcluida";
	static final int SCHEMA_VERSION = 2;

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	private ExtracaoConcluidaJson() {
	}

	static String of(ObjectMapper mapper, UUID eventId, UUID requestEventId, UUID unavailabilityRequestId,
			String traceId, UnavailabilityProposer.Proposal proposal) {
		ObjectNode root = mapper.createObjectNode();
		root.put("eventId", eventId.toString());
		root.put("type", TYPE);
		root.put("schemaVersion", SCHEMA_VERSION);
		root.put("occurredAt", Instant.now().toString());
		root.put("traceId", traceId);
		root.put("requestEventId", requestEventId.toString());
		root.put("unavailabilityRequestId", unavailabilityRequestId.toString());
		ArrayNode periods = root.putArray("periods");
		for (UnavailabilityPeriod p : proposal.periods()) {
			ObjectNode period = periods.addObject();
			period.put("startDate", p.startDate().toString());
			period.put("endDate", p.endDate().toString());
			if (!p.weekdays().isEmpty()) {
				ArrayNode days = period.putArray("weekdays");
				p.weekdays().forEach(day -> days.add(day.name().substring(0, 3)));
			}
			if (p.startTime() != null) {
				period.put("startTime", p.startTime().format(TIME));
				period.put("endTime", p.endTime().format(TIME));
			}
		}
		root.put("method", proposal.method().name());
		if (proposal.model() == null) {
			root.putNull("model");
		}
		else {
			root.put("model", proposal.model());
		}
		if (proposal.latencyMs() == null) {
			root.putNull("latencyMs");
		}
		else {
			root.put("latencyMs", proposal.latencyMs());
		}
		return mapper.writeValueAsString(root);
	}
}
