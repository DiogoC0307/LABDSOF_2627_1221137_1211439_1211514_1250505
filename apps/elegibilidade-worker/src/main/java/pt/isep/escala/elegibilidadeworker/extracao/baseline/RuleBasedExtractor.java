package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import java.time.LocalDate;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityExtractor;

/** The non-AI baseline of 06 · Responsible AI Opportunity Assessment (SCRUM-99). Not implemented yet. */
public final class RuleBasedExtractor implements UnavailabilityExtractor {

	public RuleBasedExtractor() {
		this(DayParts.DEFAULT);
	}

	public RuleBasedExtractor(DayParts dayParts) {
	}

	@Override
	public ExtractionResult extract(String text, LocalDate referenceDate) {
		throw new UnsupportedOperationException("not implemented yet");
	}
}
