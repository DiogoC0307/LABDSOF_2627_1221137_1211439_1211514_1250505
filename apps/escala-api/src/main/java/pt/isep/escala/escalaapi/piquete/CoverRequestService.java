package pt.isep.escala.escalaapi.piquete;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.member.AccessRole;
import pt.isep.escala.escalaapi.member.Member;
import pt.isep.escala.escalaapi.member.MemberRepository;
import pt.isep.escala.escalaapi.member.ServiceFunction;
import pt.isep.escala.escalaapi.metrics.EvaluationMetrics;
import pt.isep.escala.escalaapi.notification.NotificationType;
import pt.isep.escala.escalaapi.notification.Notifications;
import pt.isep.escala.escalaapi.outbox.OutboxEvent;
import pt.isep.escala.escalaapi.outbox.OutboxEventRepository;
import pt.isep.escala.escalaapi.trace.TraceId;
import pt.isep.escala.escalaapi.unavailability.UnavailabilityPeriods;

/**
 * Cover requests (SCRUM-72). When a slot opens, every other member with its function is checked by the eligibility
 * rules in elegibilidade-worker (AvaliacaoPedida, purpose COVER; ADR 0007), with their own unavailability and shifts
 * near it. Only the members the rules accept are asked: "free and qualified" is the slot's function plus what the rules
 * check, so no rule is repeated here. The supervisor is told when the slot opens, when nobody is free and qualified,
 * and when nobody accepts in time.
 */
@Service
class CoverRequestService {

	// The unavailability and the other shifts sent with each check, around the shift (as for SCRUM-171)
	private static final Duration NEARBY = Duration.ofDays(1);

	// The shift of a piquete without start and end (the walking skeleton): its slot's start and a day
	private static final Duration WHOLE_DAY = Duration.ofDays(1);

	private static final Logger log = LoggerFactory.getLogger(CoverRequestService.class);

	private final CoverRequestRepository requests;
	private final CoverOfferRepository offers;
	private final SlotRepository slots;
	private final AssignmentRepository assignments;
	private final MemberRepository members;
	private final UnavailabilityPeriods unavailabilities;
	private final AvaliacaoPedidaFactory pedidos;
	private final OutboxEventRepository outbox;
	private final Notifications notifications;
	private final EvaluationMetrics metrics;
	private final Clock clock;
	private final CoverDeadline deadline;

	@SuppressWarnings("java:S107") // The requests, the slots and members, their periods, the broker and the alerts
	CoverRequestService(CoverRequestRepository requests, CoverOfferRepository offers, SlotRepository slots,
			AssignmentRepository assignments, MemberRepository members, UnavailabilityPeriods unavailabilities,
			AvaliacaoPedidaFactory pedidos, OutboxEventRepository outbox, Notifications notifications,
			EvaluationMetrics metrics, Clock clock,
			@Value("${escala.cover.no-acceptance-after:PT30M}") Duration noAcceptanceAfter,
			@Value("${escala.cover.alert-before-shift:PT2H}") Duration alertBeforeShift) {
		this.requests = requests;
		this.offers = offers;
		this.slots = slots;
		this.assignments = assignments;
		this.members = members;
		this.unavailabilities = unavailabilities;
		this.pedidos = pedidos;
		this.outbox = outbox;
		this.notifications = notifications;
		this.metrics = metrics;
		this.clock = clock;
		this.deadline = new CoverDeadline(noAcceptanceAfter, alertBeforeShift);
	}

	/**
	 * Opens a slot whose member can't come (SCRUM-71): their assignment is cancelled, a cover request is made, the
	 * supervisor is told, and every other member with the slot's function is checked. A slot that already has an open
	 * request keeps it, so a second "I can't come" asks nobody twice. Runs in the caller's transaction.
	 *
	 * @param openedAt when the member said they can't come, or when the deadline to answer passed
	 */
	@Transactional
	UUID open(UUID slotId, CoverRequest.Reason reason, Instant openedAt) {
		Slot slot = slots.findById(slotId).orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
		return requests.findBySlotIdAndStatus(slotId, CoverRequest.Status.OPEN).map(CoverRequest::getId)
				.orElseGet(() -> create(slot, reason, openedAt));
	}

	private UUID create(Slot slot, CoverRequest.Reason reason, Instant openedAt) {
		Piquete piquete = slot.getPiquete();
		Instant start = piquete.getStartsAt() != null ? piquete.getStartsAt() : slot.getShiftStartsAt();
		Instant end = piquete.getEndsAt() != null ? piquete.getEndsAt() : start.plus(WHOLE_DAY);
		List<Assignment> held = assignments.findBySlotIdAndStatus(slot.getId(), Assignment.Status.CONFIRMED);
		held.forEach(Assignment::cancel);
		UUID vacated = held.isEmpty() ? null : held.get(0).getMemberId();
		CoverRequest request = requests.saveAndFlush(new CoverRequest(UUID.randomUUID(), slot.getId(), piquete.getId(),
				slot.getRole(), start, end, vacated, reason, openedAt));
		tellSupervisors(request, NotificationType.SLOT_OPENED, null);

		ServiceFunction function = ServiceFunction.valueOf(slot.getRole().name());
		List<Member> candidates = members.findAll().stream()
				.filter(member -> member.getFunctions().contains(function))
				.filter(member -> !member.getId().equals(vacated))
				.toList();
		candidates.forEach(member -> check(request, member));
		if (log.isInfoEnabled()) {
			log.info("cover request opened id={} slotId={} role={} reason={} candidates={} traceId={}", request.getId(),
					slot.getId(), slot.getRole(), reason, candidates.size(), TraceId.current());
		}
		if (candidates.isEmpty()) {
			escalate(request, CoverRequest.Escalation.NO_CANDIDATES);
		}
		return request.getId();
	}

