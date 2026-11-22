package pt.isep.escala.escalaapi.member;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Registration by the admin: details, functions, licences and certifications (SCRUM-77). */
record MemberRequest(
		@NotBlank @Size(max = 120) String fullName,
		@NotNull Rank rank,
		@NotNull RosterStatus rosterStatus,
		@NotNull Boolean withinAgeLimit,
		@Size(max = 120) String contact,
		@NotNull List<ServiceFunction> functions,
		@NotNull List<@Valid LicenceRequest> licences,
		@NotNull List<@Valid CertificationRequest> certifications) {
}

/** A change of details and functions; licences and certifications change through their own endpoint. */
record MemberDetailsRequest(
		@NotBlank @Size(max = 120) String fullName,
		@NotNull Rank rank,
		@NotNull RosterStatus rosterStatus,
		@NotNull Boolean withinAgeLimit,
		@Size(max = 120) String contact,
		@NotNull List<ServiceFunction> functions) {
}

/** The licences and certifications a member holds after the change; anything not listed is removed. */
record QualificationsRequest(
		@NotNull List<@Valid LicenceRequest> licences,
		@NotNull List<@Valid CertificationRequest> certifications) {
}

record LicenceRequest(@NotNull LicenceClass licenceClass, @NotNull LocalDate expiresOn) {
}

record CertificationRequest(@NotBlank String type, @NotNull LocalDate issuedOn, @NotNull LocalDate expiresOn) {
}

/** A member as the caller may see them: the contact only for the admin and the member themself. */
record MemberResponse(
		UUID id,
		String fullName,
		AccessRole accessRole,
		boolean active,
		Rank rank,
		RosterStatus rosterStatus,
		boolean withinAgeLimit,
		@JsonInclude(JsonInclude.Include.NON_NULL) String contact,
		List<ServiceFunction> functions,
		List<LicenceResponse> licences,
		List<CertificationResponse> certifications) {
}

record LicenceResponse(LicenceClass licenceClass, LocalDate expiresOn) {
}

record CertificationResponse(String type, String name, LocalDate issuedOn, LocalDate expiresOn) {
}

record CertificationTypeResponse(String code, String name) {
}
