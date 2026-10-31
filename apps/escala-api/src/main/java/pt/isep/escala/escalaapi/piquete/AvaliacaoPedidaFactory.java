package pt.isep.escala.escalaapi.piquete;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pt.isep.escala.escalaapi.trace.TraceId;

@Component
class AvaliacaoPedidaFactory {

	static final UUID SKELETON_MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

	private final ObjectMapper objectMapper;

	AvaliacaoPedidaFactory(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	String payload(Slot slot, LocalDate dutyDate, UUID eventId) {
		Instant start = dutyDate.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant end = dutyDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		ObjectNode root = objectMapper.createObjectNode();
		root.put("eventId", eventId.toString());
		root.put("type", "AvaliacaoPedida");
		root.put("schemaVersion", 1);
		root.put("occurredAt", Instant.now().toString());
		root.put("traceId", TraceId.current());
		root.put("purpose", "ASSIGNMENT");
		root.put("assignmentId", slot.getAssignmentId().toString());
		ObjectNode slotNode = root.putObject("slot");
		slotNode.put("slotId", slot.getId().toString());
		slotNode.put("start", start.toString());
		slotNode.put("end", end.toString());
		slotNode.putNull("requiredCertification");
		slotNode.putNull("requiredLicenceClass");
		ObjectNode candidate = root.putObject("candidate");
		candidate.put("memberId", SKELETON_MEMBER_ID.toString());
		candidate.put("inActiveRoster", true);
		candidate.put("withinAgeLimit", true);
		candidate.set("certifications", objectMapper.createArrayNode());
		ArrayNode licences = objectMapper.createArrayNode();
		candidate.set("licenceClasses", licences);
		candidate.set("unavailabilities", objectMapper.createArrayNode());
		candidate.set("otherAssignments", objectMapper.createArrayNode());
		return objectMapper.writeValueAsString(root);
	}
}
