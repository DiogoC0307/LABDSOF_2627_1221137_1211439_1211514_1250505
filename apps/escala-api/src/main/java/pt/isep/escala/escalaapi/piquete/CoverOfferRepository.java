package pt.isep.escala.escalaapi.piquete;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CoverOfferRepository extends JpaRepository<CoverOffer, UUID> {

	List<CoverOffer> findByCoverRequestId(UUID coverRequestId);
}
