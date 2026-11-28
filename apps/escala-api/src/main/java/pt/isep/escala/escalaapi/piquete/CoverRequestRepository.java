package pt.isep.escala.escalaapi.piquete;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CoverRequestRepository extends JpaRepository<CoverRequest, UUID> {

	Optional<CoverRequest> findBySlotIdAndStatus(UUID slotId, CoverRequest.Status status);

	List<CoverRequest> findByStatus(CoverRequest.Status status);
}
