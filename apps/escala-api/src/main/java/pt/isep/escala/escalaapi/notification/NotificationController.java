package pt.isep.escala.escalaapi.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.isep.escala.escalaapi.auth.SignedInMember;

// Members Notifications API. 
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController {

	private final Notifications notifications;

	NotificationController(Notifications notifications) {
		this.notifications = notifications;
	}

	// The newest first, at most 50.
	@GetMapping
	List<NotificationResponse> mine(@AuthenticationPrincipal SignedInMember member) {
		return notifications.mine(member.memberId());
	}

	// Marks every notification of the member as read, when they open the list.
	@PostMapping("/read")
	ResponseEntity<Void> markAllRead(@AuthenticationPrincipal SignedInMember member) {
		notifications.markAllRead(member.memberId());
		return ResponseEntity.noContent().build();
	}

	// Marks one of the member's own notifications as read; 404 when it is not theirs.
	@PostMapping("/{id}/read")
	ResponseEntity<Void> markRead(@AuthenticationPrincipal SignedInMember member, @PathVariable UUID id) {
		if (!notifications.markRead(member.memberId(), id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return ResponseEntity.noContent().build();
	}
}
