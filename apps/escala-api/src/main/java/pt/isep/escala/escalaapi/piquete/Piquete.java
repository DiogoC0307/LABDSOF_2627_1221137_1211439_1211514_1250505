package pt.isep.escala.escalaapi.piquete;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
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

	/**
	 * The shift's period, start, end and minimum crew (V7), the crew being its number of slots: all set together, or
	 * none on a piquete from before.
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 5)
	private ShiftPeriod period;

	private Instant startsAt;

	private Instant endsAt;

	private Integer minimumCrew;

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

	ShiftPeriod getPeriod() {
		return period;
	}

	Instant getStartsAt() {
		return startsAt;
	}

	Instant getEndsAt() {
		return endsAt;
	}

	Integer getMinimumCrew() {
		return minimumCrew;
	}

	List<Slot> getSlots() {
		return slots;
	}

	/** Sets (or changes) the shift: its day, period, start, end and crew size. The service checks them first. */
	void schedule(LocalDate dutyDate, ShiftPeriod period, Instant startsAt, Instant endsAt, int minimumCrew) {
		this.dutyDate = dutyDate;
		this.period = period;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.minimumCrew = minimumCrew;
	}

	void addSlot(Slot slot) {
		slots.add(slot);
		slot.setPiquete(this);
	}

	/** The slot is deleted with the piquete's next save (orphan removal). */
	void removeSlot(Slot slot) {
		slots.remove(slot);
	}
}
