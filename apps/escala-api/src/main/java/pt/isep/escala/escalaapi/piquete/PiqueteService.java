package pt.isep.escala.escalaapi.piquete;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PiqueteService {

	private final PiqueteRepository piquetes;

	PiqueteService(PiqueteRepository piquetes) {
		this.piquetes = piquetes;
	}

	@Transactional
	PiqueteResponse create(CreatePiqueteRequest request) {
		if (request == null || request.dutyDate() == null) {
			throw new IllegalArgumentException("dutyDate is required");
		}
		Piquete piquete = new Piquete(UUID.randomUUID(), request.dutyDate());
		piquete.addSlot(new Slot(UUID.randomUUID(), SlotRole.CREW_MEMBER, SlotStatus.OPEN));
		return toResponse(piquetes.save(piquete));
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
