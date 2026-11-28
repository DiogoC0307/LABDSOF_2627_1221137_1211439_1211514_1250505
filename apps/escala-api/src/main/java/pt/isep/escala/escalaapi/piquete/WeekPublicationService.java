package pt.isep.escala.escalaapi.piquete;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.member.Member;
import pt.isep.escala.escalaapi.member.MemberRepository;
import pt.isep.escala.escalaapi.metrics.EvaluationMetrics;
import pt.isep.escala.escalaapi.notification.NotificationType;
import pt.isep.escala.escalaapi.notification.Notifications;
import pt.isep.escala.escalaapi.outbox.OutboxEvent;
import pt.isep.escala.escalaapi.outbox.OutboxEventRepository;
import pt.isep.escala.escalaapi.piquete.PublicationPlan.PlannedShift;
import pt.isep.escala.escalaapi.piquete.PublicationPlan.PlannedSlot;
import pt.isep.escala.escalaapi.trace.TraceId;
import pt.isep.escala.escalaapi.unavailability.UnavailabilityPeriods;
import pt.isep.escala.escalaapi.validation.InvalidRequestException;
import pt.isep.escala.escalaapi.validation.InvalidRequestException.InvalidField;
import tools.jackson.databind.ObjectMapper;

/**
 * Publishes a weekly schedule
 *
 * The supervisor attempts to publish the whole week. Shifts are checked as they are.
 * 
 * Each member on a shift is a PROPOSED assignment, and the eligibility rules in elegibilidade-worker check it. 
 * Publication is CHECKING.</li>
 * 
 * When the last answer arrives: if any member was refused (RN01, RN05, RN06), could not be checked, or a shift has
 * nobody on it, the publication is BLOCKED with those conflicts and nothing changes. 
 * 
 * Otherwise the week is applied as it was checked, the assignments are CONFIRMED, and the members are notified in the 
 * same transaction.
 *
 * A shift that is short on people, but that has at least one member can still be published
 */
@Service
class WeekPublicationService {

	// Range of time to check for unavailability periods around a shift start and end times
	private static final Duration NEARBY = Duration.ofDays(1);

	private static final String SHIFTS = "shifts";
	private static final String REQUIRED = "required";
	private static final String DUPLICATE = "duplicate";

	private static final Logger log = LoggerFactory.getLogger(WeekPublicationService.class);

