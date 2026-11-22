package pt.isep.escala.elegibilidadeworker.extracao.claude;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import pt.isep.escala.elegibilidadeworker.extracao.baseline.DayParts;

/** Builds a {@link ClaudeCliExtractor} that starts {@link FakeClaude} instead of the real CLI. */
public final class FakeClaudeSupport {

	/** A fake token: the tests check that it never leaves the child process's environment. */
	public static final String TOKEN = "tok-SEGREDO-de-teste-0123456789";

	private FakeClaudeSupport() {
	}

	/** {@code java -cp <test-classes> FakeClaude <mode> <record>}. */
	public static List<String> command(String mode, Path recordFile) {
		String java = ProcessHandle.current().info().command().orElse("java");
		return List.of(java, "-cp", testClasses(), FakeClaude.class.getName(), mode, recordFile.toString());
	}

	/** The environment of a worker with the token, plus {@code extra}. */
	public static Map<String, String> environment(Map<String, String> extra) {
		Map<String, String> env = new HashMap<>();
		copy(env, "PATH");
		copy(env, "SystemRoot");
		copy(env, "TEMP");
		copy(env, "TMP");
		env.put(ClaudeCliExtractor.TOKEN_VARIABLE, TOKEN);
		env.putAll(extra);
		return env;
	}

	public static ClaudeCliExtractor extractor(String mode, Path recordFile, Duration timeout, Map<String, String> extra) {
		return new ClaudeCliExtractor(
				new ClaudeCliExtractor.Settings(command(mode, recordFile), "haiku", timeout, DayParts.DEFAULT),
				environment(extra));
	}

	public static ClaudeCliExtractor extractor(String mode, Path recordFile) {
		return extractor(mode, recordFile, Duration.ofSeconds(30), Map.of());
	}

	/** What {@link FakeClaude} recorded. */
	public static Properties recorded(Path recordFile) throws IOException {
		Properties seen = new Properties();
		try (InputStream in = Files.newInputStream(recordFile)) {
			seen.load(in);
		}
		return seen;
	}

	private static void copy(Map<String, String> env, String name) {
		String value = System.getenv(name);
		if (value != null) {
			env.put(name, value);
		}
	}

	private static String testClasses() {
		try {
			return Path.of(FakeClaude.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}
}
