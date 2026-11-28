package pt.isep.escala.escalaapi.piquete;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A shift as the supervisor sets it (SCRUM-65, D1): the day, period, start, end and the crew it needs, as how many
 * people of each role. On create, a body with only dutyDate still makes a piquete with no times (the walking
 * skeleton, SCRUM-45); on edit every field is required.
 */
record PiqueteRequest(
		LocalDate dutyDate, ShiftPeriod period, OffsetDateTime startsAt, OffsetDateTime endsAt, List<CrewNeed> crew) {

	/** Only dutyDate: the walking-skeleton piquete, with no shift times. */
	boolean onlyDutyDate() {
		return period == null && startsAt == null && endsAt == null && crew == null;
	}
}

/** How many people of one role the shift needs: one slot each. A role left out needs nobody. */
record CrewNeed(SlotRole role, Integer count) {
}

record SlotResponse(UUID id, SlotRole role, SlotStatus status, UUID memberId) {
}

/** period, startsAt, endsAt and minimumCrew are null on a piquete created without them. */
record PiqueteResponse(
		UUID id,
		LocalDate dutyDate,
		ShiftPeriod period,
		Instant startsAt,
		Instant endsAt,
		Integer minimumCrew,
		List<SlotResponse> slots) {
}
