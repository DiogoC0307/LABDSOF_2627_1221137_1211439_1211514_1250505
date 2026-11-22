package pt.isep.escala.escalaapi.member;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A member of the corps (SCRUM-63 sign-in; SCRUM-77 details, functions, licences and certifications). The contact is
 * personal data: only the admin and the member themself see it, and it is never logged (09).
 */
@Entity
@Table(name = "member")
public class Member {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String fullName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AccessRole accessRole;

	@Column(nullable = false)
	private boolean active;

	@Column(nullable = false)
	private Instant createdAt;

	@Enumerated(EnumType.STRING)
	private Rank rank;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RosterStatus rosterStatus;

	@Column(nullable = false)
	private boolean withinAgeLimit;

	private String contact;

	@ElementCollection
	@CollectionTable(name = "member_function", joinColumns = @JoinColumn(name = "member_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "service_function", nullable = false)
	private Set<ServiceFunction> functions = EnumSet.noneOf(ServiceFunction.class);

	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MemberCertification> certifications = new ArrayList<>();

	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<DrivingLicence> licences = new ArrayList<>();

	protected Member() {
	}

	/** A new member registered by the admin. The access role is always MEMBER (SCRUM-77). */
	static Member register(String fullName) {
		Member member = new Member();
		member.id = UUID.randomUUID();
		member.fullName = fullName;
		member.accessRole = AccessRole.MEMBER;
		member.active = true;
		member.createdAt = Instant.now();
		member.rosterStatus = RosterStatus.ACTIVE_ROSTER;
		member.withinAgeLimit = true;
		return member;
	}

	void changeDetails(String newFullName, Rank newRank, RosterStatus newRosterStatus, boolean newWithinAgeLimit,
			String newContact, Set<ServiceFunction> newFunctions) {
		this.fullName = newFullName;
		this.rank = newRank;
		this.rosterStatus = newRosterStatus;
		this.withinAgeLimit = newWithinAgeLimit;
		this.contact = newContact;
		this.functions.retainAll(newFunctions);
		this.functions.addAll(newFunctions);
	}

	/** A certification to hold, with its dates. */
	record HeldCertification(CertificationType type, LocalDate issuedOn, LocalDate expiresOn) {
	}

	/**
	 * Replaces the certifications, keyed by type code: a type already held is renewed in place (so the unique type per
	 * member is never broken during the flush), a new type is added and a type not given is removed.
	 */
	void replaceCertifications(Map<String, HeldCertification> byTypeCode) {
		certifications.removeIf(certification -> !byTypeCode.containsKey(certification.getType().getCode()));
		byTypeCode.forEach((code, held) -> certifications.stream()
				.filter(certification -> certification.getType().getCode().equals(code))
				.findFirst()
				.ifPresentOrElse(
						certification -> certification.renew(held.issuedOn(), held.expiresOn()),
						() -> certifications.add(new MemberCertification(this, held.type(), held.issuedOn(), held.expiresOn()))));
	}

	/** Replaces the driving licences in the same way as {@link #replaceCertifications}. */
	void replaceLicences(Map<LicenceClass, LocalDate> byClass) {
		licences.removeIf(licence -> !byClass.containsKey(licence.getLicenceClass()));
		byClass.forEach((licenceClass, expiresOn) -> licences.stream()
				.filter(licence -> licence.getLicenceClass() == licenceClass)
				.findFirst()
				.ifPresentOrElse(
						licence -> licence.renew(expiresOn),
						() -> licences.add(new DrivingLicence(this, licenceClass, expiresOn))));
	}

	public UUID getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public AccessRole getAccessRole() {
		return accessRole;
	}

	public boolean isActive() {
		return active;
	}

	public Rank getRank() {
		return rank;
	}

	public RosterStatus getRosterStatus() {
		return rosterStatus;
	}

	public boolean isWithinAgeLimit() {
		return withinAgeLimit;
	}

	public String getContact() {
		return contact;
	}

	public Set<ServiceFunction> getFunctions() {
		return functions;
	}

	public List<MemberCertification> getCertifications() {
		return certifications;
	}

	public List<DrivingLicence> getLicences() {
		return licences;
	}
}
