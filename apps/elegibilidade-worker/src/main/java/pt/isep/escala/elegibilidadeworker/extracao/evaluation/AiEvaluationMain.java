package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult;
import pt.isep.escala.elegibilidadeworker.extracao.claude.ClaudeCliExtractor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * The SCRUM-38 run: Claude Haiku through the Claude Code CLI on the evaluation dataset, scored by the same
 * {@link EvaluationScorer} as the rule-based baseline. It runs inside the worker container (the {@code ai-eval}
 * service of {@code compose.yaml}), one case at a time. No Spring: started with Spring Boot's
 * {@code PropertiesLauncher}.
 *
 * <p>Options: {@code --dataset <file>} {@code --out <folder>} {@code --runs <n>} (default 3) {@code --timeout <60s>}
 * {@code --dataset-commit <hash>} {@code --command <part>} (repeatable, one part of the CLI command each; default {@code claude}) {@code --smoke} (one call with an example from 06,
 * before the real run).
 *
 * <p>It refuses to run unless {@code AI_ENABLED=true} and {@code CLAUDE_CODE_OAUTH_TOKEN} are set and
 * {@code ANTHROPIC_API_KEY} is not. It never prints or writes the token, and it prints no message text: only case ids,
 * outcomes and times. The reports it writes hold the dataset's own synthetic messages, as the baseline report does.
 */
public final class AiEvaluationMain {

	static final String LABEL = "Claude Haiku via the Claude Code CLI, prompt v0";
	static final Duration MEASUREMENT_TIMEOUT = Duration.ofSeconds(60);

	private AiEvaluationMain() {
	}

	public static void main(String[] args) {
		System.exit(run(args, System.getenv(), System.out));
	}

