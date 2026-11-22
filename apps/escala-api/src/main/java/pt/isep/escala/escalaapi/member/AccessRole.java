package pt.isep.escala.escalaapi.member;

/** What a member may do in Escala (ADR 0006; access-control matrix on 09). Not the member's rank. */
public enum AccessRole {
	MEMBER,
	SUPERVISOR,
	ADMIN
}
