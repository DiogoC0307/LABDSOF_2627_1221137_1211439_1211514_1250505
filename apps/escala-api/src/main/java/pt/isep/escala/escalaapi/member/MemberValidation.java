package pt.isep.escala.escalaapi.member;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import pt.isep.escala.escalaapi.member.InvalidRequestException.InvalidField;

/**
 * Checks a member request and collects every invalid field in one answer: the bean constraints (required, length)
 * and the rules of SCRUM-77 (an expiry before the issue date, an unknown or repeated certification type, a repeated
 * licence class, a driver without a licence). It never reads the values into the errors.
 */
@Component
class MemberValidation {

	static final String CERTIFICATIONS = "certifications";
	static final String LICENCES = "licences";

	private static final String REQUIRED = "required";
	private static final String DUPLICATE = "duplicate";

	private final Validator validator;

	MemberValidation(Validator validator) {
		this.validator = validator;
	}

	/** Throws {@link InvalidRequestException} when {@code errors} plus the bean constraints of {@code request} are not empty. */
	void check(Object request, List<InvalidField> errors) {
		List<InvalidField> all = new ArrayList<>(beanConstraints(request));
		all.addAll(errors);
		if (!all.isEmpty()) {
			throw new InvalidRequestException(all);
		}
	}

	List<InvalidField> qualifications(List<LicenceRequest> licences, List<CertificationRequest> certifications,
			Map<String, CertificationType> typesByCode) {
		List<InvalidField> errors = new ArrayList<>();
		if (certifications != null) {
			Set<String> seen = new HashSet<>();
			for (int i = 0; i < certifications.size(); i++) {
				errors.addAll(certification(certifications.get(i), CERTIFICATIONS + "[" + i + "]", typesByCode, seen));
			}
		}
		if (licences != null) {
			Set<LicenceClass> seen = new HashSet<>();
			for (int i = 0; i < licences.size(); i++) {
				errors.addAll(licence(licences.get(i), LICENCES + "[" + i + "]", seen));
			}
		}
		return errors;
	}

	private static List<InvalidField> certification(CertificationRequest certification, String field,
			Map<String, CertificationType> typesByCode, Set<String> seen) {
		if (certification == null) {
			return List.of(new InvalidField(field, REQUIRED));
		}
		List<InvalidField> errors = new ArrayList<>();
		String type = certification.type();
		if (type != null && !type.isBlank()) {
			if (!typesByCode.containsKey(type)) {
				errors.add(new InvalidField(field + ".type", "unknown_certification_type"));
			} else if (!seen.add(type)) {
				errors.add(new InvalidField(field + ".type", DUPLICATE));
			}
		}
		if (certification.issuedOn() != null && certification.expiresOn() != null
				&& certification.expiresOn().isBefore(certification.issuedOn())) {
			errors.add(new InvalidField(field + ".expiresOn", "expires_before_issued"));
		}
		return errors;
	}

	private static List<InvalidField> licence(LicenceRequest licence, String field, Set<LicenceClass> seen) {
		if (licence == null) {
			return List.of(new InvalidField(field, REQUIRED));
		}
		if (licence.licenceClass() != null && !seen.add(licence.licenceClass())) {
			return List.of(new InvalidField(field + ".licenceClass", DUPLICATE));
		}
		return List.of();
	}

	/** A driver needs at least one driving licence. */
	static List<InvalidField> driverNeedsLicence(List<ServiceFunction> functions, boolean hasLicence, String field) {
		if (functions != null && functions.contains(ServiceFunction.DRIVER) && !hasLicence) {
			return List.of(new InvalidField(field, "driver_without_licence"));
		}
		return List.of();
	}

	private List<InvalidField> beanConstraints(Object request) {
		return validator.validate(request).stream()
				.map(violation -> new InvalidField(violation.getPropertyPath().toString(), code(violation)))
				.sorted(Comparator.comparing(InvalidField::field))
				.toList();
	}

	private static String code(ConstraintViolation<?> violation) {
		String constraint = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
		return switch (constraint) {
			case "NotNull", "NotBlank", "NotEmpty" -> REQUIRED;
			case "Size" -> "too_long";
			default -> constraint.toLowerCase();
		};
	}
}