	private final PiqueteRepository piquetes;
	private final AssignmentRepository assignments;
	private final WeekPublicationRepository publications;
	private final MemberRepository members;
	private final UnavailabilityPeriods unavailabilities;
	private final AvaliacaoPedidaFactory pedidos;
	private final OutboxEventRepository outbox;
	private final EvaluationMetrics metrics;
	private final Notifications notifications;
	private final PiqueteService piqueteService;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	@SuppressWarnings("java:S107")
	WeekPublicationService(PiqueteRepository piquetes, AssignmentRepository assignments,
			WeekPublicationRepository publications, MemberRepository members, UnavailabilityPeriods unavailabilities,
			AvaliacaoPedidaFactory pedidos, OutboxEventRepository outbox, EvaluationMetrics metrics,
			Notifications notifications, PiqueteService piqueteService, ObjectMapper objectMapper, Clock clock) {
		this.piquetes = piquetes;
		this.assignments = assignments;
		this.publications = publications;
		this.members = members;
		this.unavailabilities = unavailabilities;
		this.pedidos = pedidos;
		this.outbox = outbox;
		this.metrics = metrics;
		this.notifications = notifications;
		this.piqueteService = piqueteService;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	/**
	 * Publishes a week of shifts, with the members on them. The week is checked as it is
	 */
	@Transactional
	PublicationResponse publish(PublicationRequest request, UUID supervisorId) {
		IsoWeek week = week(request);
		LocalDate today = piqueteService.today();
		Map<UUID, Piquete> changeable = piquetes
				.findByDutyDateBetweenOrderByDutyDateAsc(week.monday(), week.sunday()).stream()
				.filter(piquete -> !piquete.getDutyDate().isBefore(today))
				.collect(Collectors.toMap(Piquete::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
		check(request, week, today, changeable);
		PublicationPlan plan = plan(request, changeable);

		Instant now = clock.instant();
		String weekText = request.week();
		publications.findByWeekAndStatus(weekText, WeekPublication.Status.CHECKING).forEach(older -> {
			older.superseded(now);
			assignments.findByPublicationId(older.getId()).forEach(Assignment::cancel);
		});
		WeekPublication publication = publications.save(
				new WeekPublication(UUID.randomUUID(), weekText, supervisorId, objectMapper.writeValueAsString(plan), now));

		List<Assignment> proposed = new ArrayList<>();
		for (PlannedShift shift : plan.shifts()) {
			for (PlannedSlot slot : shift.slots()) {
				if (slot.memberId() != null) {
					proposed.add(new Assignment(UUID.randomUUID(), publication.getId(), shift.piqueteId(), slot.slotId(),
							slot.memberId(), slot.role(), shift.startsAt(), shift.endsAt(), now));
				}
			}
		}
		assignments.saveAll(proposed);
		Set<UUID> managed = managed(plan);
		proposed.forEach(assignment -> queueEvaluation(assignment, plan, managed));
		if (log.isInfoEnabled()) {
			log.info("week publication requested id={} week={} shifts={} assignments={} traceId={}", publication.getId(),
					weekText, plan.shifts().size(), proposed.size(), TraceId.current());
		}
		if (proposed.isEmpty()) {
			finish(publication);
		}
		return response(publication);
	}

	@Transactional(readOnly = true)
	Optional<PublicationResponse> get(UUID id) {
		return publications.findById(id).map(this::response);
	}

	/**
	 * The worker's answer for an assignment of a publication.
	 * 
	 * The last answer of a CHECKING publication publishes or blocks the week.
	 */
	@Transactional
	boolean onEvaluated(UUID assignmentId, String result, List<String> violatedRules, String engineVersion) {
		Optional<Assignment> found = assignments.findById(assignmentId);
		if (found.isEmpty()) {
			return false;
		}
		WeekPublication publication = publications.lockById(found.get().getPublicationId()).orElseThrow();
		Assignment assignment = found.get();
		Instant now = clock.instant();
		boolean applied = "FAILED".equals(result)
				? assignment.failed(now)
				: assignment.evaluated(result, violatedRules, engineVersion, now);
		if (applied && publication.isChecking()
				&& assignments.countByPublicationIdAndEvaluationState(publication.getId(),
						Assignment.EvaluationState.PENDING) == 0) {
			finish(publication);
		}
		return applied;
	}

	// Finish the publication check. Publish the week, or block it with its conflicts.
	private void finish(WeekPublication publication) {
		PublicationPlan plan = plan(publication);
		List<Assignment> checked = assignments.findByPublicationId(publication.getId());
		Instant now = clock.instant();
		if (!conflicts(plan, checked).isEmpty()) {
			publication.blocked(now);
			checked.forEach(Assignment::cancel);
			log.info("week publication blocked id={} week={}", publication.getId(), publication.getWeek());
			return;
		}
		apply(publication, plan, checked, now);
	}

	// The week as it was checked: shifts, slots and who holds them; then who must be told.
	private void apply(WeekPublication publication, PublicationPlan plan, List<Assignment> checked, Instant now) {
		boolean firstPublication = !publications.existsByWeekAndStatus(publication.getWeek(),
				WeekPublication.Status.PUBLISHED);
		List<Assignment> previous = assignments.findByPiqueteIdInAndStatus(managed(plan), Assignment.Status.CONFIRMED);

		Map<UUID, Piquete> existing = piquetes.findAllById(managed(plan)).stream()
				.collect(Collectors.toMap(Piquete::getId, Function.identity()));
		plan.deleted().stream().map(existing::get).filter(Objects::nonNull).forEach(piquetes::delete);
		Set<UUID> held = checked.stream().map(Assignment::getSlotId).collect(Collectors.toSet());
		for (PlannedShift shift : plan.shifts()) {
			Piquete piquete = existing.get(shift.piqueteId());
			if (piquete == null) {
				piquete = new Piquete(shift.piqueteId(), shift.dutyDate());
			}
			piquete.schedule(shift.dutyDate(), shift.period(), shift.startsAt(), shift.endsAt(), shift.slots().size());
			Map<UUID, Slot> current = piquete.getSlots().stream().collect(Collectors.toMap(Slot::getId, Function.identity()));
			Set<UUID> planned = shift.slots().stream().map(PlannedSlot::slotId).collect(Collectors.toSet());
			current.values().stream().filter(slot -> !planned.contains(slot.getId())).toList().forEach(piquete::removeSlot);
			for (PlannedSlot planSlot : shift.slots()) {
				Slot slot = current.get(planSlot.slotId());
				if (slot == null) {
					slot = new Slot(planSlot.slotId(), planSlot.role(), SlotStatus.OPEN, UUID.randomUUID());
					piquete.addSlot(slot);
				}
				slot.published(held.contains(slot.getId()));
			}
			piquetes.save(piquete);
		}
		previous.forEach(Assignment::cancel);
		checked.forEach(assignment -> assignment.confirm(now));
		publication.published(now);
		notifyMembers(publication.getWeek(), firstPublication, previous, checked, Set.copyOf(plan.deleted()));
		log.info("week published id={} week={} assignments={} firstPublication={}", publication.getId(),
				publication.getWeek(), checked.size(), firstPublication);
	}

	/**
	 * The first publication of a week tells every member on it how many shifts they have
	 */
	private void notifyMembers(String week, boolean firstPublication, List<Assignment> previous,
			List<Assignment> current, Set<UUID> deleted) {
		if (firstPublication) {
			current.stream()
					.collect(Collectors.groupingBy(Assignment::getMemberId, LinkedHashMap::new,
							Collectors.mapping(Assignment::getPiqueteId, Collectors.toSet())))
					.forEach((member, shiftsOfMember) -> notifications.weeklySchedulePublished(member, week, shiftsOfMember.size()));
			return;
		}
		Map<MemberOnShift, Assignment> before = byMemberOnShift(previous);
		Map<MemberOnShift, Assignment> after = byMemberOnShift(current);
		after.forEach((key, now) -> {
			Assignment then = before.get(key);
			if (then == null) {
				notifications.shift(key.memberId(), NotificationType.ADDED_TO_SHIFT, week, key.piqueteId(),
						now.getStartsAt(), now.getEndsAt());
			} else if (!then.getStartsAt().equals(now.getStartsAt()) || !then.getEndsAt().equals(now.getEndsAt())) {
				notifications.timesChanged(key.memberId(), week, key.piqueteId(), then.getStartsAt(), then.getEndsAt(),
						now.getStartsAt(), now.getEndsAt());
			}
		});
		before.forEach((key, then) -> {
			if (!after.containsKey(key)) {
				NotificationType type = deleted.contains(key.piqueteId())
						? NotificationType.SHIFT_CANCELLED
						: NotificationType.REMOVED_FROM_SHIFT;
				notifications.shift(key.memberId(), type, week, key.piqueteId(), then.getStartsAt(), then.getEndsAt());
			}
		});
	}

	private record MemberOnShift(UUID memberId, UUID piqueteId) {
	}

	private static Map<MemberOnShift, Assignment> byMemberOnShift(List<Assignment> list) {
		Map<MemberOnShift, Assignment> map = new LinkedHashMap<>();
		list.forEach(assignment -> map.putIfAbsent(
				new MemberOnShift(assignment.getMemberId(), assignment.getPiqueteId()), assignment));
		return map;
	}

	// Shifts with nobody on them, then each member refused or not checked, in the order of the week.
	private static List<PublicationConflict> conflicts(PublicationPlan plan, List<Assignment> checked) {
		Map<UUID, PlannedShift> shiftById = plan.shifts().stream()
				.collect(Collectors.toMap(PlannedShift::piqueteId, Function.identity()));
		List<PublicationConflict> conflicts = new ArrayList<>();
		plan.shifts().stream().filter(PlannedShift::empty).forEach(shift -> conflicts.add(new PublicationConflict(
				PublicationConflict.Type.EMPTY_SHIFT, shift.key(), shift.piqueteId(), shift.dutyDate(), null, null, null)));
		checked.stream()
				.filter(Assignment::isRefused)
				.sorted(Comparator.comparing(Assignment::getStartsAt))
				.forEach(assignment -> {
					PlannedShift shift = shiftById.get(assignment.getPiqueteId());
					boolean failed = assignment.getEvaluationState() == Assignment.EvaluationState.FAILED;
					conflicts.add(new PublicationConflict(
							failed ? PublicationConflict.Type.NOT_CHECKED : PublicationConflict.Type.INELIGIBLE,
							shift.key(), shift.piqueteId(), shift.dutyDate(), assignment.getMemberId(), assignment.getRole(),
							failed ? List.of() : assignment.getViolatedRules()));
				});
		return conflicts;
	}

	private PublicationResponse response(WeekPublication publication) {
		List<PublicationConflict> conflicts = publication.getStatus() == WeekPublication.Status.BLOCKED
				? conflicts(plan(publication), assignments.findByPublicationId(publication.getId()))
				: List.of();
		return new PublicationResponse(publication.getId(), publication.getWeek(), publication.getStatus(), conflicts);
	}

	/**
	 * One AvaliacaoPedida through the outbox, in the same transaction. The candidate's other shifts are the others of
	 * this week as sent, and their published shifts outside it; their unavailabilities are those near this shift.
	 */
	private void queueEvaluation(Assignment assignment, PublicationPlan plan, Set<UUID> managed) {
		Member member = members.findById(assignment.getMemberId()).orElseThrow();
		Instant from = assignment.getStartsAt().minus(NEARBY);
		Instant to = assignment.getEndsAt().plus(NEARBY);
		List<AvaliacaoPedidaFactory.Period> others = new ArrayList<>();
		plan.shifts().stream()
				.filter(shift -> !shift.piqueteId().equals(assignment.getPiqueteId()))
				.filter(shift -> shift.slots().stream().anyMatch(slot -> assignment.getMemberId().equals(slot.memberId())))
				.forEach(shift -> others.add(new AvaliacaoPedidaFactory.Period(shift.startsAt(), shift.endsAt())));
		assignments.findByMemberIdAndStatusAndStartsAtBeforeAndEndsAtAfter(assignment.getMemberId(),
				Assignment.Status.CONFIRMED, to, from).stream()
				.filter(confirmed -> !managed.contains(confirmed.getPiqueteId()))
				.forEach(confirmed -> others.add(
						new AvaliacaoPedidaFactory.Period(confirmed.getStartsAt(), confirmed.getEndsAt())));
		List<AvaliacaoPedidaFactory.Period> away = unavailabilities.overlapping(member.getId(), from, to).stream()
				.map(period -> new AvaliacaoPedidaFactory.Period(period.startsAt(), period.endsAt()))
				.toList();
		UUID eventId = UUID.randomUUID();
		outbox.save(new OutboxEvent(eventId, PiqueteService.ROUTING_AVALIACAO_PEDIDA,
				pedidos.payload(eventId, AvaliacaoPedidaFactory.PURPOSE, assignment.getId(), assignment.getSlotId(),
						assignment.getStartsAt(), assignment.getEndsAt(), member, away, others),
				Instant.now()));
		metrics.requested(AvaliacaoPedidaFactory.PURPOSE);
		if (log.isInfoEnabled()) {
			log.info("AvaliacaoPedida queued eventId={} assignmentId={} traceId={}", eventId, assignment.getId(),
					TraceId.current());
		}
	}

	private static IsoWeek week(PublicationRequest request) {
		if (request == null || request.week() == null) {
			throw new InvalidRequestException(List.of(new InvalidField("week", REQUIRED)));
		}
		try {
			return IsoWeek.parse(request.week());
		} catch (IllegalArgumentException e) {
			throw new InvalidRequestException(List.of(new InvalidField("week", "invalid_week")));
		}
	}

	/**
	 * Each shift as a shift is checked (SCRUM-65), on a day of this week
	 */
	private void check(PublicationRequest request, IsoWeek week, LocalDate today, Map<UUID, Piquete> changeable) {
		if (request.shifts() == null) {
			throw new InvalidRequestException(List.of(new InvalidField(SHIFTS, REQUIRED)));
		}
		Set<UUID> existingMembers = members.findAllById(assignedMemberIds(request)).stream()
				.map(Member::getId)
				.collect(Collectors.toSet());
		List<InvalidField> errors = new ArrayList<>();
		Set<String> keys = new HashSet<>();
		Set<UUID> ids = new HashSet<>();
		for (int i = 0; i < request.shifts().size(); i++) {
			PublishedShift shift = request.shifts().get(i);
			if (shift == null) {
				errors.add(new InvalidField(SHIFTS + "[" + i + "]", REQUIRED));
				continue;
			}
			String prefix = SHIFTS + "[" + i + "].";
			keyError(shift, prefix, keys).ifPresent(errors::add);
			List<InvalidField> shiftErrors = PiqueteService.shiftErrors(shift.asPiqueteRequest(), prefix, today);
			errors.addAll(shiftErrors);
			weekError(shift, prefix, week).ifPresent(errors::add);
			idError(shift, prefix, changeable, ids).ifPresent(errors::add);
			boolean crewValid = shiftErrors.stream().noneMatch(error -> error.field().equals(prefix + "crew"));
			errors.addAll(assignedErrors(shift, prefix, crewValid, existingMembers));
		}
		if (!errors.isEmpty()) {
			throw new InvalidRequestException(errors);
		}
	}

	// Every member sent on any shift, to look them all up at once
	private static Set<UUID> assignedMemberIds(PublicationRequest request) {
		return request.shifts().stream()
				.filter(Objects::nonNull)
				.map(PublishedShift::assigned)
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.filter(Objects::nonNull)
				.map(AssignedMember::memberId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	// A key is required and unique in the week
	private static Optional<InvalidField> keyError(PublishedShift shift, String prefix, Set<String> keys) {
		if (shift.key() == null || shift.key().isBlank()) {
			return Optional.of(new InvalidField(prefix + "key", REQUIRED));
		}
		return keys.add(shift.key()) ? Optional.empty() : Optional.of(new InvalidField(prefix + "key", DUPLICATE));
	}

	// The shift's day is in the week being published
	private static Optional<InvalidField> weekError(PublishedShift shift, String prefix, IsoWeek week) {
		LocalDate day = shift.dutyDate();
		if (day != null && (day.isBefore(week.monday()) || day.isAfter(week.sunday()))) {
			return Optional.of(new InvalidField(prefix + "dutyDate", "not_in_week"));
		}
		return Optional.empty();
	}

	// An existing shift is one of this week that can still change, sent once
	private static Optional<InvalidField> idError(PublishedShift shift, String prefix, Map<UUID, Piquete> changeable,
			Set<UUID> ids) {
		if (shift.id() == null) {
			return Optional.empty();
		}
		if (!changeable.containsKey(shift.id())) {
			return Optional.of(new InvalidField(prefix + "id", "unknown_shift"));
		}
		return ids.add(shift.id()) ? Optional.empty() : Optional.of(new InvalidField(prefix + "id", DUPLICATE));
	}

	private static List<InvalidField> assignedErrors(PublishedShift shift, String prefix, boolean crewValid,
			Set<UUID> existingMembers) {
		List<InvalidField> errors = new ArrayList<>();
		if (shift.assigned() == null) {
			return errors;
		}
		Map<SlotRole, Integer> needed = crewValid ? PiqueteService.crew(shift.asPiqueteRequest()) : Map.of();
		Map<SlotRole, Integer> onRole = new EnumMap<>(SlotRole.class);
		Set<UUID> onShift = new HashSet<>();
		for (int j = 0; j < shift.assigned().size(); j++) {
			String field = prefix + "assigned[" + j + "]";
			AssignedMember assigned = shift.assigned().get(j);
			if (assigned == null || assigned.role() == null || assigned.memberId() == null) {
				errors.add(new InvalidField(field, "invalid"));
				continue;
			}
			if (!existingMembers.contains(assigned.memberId())) {
				errors.add(new InvalidField(field + ".memberId", "unknown_member"));
			} else if (!onShift.add(assigned.memberId())) {
				errors.add(new InvalidField(field + ".memberId", DUPLICATE));
			}
			int count = onRole.merge(assigned.role(), 1, Integer::sum);
			if (crewValid && count > needed.getOrDefault(assigned.role(), 0)) {
				errors.add(new InvalidField(field + ".role", "too_many"));
			}
		}
		return errors;
	}

	/**
	 * The week as it will be applied: a slot for each person of each role, the members in the order sent
	 */
	private static PublicationPlan plan(PublicationRequest request, Map<UUID, Piquete> changeable) {
		List<PlannedShift> planned = new ArrayList<>();
		Set<UUID> kept = new HashSet<>();
		for (PublishedShift shift : request.shifts()) {
			Piquete existing = shift.id() == null ? null : changeable.get(shift.id());
			UUID piqueteId = existing == null ? UUID.randomUUID() : existing.getId();
			kept.add(piqueteId);
			planned.add(new PlannedShift(shift.key(), piqueteId, existing == null, shift.dutyDate(), shift.period(),
					shift.startsAt().toInstant(), shift.endsAt().toInstant(), plannedSlots(shift, existing)));
		}
		List<UUID> deleted = changeable.keySet().stream().filter(id -> !kept.contains(id)).toList();
		return new PublicationPlan(planned, deleted);
	}

	// A slot for each person of each role: the shift's own slots first, then new ones; the members in the order sent
	private static List<PlannedSlot> plannedSlots(PublishedShift shift, Piquete existing) {
		Map<SlotRole, Integer> crew = PiqueteService.crew(shift.asPiqueteRequest());
		List<PlannedSlot> slots = new ArrayList<>();
		for (SlotRole role : SlotRole.values()) {
			int count = crew.getOrDefault(role, 0);
			List<UUID> reused = reusedSlots(existing, role, count);
			List<UUID> onRole = membersOn(shift, role);
			for (int i = 0; i < count; i++) {
				UUID slotId = i < reused.size() ? reused.get(i) : UUID.randomUUID();
				slots.add(new PlannedSlot(slotId, role, i < onRole.size() ? onRole.get(i) : null));
			}
		}
		return slots;
	}

	// The existing shift's slots of that role, as many as are still needed
	private static List<UUID> reusedSlots(Piquete existing, SlotRole role, int count) {
		if (existing == null) {
			return List.of();
		}
		return existing.getSlots().stream()
				.filter(slot -> slot.getRole() == role)
				.map(Slot::getId)
				.sorted()
				.limit(count)
				.toList();
	}

	// The members sent for that role, in the order sent
	private static List<UUID> membersOn(PublishedShift shift, SlotRole role) {
		if (shift.assigned() == null) {
			return List.of();
		}
		return shift.assigned().stream()
				.filter(assigned -> assigned.role() == role)
				.map(AssignedMember::memberId)
				.toList();
	}

	private PublicationPlan plan(WeekPublication publication) {
		return objectMapper.readValue(publication.getPlan(), PublicationPlan.class);
	}

	// The shifts this publication decides about: those it keeps or creates, and those it deletes.
	private static Set<UUID> managed(PublicationPlan plan) {
		Set<UUID> ids = new HashSet<>(plan.deleted());
		plan.shifts().forEach(shift -> ids.add(shift.piqueteId()));
		return ids;
	}
}
