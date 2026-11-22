package pt.isep.escala.elegibilidadeworker.health;

import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * DOWN when a RabbitMQ listener container is not running, so a stopped consumer takes the pod out of the Service
 * and is visible, instead of leaving messages waiting with health UP (SCRUM-88). Part of the readiness group as
 * "listeners"; never of liveness, so it does not restart the pod.
 */
@Component
class ListenersHealthIndicator implements HealthIndicator {

	private final RabbitListenerEndpointRegistry registry;

	ListenersHealthIndicator(RabbitListenerEndpointRegistry registry) {
		this.registry = registry;
	}

	@Override
	public Health health() {
		long total = registry.getListenerContainers().size();
		long stopped = registry.getListenerContainers().stream().filter(container -> !container.isRunning()).count();
		Health.Builder health = total > 0 && stopped == 0 ? Health.up() : Health.down();
		return health.withDetail("containers", total).withDetail("stopped", stopped).build();
	}
}
