package pt.isep.escala.escalaapi.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * TC-SCRUM-97-02: the web proxy (nginx in Docker Compose and in the three Kubernetes environments) forwards only the
 * health endpoints of the Actuator. {@code /actuator/prometheus} needs no token (Prometheus scrapes it inside the
 * cluster), so it must not be reachable from outside. nginx picks the longest matching prefix location; the test does
 * the same on the configuration files.
 */
class ProxyActuatorExposureTest {

	// possessive quantifiers: the prefix cannot hold a space or a brace, so nothing is ever given back
	private static final Pattern LOCATION = Pattern.compile("location\\s++([^\\s{]++)\\s*+\\{([^}]*+)}");

	static Stream<Path> proxies() {
		return Stream.of(Path.of("../../infra/compose/nginx.conf"),
				Path.of("../../infra/k8s/dev/web/configmap.yaml"),
				Path.of("../../infra/k8s/qa/web/configmap.yaml"),
				Path.of("../../infra/k8s/prod/web/configmap.yaml"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("proxies")
	@DisplayName("TC-SCRUM-97-02 the web proxy forwards /api/actuator/health and its groups, and answers 404 for /api/actuator/prometheus and every other Actuator path")
	void onlyHealthIsForwarded(Path file) throws IOException {
		Map<String, String> locations = locations(Files.readString(file));
		assertThat(locations.keySet()).as("only prefix locations, so nothing can match before them")
				.allMatch(prefix -> prefix.startsWith("/"));

		for (String uri : new String[] { "/api/actuator/health", "/api/actuator/health/liveness",
				"/api/actuator/health/readiness" }) {
			assertThat(handling(locations, uri)).as(uri).contains("proxy_pass").doesNotContain("return 404");
		}
		for (String uri : new String[] { "/api/actuator/prometheus", "/api/actuator/env", "/api/actuator/",
				"/api/actuator/metrics" }) {
			assertThat(handling(locations, uri)).as(uri).contains("return 404").doesNotContain("proxy_pass");
		}
	}

	private static Map<String, String> locations(String config) {
		Map<String, String> locations = new LinkedHashMap<>();
		Matcher matcher = LOCATION.matcher(config);
		while (matcher.find()) {
			locations.put(matcher.group(1), matcher.group(2));
		}
		return locations;
	}

	/** The body of the location nginx uses for {@code uri}: the longest prefix that matches it. */
	private static String handling(Map<String, String> locations, String uri) {
		return locations.entrySet().stream().filter(e -> uri.startsWith(e.getKey()))
				.max((a, b) -> Integer.compare(a.getKey().length(), b.getKey().length())).map(Map.Entry::getValue)
				.orElse("");
	}
}
