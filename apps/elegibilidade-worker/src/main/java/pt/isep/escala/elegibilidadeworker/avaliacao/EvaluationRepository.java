package pt.isep.escala.elegibilidadeworker.avaliacao;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {
}
