package pt.isep.escala.escalaapi.piquete;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.validation.InvalidRequestException;
import pt.isep.escala.escalaapi.validation.InvalidRequestException.InvalidField;

@Service
class ShiftConfirmationService {

	private static final Logger log = LoggerFactory.getLogger(ShiftConfirmationService.class);
	private static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");

	private final SlotRepository slots;
	private final Clock clock;
	private final long advanceHours;

	ShiftConfirmationService(
			SlotRepository slots,
			Clock clock,
			@Value("${escala.shift-confirmation.advance-hours:24}") long advanceHours) {
		this.slots = slots;
		this.clock = clock;
		this.advanceHours = advanceHours;
	}

	@Scheduled(fixedDelayString = "${escala.shift-confirmation.poll-ms:60000}")
	@Transactional
	public List<PresencePrompt> dispatchDuePrompts() {
		Instant now = clock.instant();
		List<PresencePrompt> dispatched = new ArrayList<>();
		for (Slot slot : slots.findAll()) {
			if (slot.shouldPromptConfirmation(now, advanceHours)) {
				slot.markConfirmationPromptSent(now);
				String prompt = slot.confirmationPromptText(LISBON);
				log.info("Shift confirmation prompt sent slotId={} assignmentId={} advanceHours={} prompt=\"{}\"",
						slot.getId(), slot.getAssignmentId(), advanceHours, prompt);
				dispatched.add(new PresencePrompt(slot.getId(), slot.getAssignmentId(), slot.getShiftStartsAt(), prompt));
			}
		}
		return dispatched;
	}

	@Transactional
	PresenceResponse respond(UUID slotId, PresenceRequest request) {
		if (request == null || request.decision() == null) {
			throw new InvalidRequestException(List.of(new InvalidField("decision", "required")));
		}
		Slot slot = slots.findById(slotId)
				.orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
		boolean freed = slot.recordPresence(request.decision());
		slots.save(slot);
		if (freed) {
			log.info("Slot freed due to CANNOT_COME slotId={} assignmentId={}", slot.getId(), slot.getAssignmentId());
		}
		return new PresenceResponse(slot.getId(), slot.getPresenceStatus(), slot.getStatus(), freed);
	}
}