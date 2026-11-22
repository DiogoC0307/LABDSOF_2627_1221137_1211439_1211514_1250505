package pt.isep.escala.elegibilidadeworker.extracao.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** TC-SCRUM-38-07: the feature flag of ADR 0011 decides whether the model extractor exists at all. */
class AiExtractionConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(AiExtractionConfiguration.class);

	@Test
	@DisplayName("TC-SCRUM-38-07 with the flag unset or false there is no model extractor")
	void offByDefault() {
		runner.run(context -> assertThat(context).doesNotHaveBean(ClaudeCliExtractor.class));
		runner.withPropertyValues("escala.ai.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(ClaudeCliExtractor.class));
	}

	@Test
	@DisplayName("TC-SCRUM-38-07 with the flag true there is one, with the 10 s timeout of ADR 0016 unless configured")
	void onWithTheFlag() {
		runner.withPropertyValues("escala.ai.enabled=true").run(context -> {
			assertThat(context).hasSingleBean(ClaudeCliExtractor.class);
			ClaudeCliExtractor.Settings settings = context.getBean(ClaudeCliExtractor.class).settings();
			assertThat(settings.timeout()).isEqualTo(Duration.ofSeconds(10));
			assertThat(settings.model()).isEqualTo("haiku");
			assertThat(settings.command()).containsExactly("claude");
		});
		runner.withPropertyValues("escala.ai.enabled=true", "escala.ai.timeout=60s").run(context -> assertThat(
				context.getBean(ClaudeCliExtractor.class).settings().timeout()).isEqualTo(Duration.ofSeconds(60)));
	}

	@Test
	@DisplayName("TC-SCRUM-38-07 the flag is read from AI_ENABLED and is off when it is not set")
	void readFromAiEnabled() throws IOException {
		try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
			String properties = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertThat(properties).contains("escala.ai.enabled=${AI_ENABLED:false}").contains("escala.ai.timeout=10s");
		}
	}
}
