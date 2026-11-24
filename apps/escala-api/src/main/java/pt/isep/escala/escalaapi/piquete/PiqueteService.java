package pt.isep.escala.escalaapi.piquete;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.isep.escala.escalaapi.metrics.EvaluationMetrics;
import pt.isep.escala.escalaapi.outbox.OutboxEvent;
import pt.isep.escala.escalaapi.outbox.OutboxEventRepository;
import pt.isep.escala.escalaapi.trace.TraceId;
import pt.isep.escala.escalaapi.validation.InvalidRequestException;
import pt.isep.escala.escalaapi.validation.InvalidRequestException.InvalidField;

@Service
class PiqueteService {

	static final String ROUTING_AVALIACAO_PEDIDA = "avaliacao.pedida";

	/** The corps' days are Lisbon days: "today" and the day a shift starts on are read in this zone. */
	static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");

	/** A shift is at most a day long: a day shift, a night shift, or one 24-hour piquete. */
	static final Duration LONGEST_SHIFT = Duration.ofHours(24);

	/** A sanity bound on the minimum crew, far above any real shift of the corps. */
	static final int MOST_CREW = 50;

	private static final Logger log = LoggerFactory.getLogger(PiqueteService.class);

	private final PiqueteRepository piquetes;
	private final OutboxEventRepository outbox;
	private final AvaliacaoPedidaFactory pedidos;
	private final EvaluationMetrics metrics;
	private final Clock clock;

	PiqueteService(
			PiqueteRepository piquetes, OutboxEventRepository outbox, AvaliacaoPedidaFactory pedidos,
			EvaluationMetrics metrics, Clock clock) {
		this.metrics = metrics;
		this.piquetes = piquetes;
		this.outbox = outbox;
		this.pedidos = pedidos;
		this.clock = clock;
	}

	/**
	 * Creates a piquete with one slot and queues its evaluation. With the shift's fields (SCRUM-65) they are all
	 * checked, and the day cannot be before today; with only dutyDate it is the walking skeleton, as before.
	 */
	@Transactional
	PiqueteResponse create(PiqueteRequest request) {
		if (request == null || request.dutyDate() == null && request.onlyDutyDate()) {
			throw new InvalidRequestException(List.of(new InvalidField("dutyDate", "required")));
		}
		Piquete piquete = new Piquete(UUID.randomUUID(), request.dutyDate());
		if (!request.onlyDutyDate()) {
			checkShift(request);
			schedule(piquete, request);
		}
		UUID assignmentId = UUID.randomUUID();
		Slot slot = new Slot(UUID.randomUUID(), SlotRole.CREW_MEMBER, SlotStatus.PENDING, assignmentId);
		piquete.addSlot(slot);
		piquetes.save(piquete);
		UUID eventId = UUID.randomUUID();
		String payload = pedidos.payload(slot, request.dutyDate(), eventId);
		outbox.save(new OutboxEvent(eventId, ROUTING_AVALIACAO_PEDIDA, payload, Instant.now()));
		metrics.requested(AvaliacaoPedidaFactory.PURPOSE);
		log.info("AvaliacaoPedida queued eventId={} assignmentId={} traceId={}", eventId, assignmentId, TraceId.current());
		return toResponse(piquete);
	}

	/** One piquete, for the Location of POST (SCRUM-88). */
	@Transactional(readOnly = true)
	Optional<PiqueteResponse> get(UUID id) {
		return piquetes.findById(id).map(PiqueteService::toResponse);
	}

	/**
	 * Changes a shift's day, period, times and minimum crew (SCRUM-65). Empty when there is no such piquete. A shift on
	 * a day that has passed cannot change here (409), nor be moved to one (400). Its slots are kept as they are.
	 */
	@Transactional
	Optional<PiqueteResponse> update(UUID id, PiqueteRequest request) {
		Optional<Piquete> found = piquetes.findById(id);
		if (found.isEmpty()) {
			return Optional.empty();
		}
		Piquete piquete = found.get();
		if (piquete.getDutyDate().isBefore(today())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A shift on a day that has passed cannot change");
		}
		checkShift(request);
		schedule(piquete, request);
		log.info("piquete changed id={} traceId={}", id, TraceId.current());
		return Optional.of(toResponse(piquete));
	}

