package pt.isep.escala.escalaapi.unavailability;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface UnavailabilityRepository extends JpaRepository<Unavailability, UUID> {

	List<Unavailability> findByMemberIdOrderByStartsAt(UUID memberId);

	Optional<Unavailability> findByIdAndMemberId(UUID id, UUID memberId);

	// Finds all unavailabilities that overlap with the given period
	List<Unavailability> findByStartsAtBeforeAndEndsAtAfterOrderByStartsAt(Instant to, Instant from);

	// One member's periods that overlap with the given period
	List<Unavailability> findByMemberIdAndStartsAtBeforeAndEndsAtAfterOrderByStartsAt(UUID memberId, Instant to, Instant from);

	@Modifying
	@Query("DELETE FROM Unavailability u WHERE u.endsAt < :cutoff")
	int deleteEndedBefore(Instant cutoff);
}
