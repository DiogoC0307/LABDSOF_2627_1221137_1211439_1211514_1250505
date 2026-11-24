package pt.isep.escala.escalaapi.unavailability;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.outbox.OutboxEventRepository;

/**
 * Deletes the free text of the messages not confirmed or discarded within
 * {@code escala.unavailability-messages.text-hours} hours (24 by default, RNF09, 09), with their outbox rows. Runs
 * every 10 minutes by default, so a text lives at most 24 hours and 10 minutes. Logs only how many.
 */
@Component
public class UnavailabilityMessageRetention {

	private static final Logger log = LoggerFactory.getLogger(UnavailabilityMessageRetention.class);

	private final UnavailabilityMessageRepository messages;
	private final OutboxEventRepository outbox;
	private final Clock clock;
	private final Duration textLifetime;

	UnavailabilityMessageRetention(UnavailabilityMessageRepository messages, OutboxEventRepository outbox, Clock clock,
			@Value("${escala.unavailability-messages.text-hours:24}") long textHours) {
		this.messages = messages;
		this.outbox = outbox;
		this.clock = clock;
		this.textLifetime = Duration.ofHours(textHours);
	}

	@Scheduled(cron = "${escala.unavailability-messages.expiry-cron:0 */10 * * * *}", zone = "UTC")
	@Transactional
	public void expire() {
		Instant now = clock.instant();
		Instant cutoff = now.minus(textLifetime);
		List<UnavailabilityMessage> expired = messages.findByFreeTextIsNotNullAndCreatedAtLessThanEqual(cutoff);
		for (UnavailabilityMessage message : expired) {
			message.expire(now);
			outbox.deleteById(message.getRequestEventId());
		}
		log.info("unavailability messages expired count={} createdBefore={}", expired.size(), cutoff);
	}
}
