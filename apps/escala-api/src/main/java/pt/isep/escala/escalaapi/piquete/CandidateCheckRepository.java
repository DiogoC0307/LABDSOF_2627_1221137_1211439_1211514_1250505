package pt.isep.escala.escalaapi.piquete;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface CandidateCheckRepository extends JpaRepository<CandidateCheck, UUID> {

	List<CandidateCheck> findByRequestId(UUID requestId);

	@Modifying
	@Query("DELETE FROM CandidateCheck c WHERE c.createdAt < :cutoff")
	int deleteMadeBefore(Instant cutoff);
}
