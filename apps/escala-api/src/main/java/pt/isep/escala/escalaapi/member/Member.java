package pt.isep.escala.escalaapi.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A member of the corps who can sign in (SCRUM-63). Registration and certifications come with SCRUM-77. */
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

	protected Member() {
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
}
