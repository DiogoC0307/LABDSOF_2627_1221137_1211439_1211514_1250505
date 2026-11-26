package pt.isep.escala.escalaapi.risk;

import java.time.Instant;
import java.time.LocalDate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.messaging.EventFields;
import pt.isep.escala.escalaapi.outbox.ProcessedEvent;
import pt.isep.escala.escalaapi.outbox.ProcessedEventRepository;

/** Keeps the display read model. A duplicate or an older event is ignored; slots are never touched. */
@Component
class RiscoAtualizadoListener {

	static final String QUEUE = "api.risco-atualizado";

	private static final Logger log = LoggerFactory.getLogger(RiscoAtualizadoListener.class);

	private final ObjectMapper objectMapper;
	private final ProcessedEventRepository processed;
	private final DailyRiskRepository risks;

	RiscoAtualizadoListener(ObjectMapper objectMapper, ProcessedEventRepository processed, DailyRiskRepository risks) {
		this.objectMapper = objectMapper;
		this.processed = processed;
		this.risks = risks;
	}

	@RabbitListener(queues = QUEUE)
	@Transactional
	void onRisco(Message message) {
		JsonNode body = EventFields.body(objectMapper, message);
		String traceId = body.path("traceId").asString();
		MDC.put("trace_id", traceId);
		try {
			UUID eventId = EventFields.uuid(body, "eventId");
			if (processed.existsById(eventId)) {
				log.info("RiscoAtualizado duplicate ignored eventId={} traceId={}", eventId, traceId);
				return;
			}
			int rcm = body.path("rcm").asInt();
			String source = body.path("source").asString();
			if (rcm < 1 || rcm > 5 || (!"IPMA".equals(source) && !"SIMULATOR".equals(source))) {
				processed.save(new ProcessedEvent(eventId, Instant.now()));
				log.info("RiscoAtualizado rejected eventId={} traceId={}", eventId, traceId);
				return;
			}
			String dico = body.path("dico").asString();
			LocalDate forecastDate = LocalDate.parse(body.path("forecastDate").asString());
			Instant fetchedAt = Instant.parse(body.path("fetchedAt").asString());
			Instant occurredAt = Instant.parse(body.path("occurredAt").asString());
			boolean assumedHigher = body.path("assumedHigher").asBoolean();
			DailyRisk current = risks.findById(dico).orElse(null);
			if (current == null) {
				risks.save(new DailyRisk(dico, rcm, forecastDate, fetchedAt, source, assumedHigher, occurredAt));
			}
			else {
				current.apply(rcm, forecastDate, fetchedAt, source, assumedHigher, occurredAt);
				risks.save(current);
			}
			processed.save(new ProcessedEvent(eventId, Instant.now()));
			log.info("RiscoAtualizado applied dico={} rcm={} assumedHigher={} traceId={}", dico, rcm, assumedHigher,
					traceId);
		}
		finally {
			MDC.remove("trace_id");
		}
	}
}
