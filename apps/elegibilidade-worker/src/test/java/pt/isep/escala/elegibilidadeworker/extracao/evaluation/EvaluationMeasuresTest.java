package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityPeriod;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TC-SCRUM-99-10. The evaluator, on pairs made by hand (never on the dataset): the measures of 06 · Responsible AI
 * Opportunity Assessment, "Relevant quality measures": exact match per message, date accuracy, period count, results
 * per category, p95 latency.
 */
class EvaluationMeasuresTest {

	private static final ObjectMapper MAPPER = JsonMapper.builder().build();
	private static final LocalDate REF = LocalDate.parse("2026-10-26");

	private static UnavailabilityPeriod day(String date) {
		return UnavailabilityPeriod.ofDays(LocalDate.parse(date), LocalDate.parse(date));
	}

	private static UnavailabilityPeriod morning(String date) {
		LocalDate d = LocalDate.parse(date);
		return new UnavailabilityPeriod(d, d, Set.of(), LocalTime.of(8, 0), LocalTime.of(14, 0));
	}

	@Test
	@DisplayName("TC-SCRUM-99-10 exact match needs every field of every period, in any order")
	void exactMatch() {
		var friday = day("2026-10-30");
		var saturday = day("2026-10-31");
		assertThat(PeriodMatcher.exact(List.of(friday, saturday), List.of(saturday, friday))).isTrue();
		assertThat(PeriodMatcher.exact(List.of(), List.of())).isTrue();
		// a time is missing
		assertThat(PeriodMatcher.exact(List.of(morning("2026-10-30")), List.of(friday))).isFalse();
		// a different weekday pattern
		var sundays = new UnavailabilityPeriod(REF, LocalDate.parse("2026-11-30"), Set.of(DayOfWeek.SUNDAY), null, null);
		var mondays = new UnavailabilityPeriod(REF, LocalDate.parse("2026-11-30"), Set.of(DayOfWeek.MONDAY), null, null);
		assertThat(PeriodMatcher.exact(List.of(sundays), List.of(mondays))).isFalse();
		// one missed, one invented
		assertThat(PeriodMatcher.exact(List.of(friday, saturday), List.of(friday))).isFalse();
		assertThat(PeriodMatcher.exact(List.of(friday), List.of(friday, saturday))).isFalse();
		// the same period twice is not the same as once
		assertThat(PeriodMatcher.exact(List.of(friday), List.of(friday, friday))).isFalse();
	}

	@Test
	@DisplayName("TC-SCRUM-99-10 dates right looks at start and end only; every expected period must be found")
	void datesRight() {
		var friday = day("2026-10-30");
		var saturday = day("2026-10-31");
		// right dates, wrong times
		assertThat(PeriodMatcher.datesRight(List.of(morning("2026-10-30")), List.of(friday))).isTrue();
		// end one day off
		var range = UnavailabilityPeriod.ofDays(LocalDate.parse("2026-11-03"), LocalDate.parse("2026-11-07"));
		var shortRange = UnavailabilityPeriod.ofDays(LocalDate.parse("2026-11-03"), LocalDate.parse("2026-11-06"));
		assertThat(PeriodMatcher.datesRight(List.of(range), List.of(shortRange))).isFalse();
		// one expected period is missing
		assertThat(PeriodMatcher.datesRight(List.of(friday, saturday), List.of(friday))).isFalse();
		// an invented extra period does not change the dates; the period count catches it
		assertThat(PeriodMatcher.datesRight(List.of(friday), List.of(friday, saturday))).isTrue();
		// two expected periods with the same dates need two actual ones
		assertThat(PeriodMatcher.datesRight(List.of(friday, morning("2026-10-30")), List.of(friday))).isFalse();
	}

	@Test
	@DisplayName("TC-SCRUM-99-10 the period count is right when none is missed and none is invented")
	void countRight() {
		var friday = day("2026-10-30");
		var saturday = day("2026-10-31");
		assertThat(PeriodMatcher.countRight(List.of(friday, saturday), List.of(saturday, day("2026-11-10")))).isTrue();
		assertThat(PeriodMatcher.countRight(List.of(), List.of())).isTrue();
		assertThat(PeriodMatcher.countRight(List.of(friday, saturday), List.of(friday))).isFalse();
		assertThat(PeriodMatcher.countRight(List.of(), List.of(friday))).isFalse();
	}

