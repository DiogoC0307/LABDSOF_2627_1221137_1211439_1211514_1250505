package pt.isep.escala.escalaapi.piquete;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.outbox.OutboxEvent;
import pt.isep.escala.escalaapi.outbox.OutboxEventRepository;
import pt.isep.escala.escalaapi.trace.TraceId;

@Service
class PiqueteService {

	static final String ROUTING_AVALIACAO_PEDIDA = "avaliacao.pedida";

	private static final Logger log = LoggerFactory.getLogger(PiqueteService.class);

	private final PiqueteRepository piquetes;
	private final OutboxEventRepository outbox;
	private final AvaliacaoPedidaFactory pedidos;

	PiqueteService(
			PiqueteRepository piquetes, OutboxEventRepository outbox, AvaliacaoPedidaFactory pedidos) {
		this.piquetes = piquetes;
		this.outbox = outbox;
		this.pedidos = pedidos;
	}

	@Transactional
	PiqueteResponse create(CreatePiqueteRequest request) {
		if (request == null || request.dutyDate() == null) {
			throw new IllegalArgumentException("dutyDate is required");
		}
		UUID assignmentId = UUID.randomUUID();
		Slot slot = new Slot(UUID.randomUUID(), SlotRole.CREW_MEMBER, SlotStatus.PENDING, assignmentId);
		Piquete piquete = new Piquete(UUID.randomUUID(), request.dutyDate());
		piquete.addSlot(slot);
		piquetes.save(piquete);
		UUID eventId = UUID.randomUUID();
		String payload = pedidos.payload(slot, request.dutyDate(), eventId);
		outbox.save(new OutboxEvent(eventId, ROUTING_AVALIACAO_PEDIDA, payload, Instant.now()));
		log.info("AvaliacaoPedida queued eventId={} assignmentId={} traceId={}", eventId, assignmentId, TraceId.current());
		return toResponse(piquete);
	}

	@Transactional(readOnly = true)
	List<PiqueteResponse> list() {
		return piquetes.findAll().stream().map(PiqueteService::toResponse).toList();
	}

	private static PiqueteResponse toResponse(Piquete piquete) {
		List<SlotResponse> slots = piquete.getSlots().stream()
				.map(slot -> new SlotResponse(slot.getId(), slot.getRole(), slot.getStatus()))
				.toList();
		return new PiqueteResponse(piquete.getId(), piquete.getDutyDate(), slots);
	}
}
