package pt.isep.escala.escalaapi.piquete;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The week the supervisor publishes, with every shift of the week on a day that has not passed.
 * 
 * A shift of the week left out is deleted. 
 * Shifts on days that have passed are never changed nor deleted.
 */
record PublicationRequest(String week, List<PublishedShift> shifts) {
}

// A shift with the members assigned to it
record PublishedShift(
		String key,
		UUID id,
		LocalDate dutyDate,
		ShiftPeriod period,
		OffsetDateTime startsAt,
		OffsetDateTime endsAt,
		List<CrewNeed> crew,
		List<AssignedMember> assigned) {

	PiqueteRequest asPiqueteRequest() {
		return new PiqueteRequest(dutyDate, period, startsAt, endsAt, crew);
	}
}

// Assigned member to a specific role on a shift
record AssignedMember(SlotRole role, UUID memberId) {
}

record PublicationResponse(UUID id, String week, WeekPublication.Status status, List<PublicationConflict> conflicts) {
}

// Conflict/Why a shift cannot be published
record PublicationConflict(
		PublicationConflict.Type type, String shiftKey, UUID shiftId, LocalDate dutyDate, UUID memberId,
		SlotRole role, List<String> rules) {

	enum Type {
		EMPTY_SHIFT,
		INELIGIBLE,
		NOT_CHECKED
	}
}
