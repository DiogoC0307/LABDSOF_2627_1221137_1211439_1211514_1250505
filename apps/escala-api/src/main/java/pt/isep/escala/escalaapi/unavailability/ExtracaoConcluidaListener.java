package pt.isep.escala.escalaapi.unavailability;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.messaging.EventFields;
import pt.isep.escala.escalaapi.messaging.MalformedEventException;
import pt.isep.escala.escalaapi.outbox.ProcessedEvent;
import pt.isep.escala.escalaapi.outbox.ProcessedEventRepository;
import pt.isep.escala.escalaapi.validation.InvalidRequestException.InvalidField;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The worker's proposal, ExtracaoConcluida v2 (SCRUM-75): stored on the message once per eventId (RNF05), only while
 * the message waits for it. Nothing is saved as unavailability until the member confirms (AC1). A message that does
 * not follow the contract goes to the DLQ.
 */
@Component
class ExtracaoConcluidaListener {

	static final String QUEUE = "api.extracao-concluida";

	private static final Logger log = LoggerFactory.getLogger(ExtracaoConcluidaListener.class);

	private static final Set<String> METHODS = Set.of("MODEL", "BASELINE", "FALLBACK");

	private final ObjectMapper objectMapper;
	private final ProcessedEventRepository processed;
	private final UnavailabilityMessageRepository messages;
	private final Clock clock;

	ExtracaoConcluidaListener(ObjectMapper objectMapper, ProcessedEventRepository processed,
			UnavailabilityMessageRepository messages, Clock clock) {
		this.objectMapper = objectMapper;
		this.processed = processed;
		this.messages = messages;
		this.clock = clock;
	}

	@RabbitListener(queues = QUEUE)
	@Transactional
	void onConcluida(Message message) {
		JsonNode body = EventFields.body(objectMapper, message);
		String traceId = body.path("traceId").asString("");
		MDC.put("trace_id", traceId);
		try {
			UUID eventId = EventFields.uuid(body, "eventId");
			if (processed.existsById(eventId)) {
				log.info("ExtracaoConcluida duplicate ignored eventId={} traceId={}", eventId, traceId);
				return;
			}
			UUID requestId = EventFields.uuid(body, "unavailabilityRequestId");
			UUID requestEventId = EventFields.uuid(body, "requestEventId");
			String method = body.path("method").asString("");
			if (!METHODS.contains(method)) {
				throw new MalformedEventException("method is not MODEL, BASELINE or FALLBACK");
			}
			List<ProposedPeriod> periods = periods(body.get("periods"));
			String model = body.path("model").isString() ? body.path("model").asString() : null;
			Integer latencyMs = body.path("latencyMs").canConvertToInt() ? body.path("latencyMs").asInt() : null;

			boolean applied = messages.findById(requestId)
					.filter(waiting -> waiting.isPending() && waiting.getRequestEventId().equals(requestEventId))
					.map(waiting -> {
						waiting.propose(objectMapper.writeValueAsString(periods), method, model, latencyMs, clock.instant());
						return true;
					})
					.orElse(false);
			processed.save(new ProcessedEvent(eventId, Instant.now()));
			log.info("ExtracaoConcluida {} unavailabilityRequestId={} method={} periods={} traceId={}",
					applied ? "applied" : "ignored, no message waits for it", requestId, method, periods.size(), traceId);
		} finally {
			MDC.remove("trace_id");
		}
	}

	/** The periods in the v2 format; anything else is malformed. */
	private List<ProposedPeriod> periods(JsonNode node) {
		if (node == null || !node.isArray()) {
			throw new MalformedEventException("periods is not a list");
		}
		List<ProposedPeriod> periods = new ArrayList<>();
		List<InvalidField> errors = new ArrayList<>();
		for (JsonNode item : node) {
			if (!item.isObject()) {
				throw new MalformedEventException("a period is not an object");
			}
			ProposedPeriod period;
			try {
				period = objectMapper.treeToValue(item, ProposedPeriod.class);
			} catch (JacksonException e) {
				throw new MalformedEventException("a period does not have the v2 format");
			}
			PeriodPatterns.read(period, "periods[" + periods.size() + "]", errors);
			periods.add(period);
		}
		if (!errors.isEmpty()) {
			throw new MalformedEventException("a period does not have the v2 format: " + errors.get(0).field());
		}
		return periods;
	}
}
