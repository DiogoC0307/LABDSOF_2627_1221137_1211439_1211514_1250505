package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import java.util.List;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityExtractor;

/** Runs an extractor over the cases and scores it. Not implemented yet. */
public final class EvaluationScorer {

	public EvaluationScorer(UnavailabilityExtractor extractor, String parserVersion) {
	}

	public EvaluationReport score(List<EvaluationCase> cases) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	/** Nearest-rank percentile of an ascending array; {@code fraction} is 0.95 for p95. */
	public static double percentile(double[] ascending, double fraction) {
		throw new UnsupportedOperationException("not implemented yet");
	}
}
