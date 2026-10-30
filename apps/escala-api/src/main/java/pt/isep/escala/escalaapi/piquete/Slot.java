package pt.isep.escala.escalaapi.piquete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "slot")
class Slot {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SlotRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SlotStatus status;

	@ManyToOne(optional = false)
	@JoinColumn(name = "piquete_id")
	private Piquete piquete;

	protected Slot() {
	}

	Slot(UUID id, SlotRole role, SlotStatus status) {
		this.id = id;
		this.role = role;
		this.status = status;
	}

	UUID getId() {
		return id;
	}

	SlotRole getRole() {
		return role;
	}

	SlotStatus getStatus() {
		return status;
	}

	void setPiquete(Piquete piquete) {
		this.piquete = piquete;
	}
}