	@Transactional(readOnly = true)
	List<PiqueteResponse> list() {
		return piquetes.findAll().stream().map(PiqueteService::toResponse).toList();
	}

	/**
	 * Gets the week's piquetes from Monday to Sunday
	 *
	 * @param week week in ISO 8601 format yyyy-Www (e.g. 2026-W48)
	 * @return the piquetes for that week or an empty list if none exist
	 */
	@Transactional(readOnly = true)
	List<PiqueteResponse> listWeek(String week) {
		IsoWeek isoWeek;

		try {
			isoWeek = IsoWeek.parse(week);
		} catch (IllegalArgumentException e) {
			throw new InvalidRequestException(List.of(new InvalidField("week", "invalid_week")));
		}

		return piquetes.findByDutyDateBetweenOrderByDutyDateAsc(isoWeek.monday(), isoWeek.sunday()).stream()
				.map(PiqueteService::toResponse)
				.toList();
	}

	private LocalDate today() {
		return LocalDate.now(clock.withZone(LISBON));
	}

	/** Every field is required; the shift starts on its day (in Lisbon), not before today, ends after it starts. */
	private void checkShift(PiqueteRequest request) {
		List<InvalidField> errors = new ArrayList<>();
		if (request.dutyDate() == null) {
			errors.add(new InvalidField("dutyDate", "required"));
		} else if (request.dutyDate().isBefore(today())) {
			errors.add(new InvalidField("dutyDate", "in_the_past"));
		}
		if (request.period() == null) {
			errors.add(new InvalidField("period", "required"));
		}
		if (request.startsAt() == null) {
			errors.add(new InvalidField("startsAt", "required"));
		} else if (request.dutyDate() != null
				&& !request.startsAt().atZoneSameInstant(LISBON).toLocalDate().equals(request.dutyDate())) {
			errors.add(new InvalidField("startsAt", "not_on_duty_date"));
		}
		if (request.endsAt() == null) {
			errors.add(new InvalidField("endsAt", "required"));
		} else if (request.startsAt() != null) {
			Duration length = Duration.between(request.startsAt(), request.endsAt());
			if (length.isZero() || length.isNegative()) {
				errors.add(new InvalidField("endsAt", "ends_before_start"));
			} else if (length.compareTo(LONGEST_SHIFT) > 0) {
				errors.add(new InvalidField("endsAt", "too_long"));
			}
		}
		if (request.minimumCrew() == null) {
			errors.add(new InvalidField("minimumCrew", "required"));
		} else if (request.minimumCrew() < 1) {
			errors.add(new InvalidField("minimumCrew", "too_small"));
		} else if (request.minimumCrew() > MOST_CREW) {
			errors.add(new InvalidField("minimumCrew", "too_large"));
		}
		if (!errors.isEmpty()) {
			throw new InvalidRequestException(errors);
		}
	}

	private static void schedule(Piquete piquete, PiqueteRequest request) {
		piquete.schedule(request.dutyDate(), request.period(), request.startsAt().toInstant(),
				request.endsAt().toInstant(), request.minimumCrew());
	}

	private static PiqueteResponse toResponse(Piquete piquete) {
		List<SlotResponse> slots = piquete.getSlots().stream()
				.map(slot -> new SlotResponse(slot.getId(), slot.getRole(), slot.getStatus()))
				.toList();
		return new PiqueteResponse(piquete.getId(), piquete.getDutyDate(), piquete.getPeriod(), piquete.getStartsAt(),
				piquete.getEndsAt(), piquete.getMinimumCrew(), slots);
	}
}
