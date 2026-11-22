package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityExtractor;
import pt.isep.escala.elegibilidadeworker.extracao.evaluation.EvaluationReport.CaseResult;

/**
 * Runs an extractor once over each case and scores it with the measures of 06 · Responsible AI Opportunity Assessment.
 * The same scorer can score the AI extractor of SCRUM-38, so both are measured the same way.
 *
 * <p>Latency is the time of the call to {@link UnavailabilityExtractor#extract}, in the same process. For the
 * rule-based baseline there is no network, so it is a floor, not a comparison with a model behind an API.
 */
public final class EvaluationScorer {

	private final UnavailabilityExtractor extractor;
	private final String parserVersion;

	public EvaluationScorer(UnavailabilityExtractor extractor, String parserVersion) {
		this.extractor = Objects.requireNonNull(extractor, "extractor");
		this.parserVersion = Objects.requireNonNull(parserVersion, "parserVersion");
	}

	public EvaluationReport score(List<EvaluationCase> cases) {
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
				actual = ExtractionResult.empty();
				error = e.getClass().getSimpleName();
			}
			double millis = (System.nanoTime() - started) / 1_000_000.0;
			latencies[i] = millis;
			results.add(new CaseResult(testCase, actual,
					PeriodMatcher.exact(testCase.expectedPeriods(), actual.periods()),
					PeriodMatcher.datesRight(testCase.expectedPeriods(), actual.periods()),
					PeriodMatcher.countRight(testCase.expectedPeriods(), actual.periods()), millis, error));
		}
		Arrays.sort(latencies);
		return new EvaluationReport(parserVersion, null, null, results, percentile(latencies, 0.95));
	}

	/** Nearest-rank percentile of an ascending array; {@code fraction} is 0.95 for p95. 0 for an empty array. */
	public static double percentile(double[] ascending, double fraction) {
		if (ascending.length == 0) {
			return 0;
		}
		int rank = (int) Math.ceil(fraction * ascending.length);
		return ascending[Math.min(Math.max(rank, 1), ascending.length) - 1];
	}
}
