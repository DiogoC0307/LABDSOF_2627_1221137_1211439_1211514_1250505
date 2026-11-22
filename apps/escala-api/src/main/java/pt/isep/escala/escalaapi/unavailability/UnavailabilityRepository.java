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

	@Modifying
	@Query("DELETE FROM Unavailability u WHERE u.endsAt < :cutoff")
	int deleteEndedBefore(Instant cutoff);
}
