package pt.isep.escala.escalaapi.auth;

import java.util.UUID;
import pt.isep.escala.escalaapi.member.AccessRole;

/**
 * Who is calling, taken from the token and checked against the database. Endpoints that act for a member take the
 * member from here, never from the request body (ADR 0006).
 */
public record SignedInMember(UUID memberId, AccessRole role) {
}
