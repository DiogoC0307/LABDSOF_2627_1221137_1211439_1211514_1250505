package pt.isep.escala.escalaapi.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface NotificationRepository extends JpaRepository<Notification, UUID> {

	// Newest notification first, up to 50.
	List<Notification> findTop50ByMemberIdOrderByCreatedAtDesc(UUID memberId);

	@Modifying
	@Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.memberId = :memberId AND n.readAt IS NULL")
	int markAllRead(UUID memberId, Instant readAt);

	boolean existsByIdAndMemberId(UUID id, UUID memberId);

	@Modifying
	@Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.id = :id AND n.memberId = :memberId AND n.readAt IS NULL")
	int markRead(UUID id, UUID memberId, Instant readAt);
}