	private static EvaluationCase caseOf(String id, String category, String input, UnavailabilityPeriod... expected) {
		return new EvaluationCase(id, category, input, REF, List.of(expected), null);
	}

	/** Five hand-made cases and an extractor that gives a fixed answer for each. */
	private static EvaluationReport scoreHandMadeCases() {
		var friday = day("2026-10-30");
		var saturday = day("2026-10-31");
		var cases = List.of(
				caseOf("n1", "normal", "a", friday),
				caseOf("n2", "normal", "b", morning("2026-10-30")),
				caseOf("d1", "difficult", "c", friday, saturday),
				caseOf("a1", "adverse", "d"),
				caseOf("a2", "adverse", "e"));
		Map<String, List<UnavailabilityPeriod>> answers = Map.of(
				"a", List.of(friday),
				"b", List.of(friday),
				"c", List.of(friday),
				"d", List.of(),
				"e", List.of(friday));
		return new EvaluationScorer((text, ref) -> new ExtractionResult(answers.get(text)), "test").score(cases);
	}

	@Test
	@DisplayName("TC-SCRUM-99-10 the scorer reports each measure per category and overall")
	void scoresPerCategory() {
		EvaluationReport report = scoreHandMadeCases();

		var normal = report.category("normal");
		assertThat(normal.cases()).isEqualTo(2);
		assertThat(normal.exactRate()).isEqualTo(0.5);
		assertThat(normal.datesRightRate()).isEqualTo(1.0);
		assertThat(normal.countRightRate()).isEqualTo(1.0);

		var difficult = report.category("difficult");
		assertThat(difficult.exactRate()).isZero();
		assertThat(difficult.datesRightRate()).isZero();
		assertThat(difficult.countRightRate()).isZero();

		// "adverse handled" is the exact rate of the adverse cases; they expect no period, so dates right does not apply
		var adverse = report.category("adverse");
		assertThat(adverse.exactRate()).isEqualTo(0.5);
		assertThat(adverse.datesRightRate()).isNaN();
		assertThat(adverse.countRightRate()).isEqualTo(0.5);

		var overall = report.overall();
		assertThat(overall.cases()).isEqualTo(5);
		assertThat(overall.exact()).isEqualTo(2);
		assertThat(overall.casesWithPeriods()).isEqualTo(3);
		assertThat(overall.datesRight()).isEqualTo(2);
		assertThat(overall.countRight()).isEqualTo(3);

		assertThat(report.results()).extracting(r -> r.testCase().id()).containsExactly("n1", "n2", "d1", "a1", "a2");
		assertThat(report.results()).extracting(EvaluationReport.CaseResult::exact)
				.containsExactly(true, false, false, true, false);
		assertThat(report.results()).allSatisfy(r -> assertThat(r.latencyMs()).isGreaterThanOrEqualTo(0));
		assertThat(report.p95LatencyMs()).isGreaterThanOrEqualTo(0);
	}

	@Test
	@DisplayName("TC-SCRUM-99-10 p95 latency is the nearest-rank percentile")
	void percentile() {
		double[] twenty = new double[20];
		double[] sixty = new double[60];
		for (int i = 0; i < twenty.length; i++) {
			twenty[i] = i + 1;
		}
		for (int i = 0; i < sixty.length; i++) {
			sixty[i] = i + 1;
		}
		assertThat(EvaluationScorer.percentile(twenty, 0.95)).isEqualTo(19.0);
		assertThat(EvaluationScorer.percentile(sixty, 0.95)).isEqualTo(57.0);
		assertThat(EvaluationScorer.percentile(sixty, 1.0)).isEqualTo(60.0);
		assertThat(EvaluationScorer.percentile(new double[] { 4.2 }, 0.95)).isEqualTo(4.2);
	}

