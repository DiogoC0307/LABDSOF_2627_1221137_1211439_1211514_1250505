package pt.isep.escala.escalaapi.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String routingKey;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String payload;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant publishedAt;

	protected OutboxEvent() {
	}

	public OutboxEvent(UUID id, String routingKey, String payload, Instant createdAt) {
		this.id = id;
		this.routingKey = routingKey;
		this.payload = payload;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getRoutingKey() {
		return routingKey;
	}

	public String getPayload() {
		return payload;
	}

	public void markPublished(Instant at) {
		this.publishedAt = at;
	}
}
