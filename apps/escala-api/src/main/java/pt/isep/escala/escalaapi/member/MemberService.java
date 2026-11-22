package pt.isep.escala.escalaapi.member;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.isep.escala.escalaapi.auth.SignedInMember;
import pt.isep.escala.escalaapi.validation.InvalidRequestException.InvalidField;

/**
 * Registers and changes members (admin only, checked in SecurityConfig) and shows them to the caller with the contact
 * only for the admin and the member themself (SCRUM-77, 09). The contact and the free values of a request are never
 * logged.
 */
@Service
class MemberService {

	private static final Logger log = LoggerFactory.getLogger(MemberService.class);

	private final MemberRepository members;
	private final CertificationTypeRepository types;
	private final MemberValidation validation;

	MemberService(MemberRepository members, CertificationTypeRepository types, MemberValidation validation) {
		this.members = members;
		this.types = types;
		this.validation = validation;
	}

	@Transactional
	MemberResponse register(MemberRequest request, SignedInMember admin) {
		Map<String, CertificationType> typesByCode = typesByCode();
		List<InvalidField> errors = new ArrayList<>(
				validation.qualifications(request.licences(), request.certifications(), typesByCode));
		errors.addAll(MemberValidation.driverNeedsLicence(request.functions(),
				request.licences() != null && !request.licences().isEmpty(), MemberValidation.LICENCES));
		validation.check(request, errors);

		Member member = Member.register(request.fullName());
		member.changeDetails(request.fullName(), request.rank(), request.rosterStatus(), request.withinAgeLimit(),
				blankToNull(request.contact()), functionsOf(request.functions()));
		member.replaceLicences(licencesByClass(request.licences()));
		member.replaceCertifications(certificationsByType(request.certifications(), typesByCode));
		members.save(member);
		log.info("member registered memberId={} by={}", member.getId(), admin.memberId());
		return view(member, admin);
	}

	@Transactional
	Optional<MemberResponse> changeDetails(UUID id, MemberDetailsRequest request, SignedInMember admin) {
		return members.findById(id).map(member -> {
			validation.check(request, MemberValidation.driverNeedsLicence(
					request.functions(), !member.getLicences().isEmpty(), "functions"));
			member.changeDetails(request.fullName(), request.rank(), request.rosterStatus(), request.withinAgeLimit(),
					blankToNull(request.contact()), functionsOf(request.functions()));
			log.info("member details changed memberId={} by={}", member.getId(), admin.memberId());
			return view(member, admin);
		});
	}

	@Transactional
	Optional<MemberResponse> replaceQualifications(UUID id, QualificationsRequest request, SignedInMember admin) {
		return members.findById(id).map(member -> {
			Map<String, CertificationType> typesByCode = typesByCode();
			List<InvalidField> errors = new ArrayList<>(
					validation.qualifications(request.licences(), request.certifications(), typesByCode));
			errors.addAll(MemberValidation.driverNeedsLicence(new ArrayList<>(member.getFunctions()),
					request.licences() != null && !request.licences().isEmpty(), MemberValidation.LICENCES));
			validation.check(request, errors);
			member.replaceLicences(licencesByClass(request.licences()));
			member.replaceCertifications(certificationsByType(request.certifications(), typesByCode));
			log.info("member qualifications changed memberId={} by={}", member.getId(), admin.memberId());
			return view(member, admin);
		});
	}

	@Transactional(readOnly = true)
	List<MemberResponse> list(SignedInMember viewer) {
		return members.findAll().stream()
				.sorted(Comparator.comparing(Member::getFullName))
				.map(member -> view(member, viewer))
				.toList();
	}

	@Transactional(readOnly = true)
	Optional<MemberResponse> get(UUID id, SignedInMember viewer) {
		return members.findById(id).map(member -> view(member, viewer));
	}

	@Transactional(readOnly = true)
	List<CertificationTypeResponse> certificationTypes() {
		return types.findAll().stream()
				.sorted(Comparator.comparing(CertificationType::getCode))
				.map(type -> new CertificationTypeResponse(type.getCode(), type.getName()))
				.toList();
	}

	private static MemberResponse view(Member member, SignedInMember viewer) {
		boolean seesContact = viewer.role() == AccessRole.ADMIN || viewer.memberId().equals(member.getId());
		return new MemberResponse(
				member.getId(),
				member.getFullName(),
				member.getAccessRole(),
				member.isActive(),
				member.getRank(),
				member.getRosterStatus(),
				member.isWithinAgeLimit(),
				seesContact ? member.getContact() : null,
				member.getFunctions().stream().sorted().toList(),
				member.getLicences().stream()
						.sorted(Comparator.comparing(DrivingLicence::getLicenceClass))
						.map(licence -> new LicenceResponse(licence.getLicenceClass(), licence.getExpiresOn()))
						.toList(),
				member.getCertifications().stream()
						.sorted(Comparator.comparing(certification -> certification.getType().getCode()))
						.map(certification -> new CertificationResponse(
								certification.getType().getCode(),
								certification.getType().getName(),
								certification.getIssuedOn(),
								certification.getExpiresOn()))
						.toList());
	}

	private Map<String, CertificationType> typesByCode() {
		return types.findAll().stream().collect(Collectors.toMap(CertificationType::getCode, Function.identity()));
	}

	private static Map<LicenceClass, LocalDate> licencesByClass(List<LicenceRequest> licences) {
		Map<LicenceClass, LocalDate> byClass = new LinkedHashMap<>();
		licences.forEach(licence -> byClass.put(licence.licenceClass(), licence.expiresOn()));
		return byClass;
	}

	private static Map<String, Member.HeldCertification> certificationsByType(List<CertificationRequest> certifications,
			Map<String, CertificationType> typesByCode) {
		Map<String, Member.HeldCertification> byType = new LinkedHashMap<>();
		certifications.forEach(certification -> byType.put(certification.type(), new Member.HeldCertification(
				typesByCode.get(certification.type()), certification.issuedOn(), certification.expiresOn())));
		return byType;
	}

	private static Set<ServiceFunction> functionsOf(List<ServiceFunction> functions) {
		Set<ServiceFunction> set = EnumSet.noneOf(ServiceFunction.class);
		set.addAll(functions);
		return set;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
