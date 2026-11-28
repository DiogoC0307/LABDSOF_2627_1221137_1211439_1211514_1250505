package pt.isep.escala.escalaapi.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Members receive notifications about schedule publication and changes to their own shifts.
 */
@Entity
@Table(name = "notification")
class Notification {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID memberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private NotificationType type;

	@Column(nullable = false, length = 8)
	private String week;

	private UUID piqueteId;

	private Instant startsAt;

	private Instant endsAt;

	private Instant previousStartsAt;

	private Instant previousEndsAt;

	private Integer shiftCount;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant readAt;

	protected Notification() {
	}

	@SuppressWarnings("java:S107")
	Notification(UUID memberId, NotificationType type, String week, UUID piqueteId, Instant startsAt, Instant endsAt,
			Instant previousStartsAt, Instant previousEndsAt, Integer shiftCount, Instant createdAt) {
		this.id = UUID.randomUUID();
		this.memberId = memberId;
		this.type = type;
		this.week = week;
		this.piqueteId = piqueteId;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.previousStartsAt = previousStartsAt;
		this.previousEndsAt = previousEndsAt;
		this.shiftCount = shiftCount;
		this.createdAt = createdAt;
	}

	UUID getId() {
		return id;
	}

	UUID getMemberId() {
		return memberId;
	}

	NotificationType getType() {
		return type;
	}

	String getWeek() {
		return week;
	}

	UUID getPiqueteId() {
		return piqueteId;
	}

	Instant getStartsAt() {
		return startsAt;
	}

	Instant getEndsAt() {
		return endsAt;
	}

	Instant getPreviousStartsAt() {
		return previousStartsAt;
	}

	Instant getPreviousEndsAt() {
		return previousEndsAt;
	}

	Integer getShiftCount() {
		return shiftCount;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getReadAt() {
		return readAt;
	}
}
