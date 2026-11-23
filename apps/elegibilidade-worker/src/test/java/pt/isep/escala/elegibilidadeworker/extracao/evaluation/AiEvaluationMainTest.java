package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static pt.isep.escala.elegibilidadeworker.extracao.claude.FakeClaudeSupport.TOKEN;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pt.isep.escala.elegibilidadeworker.extracao.claude.FakeClaudeSupport;

/**
 * TC-SCRUM-38-10: the runner of the SCRUM-38 run, with {@code FakeClaude} as the CLI and a hand-made dataset (never the
 * evaluation dataset).
 */
class AiEvaluationMainTest {

	@TempDir
	Path tmp;

	private final ByteArrayOutputStream printed = new ByteArrayOutputStream();
	private final PrintStream out = new PrintStream(printed, true, StandardCharsets.UTF_8);

	private static Map<String, String> env(Map<String, String> extra) {
		Map<String, String> env = new HashMap<>(FakeClaudeSupport.environment(Map.of()));
		env.put("AI_ENABLED", "true");
		env.putAll(extra);
		return env;
	}

	/** The options, followed by {@code --command <part>} for each part of the FakeClaude command. */
	private String[] args(String mode, String... options) {
		List<String> args = new java.util.ArrayList<>(List.of(options));
		for (String part : FakeClaudeSupport.command(mode, tmp.resolve("record.properties"))) {
			args.add("--command");
			args.add(part);
		}
		return args.toArray(String[]::new);
	}

	private String printed() {
		return printed.toString(StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("TC-SCRUM-38-10 the run refuses to start without AI_ENABLED=true, with ANTHROPIC_API_KEY, or without the token")
	void guards() {
		String[] args = { "--smoke" };
		Map<String, String> noFlag = env(Map.of());
		noFlag.remove("AI_ENABLED");
		assertThat(AiEvaluationMain.run(args, noFlag, out)).isEqualTo(2);
		assertThat(AiEvaluationMain.run(args, env(Map.of("ANTHROPIC_API_KEY", "sk-ant-CHAVE")), out)).isEqualTo(2);
		Map<String, String> noToken = env(Map.of());
		noToken.remove("CLAUDE_CODE_OAUTH_TOKEN");
		assertThat(AiEvaluationMain.run(args, noToken, out)).isEqualTo(2);
		assertThat(AiEvaluationMain.run(new String[] { "--runs", "0", "--dataset", "x" }, env(Map.of()), out))
				.isEqualTo(2);
		assertThat(printed()).contains("AI_ENABLED is not true", "ANTHROPIC_API_KEY is set",
				"CLAUDE_CODE_OAUTH_TOKEN is not set").doesNotContain("CHAVE").doesNotContain(TOKEN);
	}

	@Test
	@DisplayName("TC-SCRUM-38-10 the smoke call prints the outcome, the periods and the cost, never the token")
	void smoke() {
		int code = AiEvaluationMain.run(args("ok", "--smoke"), env(Map.of()), out);
		assertThat(code).isZero();
		assertThat(printed()).contains("outcome=OK", "cost=0.00123", "2026-10-31..2026-11-01")
				.doesNotContain(TOKEN);
	}

	@Test
	@DisplayName("TC-SCRUM-38-10 three runs write one report per run and a summary with run 1 as the headline; no token in any of them")
	void runs() throws IOException {
		Path dataset = tmp.resolve("hand-made.jsonl");
		Files.writeString(dataset, String.join("\n",
				"{\"id\":\"H-1\",\"category\":\"normal\",\"input\":\"não posso sexta\",\"referenceDate\":\"2026-10-26\","
						+ "\"expected\":{\"periods\":[{\"startDate\":\"2026-10-30\",\"endDate\":\"2026-10-30\"}]}}",
				"{\"id\":\"H-2\",\"category\":\"adverse\",\"input\":\"só posso sexta\",\"referenceDate\":\"2026-10-26\","
						+ "\"expected\":{\"periods\":[]}}"), StandardCharsets.UTF_8);
		Path outDir = tmp.resolve("out");

		int code = AiEvaluationMain.run(args("ok-empty", "--dataset", dataset.toString(), "--out", outDir.toString(),
				"--runs", "3", "--timeout", "30s", "--dataset-commit", "abc1234"), env(Map.of()), out);

		assertThat(code).isZero();
		for (int run = 1; run <= 3; run++) {
			assertThat(outDir.resolve("haiku-results-run" + run + ".json")).exists();
			assertThat(outDir.resolve("haiku-results-run" + run + ".md")).exists();
		}
		String summary = Files.readString(outDir.resolve("haiku-results.md"));
		assertThat(summary).contains("Run 1 is the headline", "| Run 1 | Run 2 | Run 3 |", "abc1234",
				"Periods exactly right (normal)", "No valid structured output", "Slower than 10 s", "timeout 30 s");
		// progress shows case ids and outcomes, never the message text
		assertThat(printed()).contains("H-1", "H-2").doesNotContain("posso sexta").doesNotContain(TOKEN);
		try (Stream<Path> files = Files.list(outDir)) {
			for (Path file : files.toList()) {
				assertThat(Files.readString(file)).as(file.getFileName().toString()).doesNotContain(TOKEN);
			}
		}
	}

	@Test
	@DisplayName("TC-SCRUM-38-10 the dataset hash ignores line endings, as in TC-SCRUM-99-09")
	void hash() throws Exception {
		byte[] lf = "a\nb\n".getBytes(StandardCharsets.UTF_8);
		byte[] crlf = "a\r\nb\r\n".getBytes(StandardCharsets.UTF_8);
		assertThat(AiEvaluationMain.sha256WithLfLineEndings(crlf)).isEqualTo(AiEvaluationMain.sha256WithLfLineEndings(lf));
		assertThat(List.of(AiEvaluationMain.memory())).isNotEmpty();
	}
}
