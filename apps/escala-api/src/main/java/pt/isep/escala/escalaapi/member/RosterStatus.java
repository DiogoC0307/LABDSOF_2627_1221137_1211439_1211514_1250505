package pt.isep.escala.escalaapi.member;

/** The roster a member is on. Only the active roster can be scheduled (RN01, checked by the eligibility rules). */
public enum RosterStatus {
	COMMAND_ROSTER,
	ACTIVE_ROSTER,
	RESERVE_ROSTER,
	HONORARY_ROSTER
}
