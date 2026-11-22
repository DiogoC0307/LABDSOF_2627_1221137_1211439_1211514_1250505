package pt.isep.escala.escalaapi.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** A member's certification of one type; a renewal replaces the dates (SCRUM-77). */
@Entity
@Table(name = "member_certification")
public class MemberCertification {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id")
	private Member member;

	@ManyToOne(optional = false)
	@JoinColumn(name = "type_code")
	private CertificationType type;

	@Column(nullable = false)
	private LocalDate issuedOn;

	@Column(nullable = false)
	private LocalDate expiresOn;

	protected MemberCertification() {
	}

	MemberCertification(Member member, CertificationType type, LocalDate issuedOn, LocalDate expiresOn) {
		this.id = UUID.randomUUID();
		this.member = member;
		this.type = type;
		this.issuedOn = issuedOn;
		this.expiresOn = expiresOn;
	}

	void renew(LocalDate newIssuedOn, LocalDate newExpiresOn) {
		this.issuedOn = newIssuedOn;
		this.expiresOn = newExpiresOn;
	}

	public CertificationType getType() {
		return type;
	}

	public LocalDate getIssuedOn() {
		return issuedOn;
	}

	public LocalDate getExpiresOn() {
		return expiresOn;
	}
}
