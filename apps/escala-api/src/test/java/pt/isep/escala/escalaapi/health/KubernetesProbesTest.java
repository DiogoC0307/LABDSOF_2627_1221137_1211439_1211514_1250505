package pt.isep.escala.escalaapi.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The Kubernetes probes of both services use the liveness and readiness groups in dev, qa and prod (SCRUM-88). Reads
 * the manifests in infra/k8s, which Diogo Costa owns.
 */
class KubernetesProbesTest {

	static final Path REPO = Path.of("../..");

	@ParameterizedTest(name = "{0}/{1}")
	@CsvSource({
			"dev, escala-api, 8080", "qa, escala-api, 8080", "prod, escala-api, 8080",
			"dev, elegibilidade-worker, 8081", "qa, elegibilidade-worker, 8081", "prod, elegibilidade-worker, 8081"})
	@DisplayName("TC-SCRUM-88-04 the liveness probe uses /actuator/health/liveness and the readiness probe /actuator/health/readiness")
	void probesUseTheHealthGroups(String environment, String service, int port) throws IOException {
		String manifest = Files.readString(REPO.resolve("infra/k8s/" + environment + "/" + service + "/deployment.yaml"));

		assertThat(probePath(manifest, "livenessProbe")).isEqualTo("/actuator/health/liveness");
		assertThat(probePath(manifest, "readinessProbe")).isEqualTo("/actuator/health/readiness");
		assertThat(manifest).contains("port: " + port);
	}

	private static String probePath(String manifest, String probe) {
		Matcher matcher = Pattern.compile(probe + ":\\s*\\n\\s*httpGet:\\s*\\{\\s*path:\\s*([^,\\s}]+)").matcher(manifest);
		assertThat(matcher.find()).as("%s with an httpGet path", probe).isTrue();
		return matcher.group(1);
	}
}
