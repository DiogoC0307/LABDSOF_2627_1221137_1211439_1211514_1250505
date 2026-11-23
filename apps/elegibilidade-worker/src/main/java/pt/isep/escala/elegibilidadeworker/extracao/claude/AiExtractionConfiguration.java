package pt.isep.escala.elegibilidadeworker.extracao.claude;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.isep.escala.elegibilidadeworker.extracao.baseline.DayParts;

/**
 * The feature flag of ADR 0011: the model extractor exists only when {@code escala.ai.enabled=true}
 * ({@code AI_ENABLED}). With the flag off there is no {@link ClaudeCliExtractor} bean, so nothing can call the model
 * and the worker answers {@code FALLBACK} (SCRUM-75).
 *
 * <p>With the flag on and no token, the worker still starts: each call answers {@code ERROR} ("NO_TOKEN") and the form
 * is shown. With {@code ANTHROPIC_API_KEY} set it does not start (ADR 0016).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "escala.ai.enabled", havingValue = "true")
@EnableConfigurationProperties(AiExtractionConfiguration.AiProperties.class)
public class AiExtractionConfiguration {

	/** {@code escala.ai.*}: the model alias, the time limit of one call (10 s, ADR 0016) and the CLI command. */
	@ConfigurationProperties("escala.ai")
	public record AiProperties(boolean enabled, @DefaultValue("haiku") String model,
			@DefaultValue("10s") Duration timeout, @DefaultValue("claude") List<String> cliCommand) {
	}

	/**
	 * Where the extractor reads the token and the variables it passes on: the process environment, unless a test
	 * provides a bean of this type.
	 */
	public interface ProcessEnvironment extends Supplier<Map<String, String>> {
	}

	@Bean
	ClaudeCliExtractor claudeCliExtractor(AiProperties properties, ObjectProvider<ProcessEnvironment> environment) {
		Map<String, String> env = environment.getIfAvailable(() -> System::getenv).get();
		return new ClaudeCliExtractor(new ClaudeCliExtractor.Settings(properties.cliCommand(), properties.model(),
				properties.timeout(), DayParts.DEFAULT), env);
	}
}
