package pt.isep.escala.escalaapi.unavailability;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes the unavailability periods that ended more than {@code escala.unavailability.retention-days} days ago
 * (30 by default, 09 "Retention and deletion"), once a day. Logs only how many were deleted.
 */
@Component
public class UnavailabilityRetention {

	private static final Logger log = LoggerFactory.getLogger(UnavailabilityRetention.class);

	private final UnavailabilityRepository periods;
	private final Clock clock;
	private final Duration retention;

	UnavailabilityRetention(UnavailabilityRepository periods, Clock clock,
			@Value("${escala.unavailability.retention-days:30}") long retentionDays) {
		this.periods = periods;
		this.clock = clock;
		this.retention = Duration.ofDays(retentionDays);
	}

	@Scheduled(cron = "${escala.unavailability.retention-cron:0 30 3 * * *}", zone = "UTC")
	@Transactional
	public void deleteExpired() {
		Instant cutoff = clock.instant().minus(retention);
		int deleted = periods.deleteEndedBefore(cutoff);
		log.info("unavailability retention deleted={} endedBefore={}", deleted, cutoff);
	}
}
