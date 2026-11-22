package pt.isep.escala.escalaapi.member;

import java.util.List;

/**
 * A request with invalid fields: answered 400 with the field names and a code per field, never the values
 * (SCRUM-77; SCRUM-88 aligns the other error bodies with this one).
 */
class InvalidRequestException extends RuntimeException {

	/** One invalid field, e.g. {@code certifications[0].expiresOn} with the code {@code expires_before_issued}. */
	record InvalidField(String field, String code) {
	}

	private final transient List<InvalidField> errors;

	InvalidRequestException(List<InvalidField> errors) {
		super(errors.size() + " invalid field(s)");
		this.errors = List.copyOf(errors);
	}

	List<InvalidField> errors() {
		return errors;
	}
}
