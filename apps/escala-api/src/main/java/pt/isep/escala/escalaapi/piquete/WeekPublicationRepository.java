package pt.isep.escala.escalaapi.piquete;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface WeekPublicationRepository extends JpaRepository<WeekPublication, UUID> {

	/**
	 * The publication, locked until the transaction ends
	 * 
	 * The answers to its assignments arrive on several listener
	 * threads, and only the one that sees the last answer may publish or block the week.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM WeekPublication p WHERE p.id = :id")
	Optional<WeekPublication> lockById(UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<WeekPublication> findByWeekAndStatus(String week, WeekPublication.Status status);

	boolean existsByWeekAndStatus(String week, WeekPublication.Status status);
}
