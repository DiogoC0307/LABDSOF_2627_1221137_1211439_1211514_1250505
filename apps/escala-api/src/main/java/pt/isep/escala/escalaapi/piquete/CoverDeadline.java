package pt.isep.escala.escalaapi.piquete;

import java.time.Duration;
import java.time.Instant;

/**
 * When the supervisor is alerted that nobody accepted a cover request (SCRUM-72 AC4): a while after the request, or a
 * while before the shift, whichever comes first. Both are set per corps, in {@code escala.cover.*}; there is one corps
 * per installation.
 */
record CoverDeadline(Duration afterRequest, Duration beforeShift) {

	Instant of(Instant openedAt, Instant shiftStartsAt) {
		Instant late = openedAt.plus(afterRequest);
		Instant soon = shiftStartsAt.minus(beforeShift);
		return late.isBefore(soon) ? late : soon;
	}

	boolean hasPassed(Instant openedAt, Instant shiftStartsAt, Instant now) {
		return !now.isBefore(of(openedAt, shiftStartsAt));
	}
}
