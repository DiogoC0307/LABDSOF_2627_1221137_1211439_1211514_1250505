package pt.isep.escala.escalaapi.piquete;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	/** A sanity bound on the crew, far above any real shift of the corps. */
	static final int MOST_CREW = 50;

	/** When the crew shrinks, the slots furthest from being filled go first. */
	private static final Comparator<Slot> FIRST_TO_REMOVE = Comparator.comparingInt(slot -> switch (slot.getStatus()) {
		case OPEN -> 0;
		case EVALUATION_FAILED -> 1;
		case PENDING -> 2;
		case ELIGIBLE -> 3;
		case PROPOSED, FILLED -> 4;
	});

	private static final Logger log = LoggerFactory.getLogger(PiqueteService.class);
	// The fields and the code of a refused shift (ErrorResponse of the contract)
	private static final String DUTY_DATE = "dutyDate";
	private static final String ENDS_AT = "endsAt";
	private static final String REQUIRED = "required";

	private final PiqueteRepository piquetes;
	private final OutboxEventRepository outbox;
	private final AvaliacaoPedidaFactory pedidos;
	private final EvaluationMetrics metrics;
	private final AssignmentRepository assignments;
	private final Clock clock;

	PiqueteService(
			PiqueteRepository piquetes, OutboxEventRepository outbox, AvaliacaoPedidaFactory pedidos,
			EvaluationMetrics metrics, AssignmentRepository assignments, Clock clock) {
		this.assignments = assignments;
		this.metrics = metrics;
		this.piquetes = piquetes;
		this.outbox = outbox;
		this.pedidos = pedidos;
		this.clock = clock;
	}

	/**
	 * Creates a piquete and queues the evaluation of each of its slots. With the shift's fields (SCRUM-65) they are all
	 * checked, the day cannot be before today, and there is one slot for each person of each role in the crew; with
	 * only dutyDate it is the walking skeleton, as before, with one CREW_MEMBER slot.
	 */
	@Transactional
	PiqueteResponse create(PiqueteRequest request) {
		if (request == null) {
			throw new InvalidRequestException(List.of(new InvalidField(DUTY_DATE, REQUIRED)));
		}
		Instant shiftStartsAt = request.startsAt() != null
				? request.startsAt().toInstant()
				: request.dutyDate() != null
				? request.dutyDate().atTime(LocalTime.MIDNIGHT).atZone(LISBON).toInstant()
				: null;
		Piquete piquete = new Piquete(UUID.randomUUID(), request.dutyDate());
		List<Slot> added = new ArrayList<>();
		if (request.onlyDutyDate()) {
			if (request.dutyDate() == null) {
				throw new InvalidRequestException(List.of(new InvalidField(DUTY_DATE, REQUIRED)));
			}
			added.addAll(addSlots(piquete, SlotRole.CREW_MEMBER, 1, shiftStartsAt));
		} else {
			checkShift(request);
			schedule(piquete, request);
			crew(request).forEach((role, count) -> added.addAll(addSlots(piquete, role, count, shiftStartsAt)));
		}
		piquetes.save(piquete);
		queueEvaluations(added, piquete.getDutyDate());
		return response(piquete);
	}

	/** One piquete, for the Location of POST (SCRUM-88). */
	@Transactional(readOnly = true)
	Optional<PiqueteResponse> get(UUID id) {
		return piquetes.findById(id).map(this::response);
	}

	/**
	 * Changes a shift's day, period, times and crew (SCRUM-65). Empty when there is no such piquete. A shift on a day
	 * that has passed cannot change here (409), nor be moved to one (400). A role that needs more people gets new
	 * slots, each evaluated; one that needs fewer loses the slots furthest from being filled, never a slot someone
	 * was proposed for or holds (400, below_filled). The other slots are kept as they are.
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
		Map<SlotRole, Integer> crew = crew(request);
		checkTakenSlots(piquete, crew);
		schedule(piquete, request);
		List<Slot> added = new ArrayList<>();
		for (SlotRole role : SlotRole.values()) {
			List<Slot> current = piquete.getSlots().stream().filter(slot -> slot.getRole() == role).toList();
			int wanted = crew.getOrDefault(role, 0);
			if (wanted > current.size()) {
				added.addAll(addSlots(piquete, role, wanted - current.size(), request.startsAt().toInstant()));
			} else {
				current.stream().sorted(FIRST_TO_REMOVE).limit((long) current.size() - wanted).forEach(piquete::removeSlot);
			}
		}
		piquetes.save(piquete);
		queueEvaluations(added, piquete.getDutyDate());
		if (log.isInfoEnabled()) {
			log.info("piquete changed id={} slots={} traceId={}", id, piquete.getSlots().size(), TraceId.current());
		}
		return Optional.of(response(piquete));
	}

	@Transactional(readOnly = true)
	List<PiqueteResponse> list() {
		return responses(piquetes.findAll());
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

		return responses(piquetes.findByDutyDateBetweenOrderByDutyDateAsc(isoWeek.monday(), isoWeek.sunday()));
	}

	LocalDate today() {
		return LocalDate.now(clock.withZone(LISBON));
	}

	/** Every field is required; the shift starts on its day (in Lisbon), not before today, ends after it starts. */
	private void checkShift(PiqueteRequest request) {
		List<InvalidField> errors = shiftErrors(request, "", today());
		if (!errors.isEmpty()) {
			throw new InvalidRequestException(errors);
		}
	}

	/**
	 * How a published shift is checked and what is wrong with it
	 */
	static List<InvalidField> shiftErrors(PiqueteRequest request, String prefix, LocalDate today) {
		List<InvalidField> errors = new ArrayList<>();
		if (request.dutyDate() == null) {
			errors.add(new InvalidField(prefix + DUTY_DATE, REQUIRED));
		} else if (request.dutyDate().isBefore(today)) {
			errors.add(new InvalidField(prefix + DUTY_DATE, "in_the_past"));
		}
		if (request.period() == null) {
			errors.add(new InvalidField(prefix + "period", REQUIRED));
		}
		if (request.startsAt() == null) {
			errors.add(new InvalidField(prefix + "startsAt", REQUIRED));
		} else if (request.dutyDate() != null
				&& !request.startsAt().atZoneSameInstant(LISBON).toLocalDate().equals(request.dutyDate())) {
			errors.add(new InvalidField(prefix + "startsAt", "not_on_duty_date"));
		}
		if (request.endsAt() == null) {
			errors.add(new InvalidField(prefix + ENDS_AT, REQUIRED));
		} else if (request.startsAt() != null) {
			Duration length = Duration.between(request.startsAt(), request.endsAt());
			if (length.isZero() || length.isNegative()) {
				errors.add(new InvalidField(prefix + ENDS_AT, "ends_before_start"));
			} else if (length.compareTo(LONGEST_SHIFT) > 0) {
				errors.add(new InvalidField(prefix + ENDS_AT, "too_long"));
			}
		}
		String crewError = crewError(request.crew());
		if (crewError != null) {
			errors.add(new InvalidField(prefix + "crew", crewError));
		}
		return errors;
	}

	/**
	 * Why the crew is refused, or null: it lists each role at most once (duplicate), each with a count of 0 or more
	 * (invalid), and the whole crew is 1 to 50 people (too_small, too_large).
	 */
	private static String crewError(List<CrewNeed> crew) {
		if (crew == null || crew.isEmpty()) {
			return REQUIRED;
		}
		Set<SlotRole> roles = EnumSet.noneOf(SlotRole.class);
		long total = 0;
		for (CrewNeed need : crew) {
			if (need == null || need.role() == null || need.count() == null || need.count() < 0) {
				return "invalid";
			}
			if (!roles.add(need.role())) {
				return "duplicate";
			}
			total += need.count();
		}
		if (total < 1) {
			return "too_small";
		}
		return total > MOST_CREW ? "too_large" : null;
	}

	/** The crew, already checked, as a count per role in the order of SlotRole; roles that need nobody are left out. */
	static Map<SlotRole, Integer> crew(PiqueteRequest request) {
		Map<SlotRole, Integer> crew = new EnumMap<>(SlotRole.class);
		request.crew().stream().filter(need -> need.count() > 0).forEach(need -> crew.put(need.role(), need.count()));
		return crew;
	}

	/** A role cannot need fewer people than are already proposed for or holding its slots. */
	private static void checkTakenSlots(Piquete piquete, Map<SlotRole, Integer> crew) {
		for (SlotRole role : SlotRole.values()) {
			long taken = piquete.getSlots().stream().filter(slot -> slot.getRole() == role && slot.isTaken()).count();
			if (crew.getOrDefault(role, 0) < taken) {
				throw new InvalidRequestException(List.of(new InvalidField("crew", "below_filled")));
			}
		}
	}

	private static List<Slot> addSlots(Piquete piquete, SlotRole role, int count, Instant shiftStartsAt) {
		List<Slot> added = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			Slot slot = new Slot(UUID.randomUUID(), role, SlotStatus.PENDING, UUID.randomUUID(), shiftStartsAt);
			piquete.addSlot(slot);
			added.add(slot);
		}
		return added;
	}

	/** One AvaliacaoPedida per new slot, through the outbox, in the same transaction as the slots. */
	private void queueEvaluations(List<Slot> added, LocalDate dutyDate) {
		for (Slot slot : added) {
			UUID eventId = UUID.randomUUID();
			String payload = pedidos.payload(slot, dutyDate, eventId);
			outbox.save(new OutboxEvent(eventId, ROUTING_AVALIACAO_PEDIDA, payload, Instant.now()));
			metrics.requested(AvaliacaoPedidaFactory.PURPOSE);
			if (log.isInfoEnabled()) {
				log.info("AvaliacaoPedida queued eventId={} assignmentId={} traceId={}", eventId, slot.getAssignmentId(),
						TraceId.current());
			}
		}
	}

	private static void schedule(Piquete piquete, PiqueteRequest request) {
		int size = crew(request).values().stream().mapToInt(Integer::intValue).sum();
		piquete.schedule(request.dutyDate(), request.period(), request.startsAt().toInstant(),
				request.endsAt().toInstant(), size);
	}

	private PiqueteResponse response(Piquete piquete) {
		return responses(List.of(piquete)).getFirst();
	}

	// The piquetes with who holds each slot
	private List<PiqueteResponse> responses(List<Piquete> list) {
		List<UUID> ids = list.stream().map(Piquete::getId).toList();
		Map<UUID, UUID> memberOfSlot = new HashMap<>();
		if (!ids.isEmpty()) {
			assignments.findByPiqueteIdInAndStatus(ids, Assignment.Status.CONFIRMED)
					.forEach(assignment -> memberOfSlot.put(assignment.getSlotId(), assignment.getMemberId()));
		}
		return list.stream().map(piquete -> toResponse(piquete, memberOfSlot)).toList();
	}

	private static PiqueteResponse toResponse(Piquete piquete, Map<UUID, UUID> memberOfSlot) {
		List<SlotResponse> slots = piquete.getSlots().stream()
				.map(slot -> new SlotResponse(slot.getId(), slot.getRole(), slot.getStatus(), memberOfSlot.get(slot.getId())))
				.toList();
		return new PiqueteResponse(piquete.getId(), piquete.getDutyDate(), piquete.getPeriod(), piquete.getStartsAt(),
				piquete.getEndsAt(), piquete.getMinimumCrew(), slots);
	}
}