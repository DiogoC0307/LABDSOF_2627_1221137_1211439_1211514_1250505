package pt.isep.escala.escalaapi.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** A member's driving licence of one class; a renewal replaces the expiry date (SCRUM-77). */
@Entity
@Table(name = "member_driving_licence")
public class DrivingLicence {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id")
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LicenceClass licenceClass;

	@Column(nullable = false)
	private LocalDate expiresOn;

	protected DrivingLicence() {
	}

	DrivingLicence(Member member, LicenceClass licenceClass, LocalDate expiresOn) {
		this.id = UUID.randomUUID();
		this.member = member;
		this.licenceClass = licenceClass;
		this.expiresOn = expiresOn;
	}

	void renew(LocalDate newExpiresOn) {
		this.expiresOn = newExpiresOn;
	}

	public LicenceClass getLicenceClass() {
		return licenceClass;
	}

	public LocalDate getExpiresOn() {
		return expiresOn;
	}
}
