package pt.isep.escala.escalaapi.piquete;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record CreatePiqueteRequest(LocalDate dutyDate) {
}

record SlotResponse(UUID id, SlotRole role, SlotStatus status) {
}

record PiqueteResponse(UUID id, LocalDate dutyDate, List<SlotResponse> slots) {
}
