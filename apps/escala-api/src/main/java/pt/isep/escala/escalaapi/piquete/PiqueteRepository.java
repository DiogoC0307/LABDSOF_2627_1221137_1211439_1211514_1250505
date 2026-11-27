package pt.isep.escala.escalaapi.piquete;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PiqueteRepository extends JpaRepository<Piquete, UUID> {

	/** The piquetes from {@code from} to {@code to}, both included, in date order. */
	List<Piquete> findByDutyDateBetweenOrderByDutyDateAsc(LocalDate from, LocalDate to);
}
