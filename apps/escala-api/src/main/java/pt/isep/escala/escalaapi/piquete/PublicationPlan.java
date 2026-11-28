package pt.isep.escala.escalaapi.piquete;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Published weekly shcedule as the supervisor sent it to be published and already checked
 */
record PublicationPlan(List<PlannedShift> shifts, List<UUID> deleted) {

	/**
	 * @param key the supervisor's own name for the shift, so the PWA finds it in its draft (a new one has no id yet)
	 * @param created a new shift, which does not exist until the week is published
	 */
	record PlannedShift(String key, UUID piqueteId, boolean created, LocalDate dutyDate, ShiftPeriod period,
			Instant startsAt, Instant endsAt, List<PlannedSlot> slots) {

		// If there's no one assigned to the shift, then the shift cannot be published
		boolean empty() {
			return slots.stream().allMatch(slot -> slot.memberId() == null);
		}
	}

	// Assigned member to a role on a shift
	record PlannedSlot(UUID slotId, SlotRole role, UUID memberId) {
	}
}
