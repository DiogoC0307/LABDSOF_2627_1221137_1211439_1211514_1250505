package pt.isep.escala.escalaapi.risk;

import org.springframework.data.jpa.repository.JpaRepository;

interface DailyRiskRepository extends JpaRepository<DailyRisk, String> {
}
