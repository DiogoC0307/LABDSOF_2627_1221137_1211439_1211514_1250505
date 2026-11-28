package pt.isep.escala.escalaapi.piquete;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.member.Member;
import pt.isep.escala.escalaapi.member.MemberRepository;
import pt.isep.escala.escalaapi.member.ServiceFunction;
import pt.isep.escala.escalaapi.metrics.EvaluationMetrics;
import pt.isep.escala.escalaapi.outbox.OutboxEvent;
import pt.isep.escala.escalaapi.outbox.OutboxEventRepository;
import pt.isep.escala.escalaapi.trace.TraceId;
import pt.isep.escala.escalaapi.unavailability.UnavailabilityPeriods;
import pt.isep.escala.escalaapi.validation.InvalidRequestException;
import pt.isep.escala.escalaapi.validation.InvalidRequestException.InvalidField;

/**
 * Who can take a role on a shift.
 * 
 * Every member with that function is checked by the eligibility rules. 
 * A member the rules refuse is never offered.
 */
@Service
class CandidateCheckService {

	// Defines range of period to check for unavailabilities for a given shift (+/- 1 day from the shift's start and end times)
	private static final Duration NEARBY = Duration.ofDays(1);

	// Duration a eligibility check is kept in the database before being deleted
	static final Duration KEPT_FOR = Duration.ofDays(1);

	private static final String REQUIRED = "required";
	private static final String ENDS_AT = "endsAt";

	private static final Logger log = LoggerFactory.getLogger(CandidateCheckService.class);

	private final CandidateCheckRepository checks;
	private final MemberRepository members;
	private final UnavailabilityPeriods unavailabilities;
	private final AvaliacaoPedidaFactory pedidos;
	private final OutboxEventRepository outbox;
	private final EvaluationMetrics metrics;
	private final Clock clock;

	@SuppressWarnings("java:S107") // The checks, the members, their periods and the broker
	CandidateCheckService(CandidateCheckRepository checks, MemberRepository members,
			UnavailabilityPeriods unavailabilities, AvaliacaoPedidaFactory pedidos, OutboxEventRepository outbox,
			EvaluationMetrics metrics, Clock clock) {
		this.checks = checks;
		this.members = members;
		this.unavailabilities = unavailabilities;
		this.pedidos = pedidos;
		this.outbox = outbox;
		this.metrics = metrics;
		this.clock = clock;
	}

	/**
	 * Queues one AvaliacaoPedida (purpose CANDIDATE) per member with the function, through the outbox, in the same
	 * transaction as the checks.
	 */
	@Transactional
	CandidateCheckResponse start(CandidateCheckRequest request) {
		check(request);
		Instant start = request.startsAt().toInstant();
		Instant end = request.endsAt().toInstant();
		ServiceFunction function = ServiceFunction.valueOf(request.role().name());
		UUID requestId = UUID.randomUUID();
		Instant now = clock.instant();
		List<Member> withRole = members.findAll().stream()
				.filter(member -> member.getFunctions().contains(function))
				.toList();
		for (Member member : withRole) {
			CandidateCheck check = checks.save(
					new CandidateCheck(UUID.randomUUID(), requestId, member.getId(), request.role(), start, end, now));
			queueEvaluation(check, member, request);
		}
		if (log.isInfoEnabled()) {
			log.info("candidate check requested id={} role={} members={} traceId={}", requestId, request.role(),
					withRole.size(), TraceId.current());
		}
		return response(requestId, checks.findByRequestId(requestId));
	}

	@Transactional(readOnly = true)
	Optional<CandidateCheckResponse> get(UUID requestId) {
		List<CandidateCheck> found = checks.findByRequestId(requestId);
		return found.isEmpty() ? Optional.empty() : Optional.of(response(requestId, found));
	}

	/**
	 * The worker's answer for a candidate check (from AvaliacaoConcluida). 
	 * 
	 * False when the assignmentId is not one, or it was already answered.
	 */
	@Transactional
	boolean onEvaluated(UUID checkId, String result, List<String> violatedRules, String engineVersion) {
		return checks.findById(checkId).map(check -> "FAILED".equals(result)
				? check.failed(clock.instant())
				: check.evaluated(result, violatedRules, engineVersion, clock.instant())).orElse(false);
	}

