package pt.isep.escala.escalaapi.notification;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The members notifications
 */
@Service
public class Notifications {
	private final NotificationRepository repository;
	private final Clock clock;

	Notifications(NotificationRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	// Weekly schedule publication notification, with the number of shifts the member has that week.
	public void weeklySchedulePublished(UUID memberId, String week, int shiftCount) {
		save(new Notification(memberId, NotificationType.WEEKLY_SCHEDULE_PUBLISHED, week, null, null, null, null, null,
				shiftCount, clock.instant()));
	}

	// Notification of a shift that the member was assigned to
	public void shift(UUID memberId, NotificationType type, String week, UUID piqueteId, Instant startsAt,
			Instant endsAt) {
		save(new Notification(memberId, type, week, piqueteId, startsAt, endsAt, null, null, null, clock.instant()));
	}

	// Changes to the times of a shift that the member was assigned to
	@SuppressWarnings("java:S107") // The member, the shift and its times before and after
	public void timesChanged(UUID memberId, String week, UUID piqueteId, Instant previousStartsAt,
			Instant previousEndsAt, Instant startsAt, Instant endsAt) {
		save(new Notification(memberId, NotificationType.SHIFT_TIMES_CHANGED, week, piqueteId, startsAt, endsAt,
				previousStartsAt, previousEndsAt, null, clock.instant()));
	}

	/**
	 * A notification about a cover request (SCRUM-72): the slot's shift and function, and, when the supervisor is
	 * alerted, why nobody covers it.
	 */
	@SuppressWarnings("java:S107") // The member, the request, its shift and function, and the reason
	public void cover(UUID memberId, NotificationType type, String week, UUID piqueteId, Instant startsAt,
			Instant endsAt, UUID coverRequestId, String role, String reason) {
		save(new Notification(memberId, type, week, piqueteId, startsAt, endsAt, null, null, null, clock.instant())
				.aboutCover(coverRequestId, role, reason));
	}

	// Gets member notifications
	@Transactional(readOnly = true)
	List<NotificationResponse> mine(UUID memberId) {
		return repository.findTop50ByMemberIdOrderByCreatedAtDesc(memberId).stream()
				.map(Notifications::toResponse)
				.toList();
	}

	// Marks member notifications as read
	@Transactional
	void markAllRead(UUID memberId) {
		repository.markAllRead(memberId, clock.instant());
	}

	// Marks one of the member's own notifications as read; false when the member has no notification with that id
	@Transactional
	boolean markRead(UUID memberId, UUID id) {
		if (!repository.existsByIdAndMemberId(id, memberId)) {
			return false;
		}
		repository.markRead(id, memberId, clock.instant());
		return true;
	}

	// Saves a notification to the database
	private void save(Notification notification) {
		repository.save(notification);
	}

	// Converts a notification to a response object	
	private static NotificationResponse toResponse(Notification notification) {
		return new NotificationResponse(notification.getId(), notification.getType(), notification.getWeek(),
				notification.getPiqueteId(), notification.getStartsAt(), notification.getEndsAt(),
				notification.getPreviousStartsAt(), notification.getPreviousEndsAt(), notification.getShiftCount(),
				notification.getCoverRequestId(), notification.getRole(), notification.getReason(),
				notification.getCreatedAt(), notification.getReadAt() != null);
	}
}
