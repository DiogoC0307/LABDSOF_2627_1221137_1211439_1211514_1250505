package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult.Outcome;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityPeriod;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TC-SCRUM-38-09: the measures SCRUM-38 adds to the report, on hand-made answers: p50 and p95 latency, the share of
 * calls slower than 4 s and 10 s, calls with no valid structured output, timeouts, errors, cost and model.
 */
class ModelCallMeasuresTest {

	private static final ObjectMapper MAPPER = JsonMapper.builder().build();
	private static final LocalDate REF = LocalDate.parse("2026-10-26");

	private static EvaluationReport report() {
		var friday = UnavailabilityPeriod.ofDays(LocalDate.parse("2026-10-30"), LocalDate.parse("2026-10-30"));
		var cases = List.of(
				new EvaluationCase("n1", "normal", "a", REF, List.of(friday), null),
				new EvaluationCase("n2", "normal", "b", REF, List.of(friday), null),
				new EvaluationCase("d1", "difficult", "c", REF, List.of(friday), null),
				new EvaluationCase("a1", "adverse", "d", REF, List.of(), null));
		Map<String, ExtractionResult> answers = Map.of(
				"a", new ExtractionResult(List.of(friday), Outcome.OK, null, 0.002, "claude-haiku-x"),
				"b", new ExtractionResult(List.of(), Outcome.NO_VALID_OUTPUT, "IS_ERROR", 0.001, "claude-haiku-x"),
				"c", ExtractionResult.failed(Outcome.TIMEOUT, "TIMEOUT"),
				"d", new ExtractionResult(List.of(), Outcome.OK, "OUT_OF_WINDOW", 0.003, "claude-haiku-x"));
		return new EvaluationScorer((text, ref) -> answers.get(text), "model test", EvaluationReport.MODEL_LATENCY_NOTE)
				.score(cases).withDataset("abc1234", "f".repeat(64));
	}

	@Test
	@DisplayName("TC-SCRUM-38-09 outcomes, reasons, cost and model are counted per call")
	void counts() {
		EvaluationReport report = report();
		assertThat(report.outcomes()).containsEntry(Outcome.OK, 2L).containsEntry(Outcome.NO_VALID_OUTPUT, 1L)
				.containsEntry(Outcome.TIMEOUT, 1L).containsEntry(Outcome.ERROR, 0L);
		assertThat(report.reasons()).containsExactly(Map.entry("IS_ERROR", 1L), Map.entry("OUT_OF_WINDOW", 1L),
				Map.entry("TIMEOUT", 1L));
		assertThat(report.costsUsd()).containsExactly(0.002, 0.001, 0.003);
		assertThat(report.models()).containsExactly("claude-haiku-x");
		// the accuracy measures are those of TC-SCRUM-99-10: a wrong answer is wrong whatever its outcome
		assertThat(report.category("normal").exactRate()).isEqualTo(0.5);
		assertThat(report.category("adverse").exactRate()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("TC-SCRUM-38-09 p50 is at most p95, and the shares slower than a limit are between 0 and 1")
	void latency() {
		EvaluationReport report = report();
		assertThat(report.p50LatencyMs()).isLessThanOrEqualTo(report.p95LatencyMs());
		assertThat(report.shareSlowerThan(-1)).isEqualTo(1.0);
		assertThat(report.shareSlowerThan(1e9)).isZero();
	}

	@Test
	@DisplayName("TC-SCRUM-38-09 the JSON and Markdown reports show the calls, the cost and the latency note of a model")
	void reports() {
		EvaluationReport report = report();
		JsonNode calls = MAPPER.readTree(report.toJson(MAPPER)).get("calls");
		assertThat(calls.get("outcomes").get("NO_VALID_OUTPUT").asInt()).isEqualTo(1);
		assertThat(calls.get("outcomes").get("TIMEOUT").asInt()).isEqualTo(1);
		assertThat(calls.get("costUsd").get("callsWithCost").asInt()).isEqualTo(3);
		assertThat(calls.get("costUsd").get("total").asDouble()).isCloseTo(0.006, org.assertj.core.data.Offset.offset(1e-9));
		assertThat(calls.has("p50LatencyMs")).isTrue();
		assertThat(calls.has("shareOver4s")).isTrue();
		assertThat(calls.has("shareOver10s")).isTrue();

		String markdown = report.toMarkdown();
		assertThat(markdown).contains("# Evaluation results: model test")
				.contains("starts the Claude Code CLI as a new process")
				.contains("| Calls with no valid structured output | 1 |").contains("| Timeouts | 1 |")
				.contains("Slower than 4 s").contains("Slower than 10 s").contains("USD on average over 3 calls")
				.contains("(NO_VALID_OUTPUT: IS_ERROR)").contains("(TIMEOUT: TIMEOUT)");
	}
}
