package pt.isep.escala.escalaapi.member;

import org.springframework.data.jpa.repository.JpaRepository;

interface CertificationTypeRepository extends JpaRepository<CertificationType, String> {
}