	@Test
	@DisplayName("TC-SCRUM-99-10 a case is read from one JSONL line in the format of the dataset README")
	void readsACase() {
		String line = """
				{"id":"X-01","category":"normal","input":"não posso aos domingos de manhã até dezembro","referenceDate":"2026-10-26",\
				"expected":{"periods":[{"startDate":"2026-10-26","endDate":"2026-11-30","weekdays":["SUN"],"startTime":"08:00","endTime":"14:00"}]},\
				"note":"hand-made"}""";
		EvaluationCase parsed = EvaluationCase.fromJson(line, MAPPER);
		assertThat(parsed.id()).isEqualTo("X-01");
		assertThat(parsed.category()).isEqualTo("normal");
		assertThat(parsed.input()).isEqualTo("não posso aos domingos de manhã até dezembro");
		assertThat(parsed.referenceDate()).isEqualTo(REF);
		assertThat(parsed.note()).isEqualTo("hand-made");
		assertThat(parsed.expectedPeriods()).containsExactly(new UnavailabilityPeriod(REF, LocalDate.parse("2026-11-30"),
				Set.of(DayOfWeek.SUNDAY), LocalTime.of(8, 0), LocalTime.of(14, 0)));

		String empty = "{\"id\":\"X-02\",\"category\":\"adverse\",\"input\":\"\",\"referenceDate\":\"2026-10-26\","
				+ "\"expected\":{\"periods\":[]}}";
		EvaluationCase parsedEmpty = EvaluationCase.fromJson(empty, MAPPER);
		assertThat(parsedEmpty.expectedPeriods()).isEmpty();
		assertThat(parsedEmpty.note()).isNull();
	}

	@Test
	@DisplayName("TC-SCRUM-99-10 reading several lines skips blank ones; a bad line is named by number, without its content")
	void readsLines() throws IOException {
		String good = "{\"id\":\"X-01\",\"category\":\"normal\",\"input\":\"não posso sexta\",\"referenceDate\":\"2026-10-26\","
				+ "\"expected\":{\"periods\":[{\"startDate\":\"2026-10-30\",\"endDate\":\"2026-10-30\"}]}}";
		var two = new ByteArrayInputStream((good + "\n\n" + good.replace("X-01", "X-02") + "\n")
				.getBytes(StandardCharsets.UTF_8));
		assertThat(EvaluationCase.readAll(two, MAPPER)).extracting(EvaluationCase::id).containsExactly("X-01", "X-02");

		var bad = new ByteArrayInputStream((good + "\n{\"id\": SEGREDO-NAO-E-JSON\n").getBytes(StandardCharsets.UTF_8));
		assertThatThrownBy(() -> EvaluationCase.readAll(bad, MAPPER)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("line 2").hasMessageNotContaining("SEGREDO").hasNoCause();
	}

	@Test
	@DisplayName("TC-SCRUM-99-10 the report shows the measures against the targets of 06, per category, with the failures")
	void report() {
		EvaluationReport report = scoreHandMadeCases().withDataset("abc1234", "f".repeat(64));

		String markdown = report.toMarkdown();
		assertThat(markdown).contains("test", "abc1234", "same process, no network")
				.contains("Periods exactly right", "Dates right", "Right number of periods", "Adverse handled",
						"p95 latency")
				.contains("normal", "difficult", "adverse")
				// the failures are listed, the successes are not
				.contains("n2", "d1", "a2").doesNotContain("`n1`").doesNotContain("`a1`");

		JsonNode json = MAPPER.readTree(report.toJson(MAPPER));
		assertThat(json.get("parserVersion").asString()).isEqualTo("test");
		assertThat(json.get("datasetCommit").asString()).isEqualTo("abc1234");
		assertThat(json.get("datasetSha256").asString()).hasSize(64);
		assertThat(json.get("categories").get("normal").get("exactRate").asDouble()).isEqualTo(0.5);
		assertThat(json.get("categories").get("adverse").get("exactRate").asDouble()).isEqualTo(0.5);
		assertThat(json.get("failures")).hasSize(3);
		assertThat(json.get("targets").get("exactNormal").asDouble()).isEqualTo(0.85);
	}
}
