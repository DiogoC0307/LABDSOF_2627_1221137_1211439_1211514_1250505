package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityPeriod;
import tools.jackson.databind.ObjectMapper;

/** One line of the evaluation dataset (JSONL). Not implemented yet. */
public record EvaluationCase(String id, String category, String input, LocalDate referenceDate,
		List<UnavailabilityPeriod> expectedPeriods, String note) {

	public static EvaluationCase fromJson(String line, ObjectMapper mapper) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	public static List<EvaluationCase> readAll(InputStream jsonl, ObjectMapper mapper) throws IOException {
		throw new UnsupportedOperationException("not implemented yet");
	}
}
