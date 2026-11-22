package pt.isep.escala.elegibilidadeworker.extracao;

import java.time.LocalDate;

/** Turns a member's free text into unavailability periods, resolving relative dates against {@code referenceDate}. */
public interface UnavailabilityExtractor {

	ExtractionResult extract(String text, LocalDate referenceDate);
}
