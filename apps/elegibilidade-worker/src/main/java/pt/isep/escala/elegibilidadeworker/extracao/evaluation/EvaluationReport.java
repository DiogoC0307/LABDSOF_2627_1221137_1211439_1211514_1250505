package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import java.util.List;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult;
import tools.jackson.databind.ObjectMapper;

/** The measures of 06 · Responsible AI Opportunity Assessment, per category and overall. Not implemented yet. */
public record EvaluationReport(String parserVersion, String datasetCommit, String datasetSha256,
		List<CaseResult> results, double p95LatencyMs) {

	/** The outcome of one case. */
	public record CaseResult(EvaluationCase testCase, ExtractionResult actual, boolean exact, boolean datesRight,
			boolean countRight, double latencyMs) {
	}

	/** Counts for one category (or all). {@code datesRight} is counted over the cases that expect periods. */
	public record Stats(int cases, int exact, int casesWithPeriods, int datesRight, int countRight) {

		public double exactRate() {
			throw new UnsupportedOperationException("not implemented yet");
		}

		public double datesRightRate() {
			throw new UnsupportedOperationException("not implemented yet");
		}

		public double countRightRate() {
			throw new UnsupportedOperationException("not implemented yet");
		}
	}

	public Stats overall() {
		throw new UnsupportedOperationException("not implemented yet");
	}

	public Stats category(String category) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	public EvaluationReport withDataset(String commit, String sha256) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	public String toJson(ObjectMapper mapper) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	public String toMarkdown() {
		throw new UnsupportedOperationException("not implemented yet");
	}
}
