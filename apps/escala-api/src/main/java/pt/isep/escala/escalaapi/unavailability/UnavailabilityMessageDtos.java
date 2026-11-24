package pt.isep.escala.escalaapi.unavailability;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** What the member writes (SCRUM-75): free text, at most 500 characters. */
record MessageRequest(String text) {
}

/**
 * A message as its author sees it: the state and the proposed periods, never the free text, which the PWA already
 * has. {@code method} is null until the worker answers.
 */
record MessageResponse(UUID id, String status, LocalDate referenceDate, String method, List<ProposedPeriod> periods) {
}

/** The periods the member confirms, maybe edited; patterns are expanded only here (decision D). */
record ConfirmRequest(List<ProposedPeriod> periods) {
}

/**
 * A period in the format of ExtracaoConcluida v2 (06): dates inclusive, optional weekdays (MON to SUN) and optional
 * Lisbon times HH:mm. Kept as text so a bad value is reported per field instead of as an unreadable body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ProposedPeriod(String startDate, String endDate, List<String> weekdays, String startTime, String endTime) {
}
