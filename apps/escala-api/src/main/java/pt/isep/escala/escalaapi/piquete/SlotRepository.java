package pt.isep.escala.escalaapi.piquete;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SlotRepository extends JpaRepository<Slot, UUID> {

	Optional<Slot> findByAssignmentId(UUID assignmentId);
}
