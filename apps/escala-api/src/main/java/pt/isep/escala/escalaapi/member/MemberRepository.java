package pt.isep.escala.escalaapi.member;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {

	List<Member> findByActiveTrue();

	Optional<Member> findByIdAndActiveTrue(UUID id);
}
