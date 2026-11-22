package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityPeriod;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The measures of 06 · Responsible AI Opportunity Assessment ("Acceptance criteria", "Relevant quality measures") for
 * one run of one extractor, overall and per category, with the targets next to them and the cases that failed.
 *
 * <p>The targets are reported, never enforced: a run below target is a result, not a failed build.
 */
public record EvaluationReport(String parserVersion, String datasetCommit, String datasetSha256,
		List<CaseResult> results, double p95LatencyMs) {

	/** Targets fixed on 2026-10-25, before any measurement (06). */
	public static final double TARGET_EXACT_NORMAL = 0.85;
	public static final double TARGET_DATES_NORMAL = 0.95;
	public static final double TARGET_COUNT = 0.95;
	public static final double TARGET_ADVERSE = 1.0;
	public static final double TARGET_P95_MS = 4000;

	public static final String LATENCY_NOTE = "Time of one extract call, in ms (same process, no network). No model is "
			+ "called, so this is a floor and not comparable with a hosted model.";

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	/** The outcome of one case. {@code error} is the type of an exception thrown by the extractor, if any. */
	public record CaseResult(EvaluationCase testCase, ExtractionResult actual, boolean exact, boolean datesRight,
			boolean countRight, double latencyMs, String error) {
	}

	/** Counts for one category (or all). {@code datesRight} is counted over the cases that expect periods. */
	public record Stats(int cases, int exact, int casesWithPeriods, int datesRight, int countRight) {

		public double exactRate() {
			return rate(exact, cases);
		}

		public double datesRightRate() {
			return rate(datesRight, casesWithPeriods);
		}

		public double countRightRate() {
			return rate(countRight, cases);
		}

		private static double rate(int part, int whole) {
			return whole == 0 ? Double.NaN : (double) part / whole;
		}
	}

	public EvaluationReport {
		results = List.copyOf(results);
	}

	public Stats overall() {
		return stats(results);
	}

	public Stats category(String category) {
		return stats(results.stream().filter(r -> r.testCase().category().equals(category)).toList());
	}

	/** The same report with the dataset it was run on: its git commit and the SHA-256 of the file. */
	public EvaluationReport withDataset(String commit, String sha256) {
		return new EvaluationReport(parserVersion, commit, sha256, results, p95LatencyMs);
	}

	private static Stats stats(List<CaseResult> selected) {
		int withPeriods = 0;
		int exact = 0;
		int dates = 0;
		int count = 0;
		for (CaseResult r : selected) {
			exact += r.exact() ? 1 : 0;
			count += r.countRight() ? 1 : 0;
			if (!r.testCase().expectedPeriods().isEmpty()) {
				withPeriods++;
				dates += r.datesRight() ? 1 : 0;
			}
		}
		return new Stats(selected.size(), exact, withPeriods, dates, count);
	}

	private List<CaseResult> failures() {
		return results.stream().filter(r -> !r.exact()).toList();
	}

	// ---- JSON -----------------------------------------------------------------------------------------------

	public String toJson(ObjectMapper mapper) {
		ObjectNode root = mapper.createObjectNode();
		root.put("parserVersion", parserVersion);
		putOrNull(root, "datasetCommit", datasetCommit);
		putOrNull(root, "datasetSha256", datasetSha256);
		root.put("latencyNote", LATENCY_NOTE);
		root.put("p95LatencyMs", round(p95LatencyMs));
		ObjectNode targets = root.putObject("targets");
		targets.put("exactNormal", TARGET_EXACT_NORMAL);
		targets.put("datesNormal", TARGET_DATES_NORMAL);
		targets.put("countAll", TARGET_COUNT);
		targets.put("adverse", TARGET_ADVERSE);
		targets.put("p95LatencyMs", TARGET_P95_MS);
		root.set("overall", statsNode(mapper, overall()));
		ObjectNode categories = root.putObject("categories");
		for (String category : List.of("normal", "difficult", "adverse")) {
			categories.set(category, statsNode(mapper, category(category)));
		}
		ArrayNode failed = root.putArray("failures");
		for (CaseResult r : failures()) {
			ObjectNode node = failed.addObject();
			node.put("id", r.testCase().id());
			node.put("category", r.testCase().category());
			node.put("input", r.testCase().input());
			node.put("referenceDate", r.testCase().referenceDate().toString());
			node.set("expected", periodsNode(mapper, r.testCase().expectedPeriods()));
			node.set("actual", periodsNode(mapper, r.actual().periods()));
			putOrNull(node, "error", r.error());
		}
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
	}

	private static ObjectNode statsNode(ObjectMapper mapper, Stats stats) {
		ObjectNode node = mapper.createObjectNode();
		node.put("cases", stats.cases());
		node.put("exact", stats.exact());
		node.put("casesWithPeriods", stats.casesWithPeriods());
		node.put("datesRight", stats.datesRight());
		node.put("countRight", stats.countRight());
		putRate(node, "exactRate", stats.exactRate());
		putRate(node, "datesRightRate", stats.datesRightRate());
		putRate(node, "countRightRate", stats.countRightRate());
		return node;
	}

	private static ArrayNode periodsNode(ObjectMapper mapper, List<UnavailabilityPeriod> periods) {
		ArrayNode array = mapper.createArrayNode();
		for (UnavailabilityPeriod p : periods) {
			ObjectNode node = array.addObject();
			node.put("startDate", p.startDate().toString());
			node.put("endDate", p.endDate().toString());
			if (!p.weekdays().isEmpty()) {
				ArrayNode days = node.putArray("weekdays");
				p.weekdays().forEach(d -> days.add(d.name().substring(0, 3)));
			}
			if (p.startTime() != null) {
				node.put("startTime", p.startTime().format(TIME));
				node.put("endTime", p.endTime().format(TIME));
			}
		}
		return array;
	}

	private static void putOrNull(ObjectNode node, String field, String value) {
		if (value == null) {
			node.putNull(field);
		}
		else {
			node.put(field, value);
		}
	}

	private static void putRate(ObjectNode node, String field, double rate) {
		if (Double.isNaN(rate)) {
			node.putNull(field);
		}
		else {
			node.put(field, rate);
		}
	}

	private static double round(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}

	// ---- Markdown -------------------------------------------------------------------------------------------

	public String toMarkdown() {
		Stats overall = overall();
		Stats normal = category("normal");
		Stats adverse = category("adverse");
		StringBuilder md = new StringBuilder();
		md.append("# Baseline results: rule-based parser ").append(parserVersion).append("\n\n");
		md.append("- **Dataset:** ").append(overall.cases()).append(" cases; commit `")
				.append(datasetCommit == null ? "not recorded" : datasetCommit).append("`; SHA-256 `")
				.append(datasetSha256 == null ? "not recorded" : datasetSha256).append("`.\n");
		md.append("- **Parser:** ").append(parserVersion).append(". No AI, no network.\n");
		md.append("- **Latency:** ").append(LATENCY_NOTE).append("\n");
		md.append("- **Cost per message:** not applicable (no model, no tokens).\n\n");

		md.append("## Measures against the targets of 06 · Responsible AI Opportunity Assessment\n\n");
		md.append("| Measure | Target | Result | Met |\n|---|---|---|---|\n");
		row(md, "Periods exactly right (normal cases)", "≥ 85%", normal.exactRate(), normal.exact(), normal.cases(),
				TARGET_EXACT_NORMAL);
		row(md, "Dates right (normal cases)", "≥ 95%", normal.datesRightRate(), normal.datesRight(),
				normal.casesWithPeriods(), TARGET_DATES_NORMAL);
		row(md, "Right number of periods (all cases)", "≥ 95%", overall.countRightRate(), overall.countRight(),
				overall.cases(), TARGET_COUNT);
		row(md, "Adverse handled (adverse cases)", "100%", adverse.exactRate(), adverse.exact(), adverse.cases(),
				TARGET_ADVERSE);
		md.append("| p95 latency | < 4 s | ").append(String.format(Locale.ROOT, "%.3f ms", p95LatencyMs)).append(" | ")
				.append(p95LatencyMs < TARGET_P95_MS ? "yes" : "no").append(" |\n\n");

		md.append("## Per category\n\n");
		md.append("| Category | Cases | Exact | Dates right | Right number |\n|---|---|---|---|---|\n");
		for (String category : List.of("normal", "difficult", "adverse")) {
			Stats s = category(category);
			md.append("| ").append(category).append(" | ").append(s.cases()).append(" | ")
					.append(fraction(s.exact(), s.cases())).append(" | ")
					.append(s.casesWithPeriods() == 0 ? "n/a (no expected periods)"
							: fraction(s.datesRight(), s.casesWithPeriods()))
					.append(" | ").append(fraction(s.countRight(), s.cases())).append(" |\n");
		}
		md.append("| **all** | ").append(overall.cases()).append(" | ").append(fraction(overall.exact(), overall.cases()))
				.append(" | ").append(fraction(overall.datesRight(), overall.casesWithPeriods())).append(" | ")
				.append(fraction(overall.countRight(), overall.cases())).append(" |\n\n");
		md.append("Dates right is counted over the cases that expect at least one period.\n\n");

		List<CaseResult> failed = failures();
		md.append("## Cases not exactly right (").append(failed.size()).append(")\n\n");
		if (!failed.isEmpty()) {
			md.append("| Case | Category | Input | Expected | Actual |\n|---|---|---|---|---|\n");
			for (CaseResult r : failed) {
				md.append("| `").append(r.testCase().id()).append("` | ").append(r.testCase().category()).append(" | ")
						.append(cell(r.testCase().input())).append(" (").append(r.testCase().referenceDate()).append(") | ")
						.append(cell(describe(r.testCase().expectedPeriods()))).append(" | ")
						.append(cell(describe(r.actual().periods()) + (r.error() == null ? "" : " (error: " + r.error() + ")")))
						.append(" |\n");
			}
		}
		return md.toString();
	}

	private static void row(StringBuilder md, String measure, String target, double rate, int part, int whole,
			double threshold) {
		md.append("| ").append(measure).append(" | ").append(target).append(" | ")
				.append(String.format(Locale.ROOT, "%.1f%% (%d/%d)", rate * 100, part, whole)).append(" | ")
				.append(rate >= threshold ? "yes" : "no").append(" |\n");
	}

	private static String fraction(int part, int whole) {
		return whole == 0 ? "n/a" : String.format(Locale.ROOT, "%.1f%% (%d/%d)", 100.0 * part / whole, part, whole);
	}

	private static String cell(String text) {
		return text.replace("|", "\\|").replace("\n", " ");
	}

	/** Compact notation: {@code START[..END] [MON,WED] [HH:mm-HH:mm]}, periods separated by {@code ;}; none is "-". */
	static String describe(List<UnavailabilityPeriod> periods) {
		if (periods.isEmpty()) {
			return "-";
		}
		return periods.stream().map(p -> {
			StringBuilder sb = new StringBuilder(p.startDate().toString());
			if (!p.endDate().equals(p.startDate())) {
				sb.append("..").append(p.endDate());
			}
			if (!p.weekdays().isEmpty()) {
				sb.append(" [").append(p.weekdays().stream().map(d -> d.name().substring(0, 3))
						.collect(Collectors.joining(","))).append("]");
			}
			if (p.startTime() != null) {
				sb.append(' ').append(p.startTime().format(TIME)).append('-').append(p.endTime().format(TIME));
			}
			return sb.toString();
		}).collect(Collectors.joining(" ; "));
	}
}
