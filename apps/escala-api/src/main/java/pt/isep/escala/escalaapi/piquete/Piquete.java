package pt.isep.escala.escalaapi.piquete;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "piquete")
class Piquete {

	@Id
	private UUID id;

	@Column(nullable = false)
	private LocalDate dutyDate;

	@OneToMany(mappedBy = "piquete", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Slot> slots = new ArrayList<>();

	protected Piquete() {
	}

	Piquete(UUID id, LocalDate dutyDate) {
		this.id = id;
		this.dutyDate = dutyDate;
	}

	UUID getId() {
		return id;
	}

	LocalDate getDutyDate() {
		return dutyDate;
	}

	List<Slot> getSlots() {
		return slots;
	}

	void addSlot(Slot slot) {
		slots.add(slot);
		slot.setPiquete(this);
	}
}
