package pt.isep.escala.escalaapi.piquete;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

record CreatePiqueteRequest(LocalDate dutyDate, LocalTime startTime) {

    CreatePiqueteRequest(LocalDate dutyDate) {
        this(dutyDate, null);
    }
}

record SlotResponse(UUID id, SlotRole role, SlotStatus status) {
}

record PiqueteResponse(UUID id, LocalDate dutyDate, List<SlotResponse> slots) {
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