	/** Returns the exit code: 0 done, 1 the smoke call gave no valid answer, 2 wrong options or environment. */
	static int run(String[] args, Map<String, String> environment, PrintStream out) {
		Options options;
		try {
			options = Options.parse(args);
		}
		catch (RuntimeException e) { // a bad number or duration too
			out.println("ai-eval: " + e.getMessage());
			return 2;
		}
		if (!"true".equalsIgnoreCase(environment.get("AI_ENABLED"))) {
			out.println("ai-eval: AI_ENABLED is not true; the model is called only with AI on (ADR 0011).");
			return 2;
		}
		if (environment.containsKey("ANTHROPIC_API_KEY")) {
			out.println("ai-eval: ANTHROPIC_API_KEY is set; unset it, the run uses only the subscription token (ADR 0016).");
			return 2;
		}
		String token = environment.get(ClaudeCliExtractor.TOKEN_VARIABLE);
		if (token == null || token.isBlank()) {
			out.println("ai-eval: " + ClaudeCliExtractor.TOKEN_VARIABLE + " is not set in this shell.");
			return 2;
		}
		ClaudeCliExtractor extractor = new ClaudeCliExtractor(ClaudeCliExtractor.Settings.defaults()
				.withCommand(options.command()).withTimeout(options.timeout()), environment);
		try {
			return options.smoke() ? smoke(extractor, out) : evaluate(extractor, options, out);
		}
		catch (IOException | IllegalArgumentException | NoSuchAlgorithmException e) {
			// the message of these never holds the token; dataset errors give only a line number
			out.println("ai-eval: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return 2;
		}
	}

	/** One call with the first example of 06 ("não posso este fds" on 2026-10-26), to check the CLI and the login. */
	private static int smoke(ClaudeCliExtractor extractor, PrintStream out) {
		long started = System.nanoTime();
		ExtractionResult result = extractor.extract("não posso este fds", LocalDate.parse("2026-10-26"));
		double seconds = (System.nanoTime() - started) / 1e9;
		out.printf(Locale.ROOT, "ai-eval smoke: outcome=%s reason=%s periods=%s model=%s cost=%s time=%.1f s%n",
				result.outcome(), result.reason(), EvaluationReport.describe(result.periods()), result.model(),
				result.costUsd(), seconds);
		out.println("ai-eval smoke: 06 expects 2026-10-31..2026-11-01");
		return result.outcome() == ExtractionResult.Outcome.OK ? 0 : 1;
	}

	private static int evaluate(ClaudeCliExtractor extractor, Options options, PrintStream out)
			throws IOException, NoSuchAlgorithmException {
		byte[] bytes;
		try (InputStream in = Files.newInputStream(options.dataset())) {
			bytes = in.readAllBytes();
		}
		ObjectMapper mapper = JsonMapper.builder().build();
		List<EvaluationCase> cases = EvaluationCase.readAll(new java.io.ByteArrayInputStream(bytes), mapper);
		String sha256 = sha256WithLfLineEndings(bytes);
		Files.createDirectories(options.out());
		List<EvaluationReport> reports = new ArrayList<>();
		for (int run = 1; run <= options.runs(); run++) {
			int current = run;
			out.printf(Locale.ROOT, "ai-eval: run %d of %d, %d cases, timeout %d s%n", run, options.runs(),
					cases.size(), options.timeout().toSeconds());
			int[] done = { 0 };
			EvaluationReport report = new EvaluationScorer(extractor,
					LABEL + ", run " + run + " of " + options.runs(), EvaluationReport.MODEL_LATENCY_NOTE)
					.score(cases, r -> out.printf(Locale.ROOT, "ai-eval: run %d case %d/%d %s %s %.1f s%n", current,
							++done[0], cases.size(), r.testCase().id(), r.actual().outcome(), r.latencyMs() / 1000))
					.withDataset(options.datasetCommit(), sha256);
			Files.writeString(options.out().resolve("haiku-results-run" + run + ".json"), report.toJson(mapper),
					StandardCharsets.UTF_8);
			Files.writeString(options.out().resolve("haiku-results-run" + run + ".md"), report.toMarkdown(),
					StandardCharsets.UTF_8);
			reports.add(report);
		}
		Files.writeString(options.out().resolve("haiku-results.md"), summary(reports, options, sha256),
				StandardCharsets.UTF_8);
		out.println("ai-eval: done; reports in " + options.out());
		return 0;
	}

	/** The three runs side by side, run 1 first (the headline: no run is picked after the fact). */
	static String summary(List<EvaluationReport> reports, Options options, String sha256) {
		StringBuilder md = new StringBuilder();
		md.append("# Claude Haiku through the Claude Code CLI on the evaluation dataset\n\n");
		md.append("- **Extractor:** ").append(LABEL).append(" (`ClaudeCliExtractor`); model reported: ")
				.append(reports.stream().flatMap(r -> r.models().stream()).distinct().sorted().reduce((a, b) -> a + ", " + b)
						.orElse("none"))
				.append(".\n");
		md.append("- **Dataset:** commit `").append(options.datasetCommit() == null ? "not recorded" : options.datasetCommit())
				.append("`; SHA-256 `").append(sha256).append("`.\n");
		md.append("- **Runs:** ").append(reports.size()).append(", one case at a time, timeout ")
				.append(options.timeout().toSeconds()).append(" s per call (the product uses 10 s). **Run 1 is the headline**; "
						+ "the others show how much the model varies.\n");
		md.append("- **Latency:** ").append(EvaluationReport.MODEL_LATENCY_NOTE).append("\n");
		md.append("- **Memory of the container:** ").append(memory()).append("\n\n");
		md.append("| Measure | Target |");
		for (int i = 1; i <= reports.size(); i++) {
			md.append(" Run ").append(i).append(" |");
		}
		md.append("\n|---|---|").append("---|".repeat(reports.size())).append("\n");
		line(md, reports, "Periods exactly right (normal)", "≥ 85%", r -> pct(r.category("normal").exactRate()));
		line(md, reports, "Dates right (normal)", "≥ 95%", r -> pct(r.category("normal").datesRightRate()));
		line(md, reports, "Right number of periods (all)", "≥ 95%", r -> pct(r.overall().countRightRate()));
		line(md, reports, "Adverse handled", "100%", r -> pct(r.category("adverse").exactRate()));
		line(md, reports, "p50 latency", "", r -> String.format(Locale.ROOT, "%.2f s", r.p50LatencyMs() / 1000));
		line(md, reports, "p95 latency", "< 4 s", r -> String.format(Locale.ROOT, "%.2f s", r.p95LatencyMs() / 1000));
		line(md, reports, "Slower than 4 s", "", r -> pct(r.shareSlowerThan(EvaluationReport.TARGET_P95_MS)));
		line(md, reports, "Slower than 10 s", "", r -> pct(r.shareSlowerThan(EvaluationReport.PRODUCT_TIMEOUT_MS)));
		line(md, reports, "No valid structured output", "",
				r -> String.valueOf(r.outcomes().get(ExtractionResult.Outcome.NO_VALID_OUTPUT)));
		line(md, reports, "Timeouts", "", r -> String.valueOf(r.outcomes().get(ExtractionResult.Outcome.TIMEOUT)));
		line(md, reports, "Errors", "", r -> String.valueOf(r.outcomes().get(ExtractionResult.Outcome.ERROR)));
		line(md, reports, "Cost per call (CLI estimate)", "measured",
				r -> r.costsUsd().length == 0 ? "n/a"
						: String.format(Locale.ROOT, "%.5f USD", Arrays.stream(r.costsUsd()).average().orElse(0)));
		md.append("\nPer category and per case: `haiku-results-run<n>.md` and `.json` next to this file.\n");
		return md.toString();
	}

	private static void line(StringBuilder md, List<EvaluationReport> reports, String measure, String target,
			Function<EvaluationReport, String> value) {
		md.append("| ").append(measure).append(" | ").append(target).append(" |");
		reports.forEach(r -> md.append(' ').append(value.apply(r)).append(" |"));
		md.append('\n');
	}

	private static String pct(double rate) {
		return Double.isNaN(rate) ? "n/a" : String.format(Locale.ROOT, "%.1f%%", rate * 100);
	}

	/** Peak memory and limit of this container, from cgroup v2; "not available" elsewhere (e.g. in tests). */
	static String memory() {
		try {
			long peak = Long.parseLong(Files.readString(Path.of("/sys/fs/cgroup/memory.peak")).trim());
			String max = Files.readString(Path.of("/sys/fs/cgroup/memory.max")).trim();
			String limit = max.equals("max") ? "no limit" : (Long.parseLong(max) / (1024 * 1024)) + " MiB";
			return "peak " + (peak / (1024 * 1024)) + " MiB, limit " + limit + " (cgroup v2, whole run)";
		}
		catch (IOException | RuntimeException e) {
			return "not available";
		}
	}

	/** As in TC-SCRUM-99-09: the hash of the file as git stores it (LF), the same on Windows and Linux. */
	static String sha256WithLfLineEndings(byte[] bytes) throws NoSuchAlgorithmException {
		byte[] lf = new String(bytes, StandardCharsets.UTF_8).replace("\r", "").getBytes(StandardCharsets.UTF_8);
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(lf));
	}

