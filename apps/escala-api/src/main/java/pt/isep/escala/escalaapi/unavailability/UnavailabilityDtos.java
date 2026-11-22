package pt.isep.escala.escalaapi.unavailability;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A period from the form: ISO 8601 with an offset; stored in UTC, end exclusive (SCRUM-64). */
record UnavailabilityRequest(OffsetDateTime startsAt, OffsetDateTime endsAt) {
}

record UnavailabilityResponse(UUID id, Instant startsAt, Instant endsAt, String source) {
}