	/** One AvaliacaoPedida (purpose COVER) for the member, through the outbox, in the same transaction as the offer. */
	private void check(CoverRequest request, Member member) {
		CoverOffer offer = offers.save(new CoverOffer(UUID.randomUUID(), request.getId(), member.getId(), clock.instant()));
		Instant from = request.getStartsAt().minus(NEARBY);
		Instant to = request.getEndsAt().plus(NEARBY);
		List<AvaliacaoPedidaFactory.Period> away = unavailabilities.overlapping(member.getId(), from, to).stream()
				.map(period -> new AvaliacaoPedidaFactory.Period(period.startsAt(), period.endsAt()))
				.toList();
		List<AvaliacaoPedidaFactory.Period> onDuty = assignments
				.findByMemberIdAndStatusAndStartsAtBeforeAndEndsAtAfter(member.getId(), Assignment.Status.CONFIRMED, to,
						from)
				.stream()
				.map(other -> new AvaliacaoPedidaFactory.Period(other.getStartsAt(), other.getEndsAt()))
				.toList();
		UUID eventId = UUID.randomUUID();
		outbox.save(new OutboxEvent(eventId, PiqueteService.ROUTING_AVALIACAO_PEDIDA,
				pedidos.payload(eventId, AvaliacaoPedidaFactory.PURPOSE_COVER, offer.getId(), request.getSlotId(),
						request.getStartsAt(), request.getEndsAt(), member, away, onDuty),
				Instant.now()));
		metrics.requested(AvaliacaoPedidaFactory.PURPOSE_COVER);
	}

	/**
	 * The worker's answer for a cover offer (from AvaliacaoConcluida). The member is asked when the rules accept them;
	 * when the last answer is back and nobody was asked, the supervisor is alerted at once (SCRUM-72 AC3). False when
	 * the assignmentId is not a cover offer, or it was already answered.
	 */
	@Transactional
	boolean onEvaluated(UUID offerId, String result, List<String> violatedRules, String engineVersion) {
		return offers.findById(offerId).map(offer -> {
			CoverRequest request = requests.findById(offer.getCoverRequestId()).orElseThrow();
			Instant now = clock.instant();
			boolean applied = "FAILED".equals(result)
					? offer.failed(now)
					: offer.evaluated(result, String.join(",", violatedRules), engineVersion, now, request.isOpen());
			if (!applied) {
				return false;
			}
			if (offer.isOffered()) {
				notifications.cover(offer.getMemberId(), NotificationType.COVER_REQUESTED, week(request),
						request.getPiqueteId(), request.getStartsAt(), request.getEndsAt(), request.getId(),
						request.getRole().name(), null);
				log.info("cover offered coverRequestId={} offerId={}", request.getId(), offer.getId());
			}
			List<CoverOffer> all = offers.findByCoverRequestId(request.getId());
			if (all.stream().noneMatch(CoverOffer::isChecking) && all.stream().noneMatch(CoverOffer::isOffered)) {
				escalate(request, CoverRequest.Escalation.NO_CANDIDATES);
			}
			return true;
		}).orElse(false);
	}

	/**
	 * Alerts the supervisor when nobody accepted a cover request in time (SCRUM-72 AC4), and closes the requests whose
	 * shift has started.
	 */
	@Scheduled(fixedDelayString = "${escala.cover.check-ms:60000}", initialDelayString = "${escala.cover.check-ms:60000}")
	@Transactional
	public void checkDeadlines() {
		Instant now = clock.instant();
		for (CoverRequest request : requests.findByStatus(CoverRequest.Status.OPEN)) {
			if (!now.isBefore(request.getStartsAt())) {
				request.close();
				log.info("cover request closed, the shift started id={}", request.getId());
			} else if (request.getEscalation() == null
					&& deadline.hasPassed(request.getOpenedAt(), request.getStartsAt(), now)) {
				escalate(request, CoverRequest.Escalation.NO_ACCEPTANCE);
			}
		}
	}

	private void escalate(CoverRequest request, CoverRequest.Escalation why) {
		if (request.escalate(why, clock.instant())) {
			tellSupervisors(request, NotificationType.COVER_ESCALATED, why.name());
			log.info("cover request escalated to the supervisor id={} escalation={}", request.getId(), why);
		}
	}

	private void tellSupervisors(CoverRequest request, NotificationType type, String reason) {
		members.findByActiveTrue().stream()
				.filter(member -> member.getAccessRole() == AccessRole.SUPERVISOR)
				.forEach(supervisor -> notifications.cover(supervisor.getId(), type, week(request),
						request.getPiqueteId(), request.getStartsAt(), request.getEndsAt(), request.getId(),
						request.getRole().name(), reason));
	}

	// The ISO week of the shift, as the notifications carry it ("2026-W49"), on the Lisbon calendar
	private static String week(CoverRequest request) {
		LocalDate day = request.getStartsAt().atZone(PiqueteService.LISBON).toLocalDate();
		return "%d-W%02d".formatted(day.get(IsoFields.WEEK_BASED_YEAR), day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
	}
}
