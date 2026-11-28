package pt.isep.escala.escalaapi.piquete;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

record CreatePiqueteRequest(LocalDate dutyDate, LocalTime startTime) {

	CreatePiqueteRequest(LocalDate dutyDate) {
		this(dutyDate, null);
	}
}

record PiqueteRequest(
		LocalDate dutyDate, ShiftPeriod period, OffsetDateTime startsAt, OffsetDateTime endsAt,
		List<CrewNeed> crew) {

	boolean onlyDutyDate() {
		return period == null && startsAt == null && endsAt == null && crew == null;
	}
}

record CrewNeed(SlotRole role, Integer count) {
}

record SlotResponse(UUID id, SlotRole role, SlotStatus status, UUID memberId) {
}

record PiqueteResponse(
		UUID id,
		LocalDate dutyDate,
		ShiftPeriod period,
		Instant startsAt,
		Instant endsAt,
		Integer minimumCrew,
		List<SlotResponse> slots) {
}

record PresenceRequest(PresenceDecision decision) {

	enum PresenceDecision {
		COMING,
		CANNOT_COME
	}
}

record PresencePrompt(
		UUID slotId,
		UUID assignmentId,
		Instant shiftStartsAt,
		String message) {
}

record PresenceResponse(
		UUID slotId,
		PresenceStatus presenceStatus,
		SlotStatus slotStatus,
		boolean slotFreed) {
}