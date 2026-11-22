package pt.isep.escala.escalaapi.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A kind of certification, e.g. TAS or TAT (seeded by V4; no editing in SCRUM-77). */
@Entity
@Table(name = "certification_type")
public class CertificationType {

	@Id
	private String code;

	@Column(nullable = false)
	private String name;

	protected CertificationType() {
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}
}
