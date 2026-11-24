package pt.isep.escala.escalaapi.unavailability;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UnavailabilityMessageRepository extends JpaRepository<UnavailabilityMessage, UUID> {

	Optional<UnavailabilityMessage> findByIdAndMemberId(UUID id, UUID memberId);

	long countByMemberIdAndCreatedAtAfter(UUID memberId, Instant after);

	Optional<UnavailabilityMessage> findFirstByMemberIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID memberId, Instant after);

	List<UnavailabilityMessage> findByFreeTextIsNotNullAndCreatedAtLessThanEqual(Instant cutoff);
}