	record Options(Path dataset, Path out, int runs, Duration timeout, String datasetCommit, List<String> command,
			boolean smoke) {

		static Options parse(String[] args) {
			Path dataset = null;
			Path out = Path.of("target", "ai-eval");
			int runs = 3;
			Duration timeout = MEASUREMENT_TIMEOUT;
			String commit = null;
			List<String> command = new ArrayList<>();
			boolean smoke = false;
			for (int i = 0; i < args.length; i++) {
				switch (args[i]) {
					case "--dataset" -> dataset = Path.of(value(args, ++i));
					case "--out" -> out = Path.of(value(args, ++i));
					case "--runs" -> runs = Integer.parseInt(value(args, ++i));
					case "--timeout" -> timeout = Duration.parse("PT" + value(args, ++i).toUpperCase(Locale.ROOT));
					case "--dataset-commit" -> commit = value(args, ++i);
					case "--command" -> command.add(value(args, ++i));
					case "--smoke" -> smoke = true;
					default -> throw new IllegalArgumentException("unknown option " + args[i]);
				}
			}
			if (!smoke && dataset == null) {
				throw new IllegalArgumentException("--dataset is required");
			}
			if (runs < 1) {
				throw new IllegalArgumentException("--runs must be at least 1");
			}
			return new Options(dataset, out, runs, timeout, commit, command.isEmpty() ? List.of("claude") : List.copyOf(command), smoke);
		}

		private static String value(String[] args, int i) {
			if (i >= args.length) {
				throw new IllegalArgumentException("option " + args[i - 1] + " needs a value");
			}
			return args[i];
		}
	}
}
