package pt.isep.escala.escalaapi.unavailability;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.outbox.OutboxEvent;
import pt.isep.escala.escalaapi.outbox.OutboxEventRepository;
import pt.isep.escala.escalaapi.unavailability.MessageRefusals.AiOff;
import pt.isep.escala.escalaapi.unavailability.MessageRefusals.NotConfirmable;
import pt.isep.escala.escalaapi.unavailability.MessageRefusals.TooManyMessages;
import pt.isep.escala.escalaapi.unavailability.PatternExpansion.Interval;
import pt.isep.escala.escalaapi.validation.InvalidRequestException;
import pt.isep.escala.escalaapi.validation.InvalidRequestException.InvalidField;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * "When I can't", in my own words (SCRUM-75). The member always comes from the token (T-03), and another member's
 * message is not found. The free text goes to the worker through the outbox and is deleted, with its outbox row, when
 * the member confirms or discards, or after 24 hours (RNF09). The free text is never logged. With AI off
 * (AI_ENABLED, the same flag as the worker's), a text is refused before anything is stored (SCRUM-89).
 */
@Service
class UnavailabilityMessageService {

	static final int MAX_TEXT = 500;
	static final int MAX_PERIODS = 10;

	private static final Logger log = LoggerFactory.getLogger(UnavailabilityMessageService.class);

	private static final String TEXT = "text";
	private static final String PERIODS = "periods";
	private static final Duration WINDOW = Duration.ofHours(1);
	private static final TypeReference<List<ProposedPeriod>> PERIOD_LIST = new TypeReference<>() {
	};

	private final UnavailabilityMessageRepository messages;
	private final UnavailabilityService unavailability;
	private final OutboxEventRepository outbox;
	private final ExtracaoPedidaFactory pedidos;
	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final int perHour;
	private final boolean aiEnabled;

	UnavailabilityMessageService(UnavailabilityMessageRepository messages, UnavailabilityService unavailability,
			OutboxEventRepository outbox, ExtracaoPedidaFactory pedidos, ObjectMapper objectMapper, Clock clock,
			@Value("${escala.unavailability-messages.per-hour:10}") int perHour,
			@Value("${escala.ai.enabled:false}") boolean aiEnabled) {
		this.messages = messages;
		this.unavailability = unavailability;
		this.outbox = outbox;
		this.pedidos = pedidos;
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.perHour = perHour;
		this.aiEnabled = aiEnabled;
	}

	boolean aiEnabled() {
		return aiEnabled;
	}

	@Transactional
	MessageResponse send(UUID memberId, MessageRequest request) {
		if (!aiEnabled) {
			log.info("unavailability message refused, AI off memberId={}", memberId);
			throw new AiOff();
		}
		String text = request == null ? null : request.text();
		if (text == null || text.isBlank()) {
			throw new InvalidRequestException(List.of(new InvalidField(TEXT, "required")));
		}
		if (text.length() > MAX_TEXT) {
			throw new InvalidRequestException(List.of(new InvalidField(TEXT, "too_long")));
		}
		Instant now = clock.instant();
		Instant windowStart = now.minus(WINDOW);
		if (messages.countByMemberIdAndCreatedAtAfter(memberId, windowStart) >= perHour) {
			Instant oldest = messages.findFirstByMemberIdAndCreatedAtAfterOrderByCreatedAtAsc(memberId, windowStart)
					.map(UnavailabilityMessage::getCreatedAt).orElse(now);
			log.info("unavailability message refused, too many memberId={}", memberId);
			throw new TooManyMessages(Math.max(1, Duration.between(now, oldest.plus(WINDOW)).toSeconds()));
		}
		LocalDate referenceDate = LocalDate.ofInstant(now, PatternExpansion.LISBON);
		UnavailabilityMessage message = messages.save(
				new UnavailabilityMessage(memberId, text, referenceDate, UUID.randomUUID(), now));
		outbox.save(new OutboxEvent(message.getRequestEventId(), ExtracaoPedidaFactory.ROUTING_KEY,
				pedidos.payload(message, now), now));
		log.info("unavailability message received id={} memberId={}", message.getId(), memberId);
		return view(message);
	}

	@Transactional(readOnly = true)
	Optional<MessageResponse> get(UUID memberId, UUID id) {
		return messages.findByIdAndMemberId(id, memberId).map(this::view);
	}

	/**
	 * Saves the periods the member confirms, expanded in UTC, and deletes the free text in the same transaction
	 * (AC4). Empty when there is no such message for this member.
	 */
	@Transactional
	Optional<List<UnavailabilityResponse>> confirm(UUID memberId, UUID id, ConfirmRequest request) {
		Optional<UnavailabilityMessage> found = messages.findByIdAndMemberId(id, memberId);
		if (found.isEmpty()) {
			return Optional.empty();
		}
		UnavailabilityMessage message = found.get();
		if (!message.isProposed()) {
			throw new NotConfirmable();
		}
		Instant now = clock.instant();
		List<Interval> periods = check(request == null ? null : request.periods(), now);
		List<UnavailabilityResponse> saved = unavailability.declareFromText(memberId, periods, now);
		message.confirm(now);
		outbox.deleteById(message.getRequestEventId());
		log.info("unavailability message confirmed id={} memberId={} periods={}", id, memberId, saved.size());
		return Optional.of(saved);
	}

	/** Deletes the free text and the proposal is not used; false when there is no such message for this member. */
	@Transactional
	boolean discard(UUID memberId, UUID id) {
		return messages.findByIdAndMemberId(id, memberId).map(message -> {
			if (message.isOpen()) {
				message.discard(clock.instant());
				outbox.deleteById(message.getRequestEventId());
				log.info("unavailability message discarded id={} memberId={}", id, memberId);
			}
			return true;
		}).orElse(false);
	}

	/**
	 * The confirmed periods, read, checked against the window (as the worker does: nothing that has ended, nothing
	 * more than 6 months ahead) and expanded. A period running now keeps only the parts that have not ended.
	 */
	private List<Interval> check(List<ProposedPeriod> periods, Instant now) {
		List<InvalidField> errors = new ArrayList<>();
		if (periods == null || periods.isEmpty()) {
			throw new InvalidRequestException(List.of(new InvalidField(PERIODS, PeriodPatterns.REQUIRED)));
		}
		if (periods.size() > MAX_PERIODS) {
			throw new InvalidRequestException(List.of(new InvalidField(PERIODS, "too_many")));
		}
		LocalDate limit = LocalDate.ofInstant(now, PatternExpansion.LISBON).plusMonths(UnavailabilityService.MONTHS_AHEAD);
		List<Interval> kept = new ArrayList<>();
		for (int i = 0; i < periods.size(); i++) {
			String prefix = PERIODS + "[" + i + "]";
			PeriodPattern pattern = PeriodPatterns.read(periods.get(i), prefix, errors);
			if (pattern == null) {
				continue;
			}
			if (pattern.startDate().isAfter(limit)) {
				errors.add(new InvalidField(prefix + ".startDate", "too_far_ahead"));
			} else if (pattern.endDate().isAfter(limit)) {
				errors.add(new InvalidField(prefix + ".endDate", "too_far_ahead"));
			} else {
				List<Interval> days = PatternExpansion.days(pattern);
				List<Interval> notEnded = days.stream().filter(day -> day.end().isAfter(now)).toList();
				if (days.isEmpty()) {
					errors.add(new InvalidField(prefix + ".weekdays", "no_days"));
				} else if (notEnded.isEmpty()) {
					errors.add(new InvalidField(prefix + ".endDate", "in_the_past"));
				}
				kept.addAll(notEnded);
			}
		}
		if (!errors.isEmpty()) {
			throw new InvalidRequestException(errors);
		}
		return PatternExpansion.join(kept);
	}

	private MessageResponse view(UnavailabilityMessage message) {
		List<ProposedPeriod> periods = message.getProposal() == null
				? List.of()
				: objectMapper.readValue(message.getProposal(), PERIOD_LIST);
		return new MessageResponse(message.getId(), message.getStatus(), message.getReferenceDate(), message.getMethod(),
				periods);
	}
}
