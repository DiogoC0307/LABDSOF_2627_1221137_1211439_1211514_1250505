package pt.isep.escala.elegibilidadeworker.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics settings kept in code, so the only property is the exposure of {@code /actuator/prometheus} (SCRUM-97):
 * the tag {@code application} on every meter, and the tag policy of {@link MetricTags}.
 */
@Configuration(proxyBeanMethods = false)
public class MetricsConfiguration {

	@Bean
	MeterRegistryCustomizer<MeterRegistry> applicationTag(@Value("${spring.application.name}") String application) {
		return registry -> registry.config().commonTags("application", application);
	}

	/** An {@code escala.*} meter with a tag key that is not in {@link MetricTags#ALLOWED_KEYS} is never registered. */
	@Bean
	MeterFilter escalaTagPolicy() {
		return MeterFilter.deny(MetricsConfiguration::breaksTagPolicy);
	}

	static boolean breaksTagPolicy(Meter.Id id) {
		return id.getName().startsWith("escala.")
				&& id.getTags().stream().map(Tag::getKey).anyMatch(key -> !MetricTags.ALLOWED_KEYS.contains(key));
	}
}
