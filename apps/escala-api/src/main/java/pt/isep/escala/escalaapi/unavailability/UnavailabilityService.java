package pt.isep.escala.escalaapi.unavailability;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.unavailability.PatternExpansion.Interval;
import pt.isep.escala.escalaapi.validation.InvalidRequestException;
import pt.isep.escala.escalaapi.validation.InvalidRequestException.InvalidField;

/**
 * A member's own unavailability (SCRUM-64): the member always comes from the token, never from the request (T-03),
 * and a period of another member is not found. Overlapping periods are kept as they are.
 */
@Service
class UnavailabilityService {

	/** As far ahead as a period may start: the same limit as the AI extraction on 06. */
	static final int MONTHS_AHEAD = 6;

	private static final Logger log = LoggerFactory.getLogger(UnavailabilityService.class);

	private static final String STARTS_AT = "startsAt";
	private static final String ENDS_AT = "endsAt";

	private final UnavailabilityRepository periods;
	private final Clock clock;

	UnavailabilityService(UnavailabilityRepository periods, Clock clock) {
		this.periods = periods;
		this.clock = clock;
	}

	@Transactional
	UnavailabilityResponse declare(UUID memberId, UnavailabilityRequest request) {
		Instant now = clock.instant();
		check(request, now);
		Unavailability period = periods.save(new Unavailability(
				memberId, request.startsAt().toInstant(), request.endsAt().toInstant(), now));
		log.info("unavailability declared id={} memberId={}", period.getId(), memberId);
		return view(period);
	}

	/**
	 * Periods confirmed from a free-text message (SCRUM-75), already checked and expanded by
	 * UnavailabilityMessageService; in the caller's transaction, so the free text is deleted together.
	 */
	@Transactional
	List<UnavailabilityResponse> declareFromText(UUID memberId, List<Interval> intervals, Instant now) {
		List<UnavailabilityResponse> saved = new ArrayList<>();
		for (Interval interval : intervals) {
			saved.add(view(periods.save(new Unavailability(
					memberId, interval.start(), interval.end(), Unavailability.SOURCE_FREE_TEXT, now))));
		}
		log.info("unavailability declared from text memberId={} periods={}", memberId, saved.size());
		return saved;
	}

	@Transactional(readOnly = true)
	List<UnavailabilityResponse> mine(UUID memberId) {
		return periods.findByMemberIdOrderByStartsAt(memberId).stream().map(UnavailabilityService::view).toList();
	}

	@Transactional(readOnly = true)
	Optional<UnavailabilityResponse> mine(UUID memberId, UUID id) {
		return periods.findByIdAndMemberId(id, memberId).map(UnavailabilityService::view);
	}

	/** Deletes the member's own period; false when there is none with that ID for this member. */
	@Transactional
	boolean delete(UUID memberId, UUID id) {
		return periods.findByIdAndMemberId(id, memberId).map(period -> {
			periods.delete(period);
			log.info("unavailability deleted id={} memberId={}", id, memberId);
			return true;
		}).orElse(false);
	}

	private void check(UnavailabilityRequest request, Instant now) {
		List<InvalidField> errors = new ArrayList<>();
		if (request.startsAt() == null) {
			errors.add(new InvalidField(STARTS_AT, "required"));
		}
		if (request.endsAt() == null) {
			errors.add(new InvalidField(ENDS_AT, "required"));
		}
		if (errors.isEmpty()) {
			Instant startsAt = request.startsAt().toInstant();
			Instant endsAt = request.endsAt().toInstant();
			if (!endsAt.isAfter(startsAt)) {
				errors.add(new InvalidField(ENDS_AT, "ends_before_start"));
			} else if (!endsAt.isAfter(now)) {
				errors.add(new InvalidField(ENDS_AT, "in_the_past"));
			}
			if (startsAt.isAfter(now.atZone(ZoneOffset.UTC).plusMonths(MONTHS_AHEAD).toInstant())) {
				errors.add(new InvalidField(STARTS_AT, "too_far_ahead"));
			}
		}
		if (!errors.isEmpty()) {
			throw new InvalidRequestException(errors);
		}
	}

	private static UnavailabilityResponse view(Unavailability period) {
		return new UnavailabilityResponse(period.getId(), period.getStartsAt(), period.getEndsAt(), period.getSource());
	}
}
