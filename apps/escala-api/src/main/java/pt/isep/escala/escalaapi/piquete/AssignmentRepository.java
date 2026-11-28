package pt.isep.escala.escalaapi.piquete;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

	List<Assignment> findByPublicationId(UUID publicationId);

	long countByPublicationIdAndEvaluationState(UUID publicationId, Assignment.EvaluationState state);

	List<Assignment> findByPiqueteIdInAndStatus(Collection<UUID> piqueteIds, Assignment.Status status);

	List<Assignment> findByMemberIdAndStatusAndStartsAtBeforeAndEndsAtAfter(UUID memberId, Assignment.Status status,
			Instant to, Instant from);
}