	// Deletes old candidate checks from database
	@Scheduled(cron = "0 17 * * * *")
	@Transactional
	void deleteOldChecks() {
		int deleted = checks.deleteMadeBefore(clock.instant().minus(KEPT_FOR));
		if (deleted > 0) {
			log.info("candidate checks deleted count={}", deleted);
		}
	}

	private void queueEvaluation(CandidateCheck check, Member member, CandidateCheckRequest request) {
		Instant from = check.getStartsAt().minus(NEARBY);
		Instant to = check.getEndsAt().plus(NEARBY);
		List<AvaliacaoPedidaFactory.Period> others = request.otherShifts() == null ? List.of()
				: request.otherShifts().stream()
						.filter(other -> member.getId().equals(other.memberId()))
						.map(other -> new AvaliacaoPedidaFactory.Period(other.startsAt().toInstant(),
								other.endsAt().toInstant()))
						.toList();
		List<AvaliacaoPedidaFactory.Period> away = unavailabilities.overlapping(member.getId(), from, to).stream()
				.map(period -> new AvaliacaoPedidaFactory.Period(period.startsAt(), period.endsAt()))
				.toList();
		UUID eventId = UUID.randomUUID();
		outbox.save(new OutboxEvent(eventId, PiqueteService.ROUTING_AVALIACAO_PEDIDA,
				pedidos.payload(eventId, AvaliacaoPedidaFactory.PURPOSE_CANDIDATE, check.getId(), check.getRequestId(),
						check.getStartsAt(), check.getEndsAt(), member, away, others),
				Instant.now()));
		metrics.requested(AvaliacaoPedidaFactory.PURPOSE_CANDIDATE);
	}

	private static void check(CandidateCheckRequest request) {
		if (request == null) {
			throw new InvalidRequestException(List.of(new InvalidField("startsAt", REQUIRED)));
		}
		List<InvalidField> errors = new ArrayList<>();
		if (request.role() == null) {
			errors.add(new InvalidField("role", REQUIRED));
		}
		if (request.startsAt() == null) {
			errors.add(new InvalidField("startsAt", REQUIRED));
		}
		if (request.endsAt() == null) {
			errors.add(new InvalidField(ENDS_AT, REQUIRED));
		} else if (request.startsAt() != null) {
			Duration length = Duration.between(request.startsAt(), request.endsAt());
			if (length.isZero() || length.isNegative()) {
				errors.add(new InvalidField(ENDS_AT, "ends_before_start"));
			} else if (length.compareTo(PiqueteService.LONGEST_SHIFT) > 0) {
				errors.add(new InvalidField(ENDS_AT, "too_long"));
			}
		}
		List<OtherShift> others = request.otherShifts() == null ? List.of() : request.otherShifts();
		for (int i = 0; i < others.size(); i++) {
			OtherShift other = others.get(i);
			if (other == null || other.memberId() == null || other.startsAt() == null || other.endsAt() == null
					|| !other.endsAt().isAfter(other.startsAt())) {
				errors.add(new InvalidField("otherShifts[" + i + "]", "invalid"));
			}
		}
		if (!errors.isEmpty()) {
			throw new InvalidRequestException(errors);
		}
	}

	private static CandidateCheckResponse response(UUID requestId, List<CandidateCheck> found) {
		List<CandidateResult> candidates = found.stream()
				.sorted(Comparator.comparing(CandidateCheck::getMemberId))
				.map(CandidateCheckService::result)
				.toList();
		boolean checking = found.stream().anyMatch(CandidateCheck::isPending);
		return new CandidateCheckResponse(requestId,
				checking ? CandidateCheckResponse.Status.CHECKING : CandidateCheckResponse.Status.DONE, candidates);
	}

	private static CandidateResult result(CandidateCheck check) {
		return switch (check.getEvaluationState()) {
			case PENDING -> new CandidateResult(check.getMemberId(), CandidateResult.Result.PENDING, List.of());
			case FAILED -> new CandidateResult(check.getMemberId(), CandidateResult.Result.NOT_CHECKED, List.of());
			case EVALUATED -> CandidateCheck.ELIGIBLE.equals(check.getResult())
					? new CandidateResult(check.getMemberId(), CandidateResult.Result.ELIGIBLE, List.of())
					: new CandidateResult(check.getMemberId(), CandidateResult.Result.INELIGIBLE,
							check.getViolatedRules());
		};
	}
}
