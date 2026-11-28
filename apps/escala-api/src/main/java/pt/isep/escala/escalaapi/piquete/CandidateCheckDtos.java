package pt.isep.escala.escalaapi.piquete;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Who can take a role on a shift the supervisor is building.
 */
record CandidateCheckRequest(OffsetDateTime startsAt, OffsetDateTime endsAt, SlotRole role,
		List<OtherShift> otherShifts) {
}

// A member on another shift of the week, in the supervisor's draft.
record OtherShift(UUID memberId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
}

record CandidateCheckResponse(UUID id, CandidateCheckResponse.Status status, List<CandidateResult> candidates) {

	enum Status {
		CHECKING, // While the member is still being checked
		DONE // When the member has been checked
	}
}

record CandidateResult(UUID memberId, CandidateResult.Result result, List<String> rules) {

	enum Result {
		PENDING,
		ELIGIBLE,
		INELIGIBLE,
		NOT_CHECKED
	}
}
