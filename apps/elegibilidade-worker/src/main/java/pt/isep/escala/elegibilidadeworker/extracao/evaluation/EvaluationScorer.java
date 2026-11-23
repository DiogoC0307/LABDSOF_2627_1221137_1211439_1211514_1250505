package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityExtractor;
import pt.isep.escala.elegibilidadeworker.extracao.evaluation.EvaluationReport.CaseResult;

/**
 * Runs an extractor once over each case, one case at a time, and scores it with the measures of 06 · Responsible AI
 * Opportunity Assessment. The rule-based baseline (SCRUM-99) and the model (SCRUM-38) are scored by this same code.
 *
 * <p>Latency is the time of the call to {@link UnavailabilityExtractor#extract}. What it includes depends on the
 * extractor, and the report says so ({@code latencyNote}): for the baseline it is the same process with no network;
 * for the model it includes starting the CLI and the call over the network.
 */
public final class EvaluationScorer {

	private final UnavailabilityExtractor extractor;
	private final String parserVersion;
	private final String latencyNote;

	/** For the rule-based baseline: the latency note says "same process, no network". */
	public EvaluationScorer(UnavailabilityExtractor extractor, String parserVersion) {
		this(extractor, parserVersion, EvaluationReport.LATENCY_NOTE);
	}

	public EvaluationScorer(UnavailabilityExtractor extractor, String parserVersion, String latencyNote) {
		this.extractor = Objects.requireNonNull(extractor, "extractor");
		this.parserVersion = Objects.requireNonNull(parserVersion, "parserVersion");
		this.latencyNote = Objects.requireNonNull(latencyNote, "latencyNote");
	}

	public EvaluationReport score(List<EvaluationCase> cases) {
		return score(cases, result -> {
		});
	}

	/** As {@link #score(List)}, calling {@code afterEach} after every case (for progress; it gets no input text). */
	public EvaluationReport score(List<EvaluationCase> cases, Consumer<CaseResult> afterEach) {
		List<CaseResult> results = new ArrayList<>();
		double[] latencies = new double[cases.size()];
		for (int i = 0; i < cases.size(); i++) {
			EvaluationCase testCase = cases.get(i);
			ExtractionResult actual;
			String error = null;
			long started = System.nanoTime();
			try {
				actual = extractor.extract(testCase.input(), testCase.referenceDate());
			}
			catch (RuntimeException e) {
				// a crash is a wrong answer for that message, not the end of the run; only the type is recorded
				actual = ExtractionResult.failed(ExtractionResult.Outcome.ERROR, "EXCEPTION");
				error = e.getClass().getSimpleName();
			}
			double millis = (System.nanoTime() - started) / 1_000_000.0;
			latencies[i] = millis;
			CaseResult result = new CaseResult(testCase, actual,
					PeriodMatcher.exact(testCase.expectedPeriods(), actual.periods()),
					PeriodMatcher.datesRight(testCase.expectedPeriods(), actual.periods()),
					PeriodMatcher.countRight(testCase.expectedPeriods(), actual.periods()), millis, error);
			results.add(result);
			afterEach.accept(result);
		}
		Arrays.sort(latencies);
		return new EvaluationReport(parserVersion, latencyNote, null, null, results, percentile(latencies, 0.95));
	}

	/** Nearest-rank percentile of an ascending array; {@code fraction} is 0.95 for p95. 0 for an empty array. */
	public static double percentile(double[] ascending, double fraction) {
		if (ascending.length == 0) {
			return 0;
		}
		int rank = (int) Math.ceil(fraction * ascending.length);
		return ascending[Math.clamp(rank, 1, ascending.length) - 1];
	}
}
