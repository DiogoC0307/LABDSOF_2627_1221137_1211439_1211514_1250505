package pt.isep.escala.elegibilidadeworker.risk;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEvent;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEventRepository;

/** Hourly RCM poll (ADR 0010). Off in the test profile so the suite never calls api.ipma.pt. */
@Component
class IpmaPoller {

	static final String ROUTING_KEY = "risco.atualizado";

	private static final Logger log = LoggerFactory.getLogger(IpmaPoller.class);

	private final RiskProperties properties;
	private final IpmaClient client;
	private final RiskObservationRepository observations;
	private final OutboxEventRepository outbox;
	private final ObjectMapper objectMapper;
	private final Clock clock;
	/** The Spring proxy. {@link #poll()} is transactional only when this proxy calls it. */
	private final IpmaPoller self;

	IpmaPoller(
			RiskProperties properties,
			IpmaClient client,
			RiskObservationRepository observations,
			OutboxEventRepository outbox,
			ObjectMapper objectMapper,
			Clock clock,
			@Lazy IpmaPoller self) {
		this.properties = properties;
		this.client = client;
		this.observations = observations;
		this.outbox = outbox;
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.self = self;
	}

	@Scheduled(fixedRateString = "${escala.risk.poll-interval:1h}")
	void scheduled() {
		if (!properties.isEnabled()) {
			return;
		}
		self.poll();
	}

	@Transactional
	void poll() {
		Instant now = clock.instant();
		RiskCache.Reading reading = client.fetch(
				properties.getBaseUrl(),
				properties.getDico(),
				properties.getTimeout(),
				properties.getRetries(),
				properties.getStaleness(),
				clock);
		RiskSnapshot current = observations.findById(properties.getDico()).map(RiskObservation::snapshot).orElse(null);
		if (current == null) {
			current = new RiskSnapshot(properties.getDico(), null, null, null, false, RiskOutcome.UNAVAILABLE, now,
					properties.source());
		}
		RiskSnapshot next = RiskCache.apply(current, reading, now, properties.getStaleness(), properties.source());
		observations.save(new RiskObservation(next));
		if (!next.publishable()) {
			log.info("IPMA poll kept no value dico={} outcome={}", properties.getDico(), next.lastOutcome());
			return;
		}
		UUID eventId = UUID.randomUUID();
		outbox.save(new OutboxEvent(
				eventId,
				ROUTING_KEY,
				RiscoAtualizadoJson.of(objectMapper, eventId, next, now, eventId.toString()),
				now));
		log.info("IPMA poll dico={} rcm={} outcome={} assumedHigher={}", next.dico(), next.rcm(), next.lastOutcome(),
				next.assumedHigher());
	}
}
