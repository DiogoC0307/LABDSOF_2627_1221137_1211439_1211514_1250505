package pt.isep.escala.escalaapi.unavailability;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The periods a member said they can't come (start and end)
 */
@Component
public class UnavailabilityPeriods {

	public record Period(Instant startsAt, Instant endsAt) {
	}

	private final UnavailabilityRepository unavailabilities;

	UnavailabilityPeriods(UnavailabilityRepository unavailabilities) {
		this.unavailabilities = unavailabilities;
	}

	// Members periods that ovelap with the given period
	@Transactional(readOnly = true)
	public List<Period> overlapping(UUID memberId, Instant from, Instant to) {
		return unavailabilities.findByMemberIdAndStartsAtBeforeAndEndsAtAfterOrderByStartsAt(memberId, to, from).stream()
				.map(each -> new Period(each.getStartsAt(), each.getEndsAt()))
				.toList();
	}
}
