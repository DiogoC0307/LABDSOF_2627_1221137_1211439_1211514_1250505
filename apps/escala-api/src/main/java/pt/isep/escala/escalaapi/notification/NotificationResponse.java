package pt.isep.escala.escalaapi.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * A notification as GET /api/v1/notifications answers it
 */
record NotificationResponse(
		UUID id,
		NotificationType type,
		String week,
		UUID shiftId,
		Instant startsAt,
		Instant endsAt,
		Instant previousStartsAt,
		Instant previousEndsAt,
		Integer shiftCount,
		Instant createdAt,
		boolean read) {
}
