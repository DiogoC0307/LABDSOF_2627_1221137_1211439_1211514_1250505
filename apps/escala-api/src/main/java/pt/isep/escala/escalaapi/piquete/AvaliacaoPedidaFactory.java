package pt.isep.escala.escalaapi.piquete;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pt.isep.escala.escalaapi.member.Member;
import pt.isep.escala.escalaapi.member.RosterStatus;
import pt.isep.escala.escalaapi.trace.TraceId;

/** The AvaliacaoPedida event (contracts/events/avaliacao-pedida.schema.json). */
@Component
class AvaliacaoPedidaFactory {

	static final String PURPOSE = "ASSIGNMENT";

	// A member is checked for a shift, before anyone is assigned
	static final String PURPOSE_CANDIDATE = "CANDIDATE";

	// A member is checked for a slot someone can't come to, before they are asked to cover it (SCRUM-72)
	static final String PURPOSE_COVER = "COVER";

	static final UUID SKELETON_MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

	record Period(Instant start, Instant end) {
	}

	private final ObjectMapper objectMapper;

	AvaliacaoPedidaFactory(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * The walking skeleton's check of a new slot (SCRUM-45): the whole duty day, for a placeholder member with no other
	 * shift nor unavailability.
	 */
	String payload(Slot slot, LocalDate dutyDate, UUID eventId) {
		Instant start = dutyDate.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant end = dutyDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		ObjectNode root = header(eventId, PURPOSE, slot.getAssignmentId(), slot.getId(), start, end);
		ObjectNode candidate = root.putObject("candidate");
		candidate.put("memberId", SKELETON_MEMBER_ID.toString());
		candidate.put("inActiveRoster", true);
		candidate.put("withinAgeLimit", true);
		candidate.set("certifications", objectMapper.createArrayNode());
		candidate.set("licenceClasses", objectMapper.createArrayNode());
		candidate.set("unavailabilities", objectMapper.createArrayNode());
		candidate.set("otherAssignments", objectMapper.createArrayNode());
		return objectMapper.writeValueAsString(root);
	}

	/**
	 * The check of a real member for a shift from start to end, with every fact the rules need (ADR 0008): whether they
	 * are on the active roster (RN01), their other shifts near this one (RN05) and the periods they can't come near it
	 * (RN06), plus their certifications and licences.
	 *
	 * @param assignmentId what the answer is matched by: the candidate check, or the assignment
	 * @param slotId the slot, or the check request while the shift is only a draft
	 */
	@SuppressWarnings("java:S107") // The event's own fields: one parameter each
	String payload(UUID eventId, String purpose, UUID assignmentId, UUID slotId, Instant start, Instant end,
			Member member, List<Period> unavailabilities, List<Period> otherAssignments) {
		ObjectNode root = header(eventId, purpose, assignmentId, slotId, start, end);
		ObjectNode candidate = root.putObject("candidate");
		candidate.put("memberId", member.getId().toString());
		candidate.put("inActiveRoster", member.isActive() && member.getRosterStatus() == RosterStatus.ACTIVE_ROSTER);
		candidate.put("withinAgeLimit", member.isWithinAgeLimit());
		ArrayNode certifications = candidate.putArray("certifications");
		member.getCertifications().forEach(held -> certifications.addObject()
				.put("type", held.getType().getCode())
				.put("expiresOn", held.getExpiresOn().toString()));
		ArrayNode licences = candidate.putArray("licenceClasses");
		member.getLicences().forEach(licence -> licences.add(licence.getLicenceClass().name()));
		periods(candidate.putArray("unavailabilities"), unavailabilities);
		periods(candidate.putArray("otherAssignments"), otherAssignments);
		return objectMapper.writeValueAsString(root);
	}

	private ObjectNode header(UUID eventId, String purpose, UUID assignmentId, UUID slotId, Instant start, Instant end) {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("eventId", eventId.toString());
		root.put("type", "AvaliacaoPedida");
		root.put("schemaVersion", 1);
		root.put("occurredAt", Instant.now().toString());
		root.put("traceId", TraceId.current());
		root.put("purpose", purpose);
		root.put("assignmentId", assignmentId.toString());
		ObjectNode slotNode = root.putObject("slot");
		slotNode.put("slotId", slotId.toString());
		slotNode.put("start", start.toString());
		slotNode.put("end", end.toString());
		slotNode.putNull("requiredCertification");
		slotNode.putNull("requiredLicenceClass");
		return root;
	}

	private static void periods(ArrayNode into, List<Period> periods) {
		periods.forEach(period -> into.addObject().put("start", period.start().toString())
				.put("end", period.end().toString()));
	}
}
