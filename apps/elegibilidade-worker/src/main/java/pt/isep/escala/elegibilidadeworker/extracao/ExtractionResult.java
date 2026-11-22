package pt.isep.escala.elegibilidadeworker.extracao;

import java.util.List;

/** Zero or more periods. An empty list means nothing usable was found and the form is shown. */
public record ExtractionResult(List<UnavailabilityPeriod> periods) {

	public ExtractionResult {
		periods = List.copyOf(periods);
	}

	public static ExtractionResult empty() {
		return new ExtractionResult(List.of());
	}
}
