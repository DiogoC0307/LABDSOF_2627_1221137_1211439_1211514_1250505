package pt.isep.escala.escalaapi.notification;

public enum NotificationType {
	WEEKLY_SCHEDULE_PUBLISHED,
	ADDED_TO_SHIFT,
	REMOVED_FROM_SHIFT,
	SHIFT_TIMES_CHANGED,
	SHIFT_CANCELLED,
	/** To the supervisor: a member can't come and their slot is open (SCRUM-71 AC1, SCRUM-72). */
	SLOT_OPENED,
	/** To a member the eligibility rules accept: can they cover the slot? (SCRUM-72 AC1) */
	COVER_REQUESTED,
	/** To the supervisor: nobody is free and qualified, or nobody accepted in time (SCRUM-72 AC3, AC4). */
	COVER_ESCALATED
}
