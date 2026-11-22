package pt.isep.escala.escalaapi.auth;

import java.util.UUID;
import pt.isep.escala.escalaapi.member.AccessRole;

record MockUserResponse(UUID id, String fullName, AccessRole role) {
}

record MockSignInRequest(UUID memberId) {
}

record MockSignInResponse(String token, UUID memberId, AccessRole role, String fullName) {
}
