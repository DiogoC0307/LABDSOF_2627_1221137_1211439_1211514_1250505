package pt.isep.escala.escalaapi.piquete;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PiqueteRepository extends JpaRepository<Piquete, UUID> {
}
