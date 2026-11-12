package pt.isep.escala.escalaapi.piquete;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.messaging.EventFields;
import pt.isep.escala.escalaapi.messaging.RetriesExhaustedHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * When escala-api cannot apply an AvaliacaoConcluida after every retry, the slot shows EVALUATION_FAILED instead of
 * staying PENDING for ever (RF24). The message goes to the DLQ; replaying it makes the slot ELIGIBLE.
 */
@Component
class EvaluationFailedOnExhaustedRetries implements RetriesExhaustedHandler {

	private static final Logger log = LoggerFactory.getLogger(EvaluationFailedOnExhaustedRetries.class);

	private final ObjectMapper objectMapper;
	private final SlotRepository slots;

	EvaluationFailedOnExhaustedRetries(ObjectMapper objectMapper, SlotRepository slots) {
		this.objectMapper = objectMapper;
		this.slots = slots;
	}

	@Override
	public String queue() {
		return AvaliacaoConcluidaListener.QUEUE;
	}

	@Override
	@Transactional
	public void handle(Message message) {
		UUID assignmentId = EventFields.uuid(EventFields.body(objectMapper, message), "assignmentId");
		slots.findByAssignmentId(assignmentId).ifPresent(slot -> {
			if (slot.markEvaluationFailed()) {
				slots.save(slot);
				log.warn("evaluation failed after the retries assignmentId={}", assignmentId);
			}
		});
	}
}
