package pt.isep.escala.elegibilidadeworker.extracao;

import java.util.List;
import java.util.Objects;

/**
 * Zero or more periods. An empty list means nothing usable was found and the form is shown.
 *
 * <p>{@code outcome} says whether the extractor gave an answer at all: {@code OK} (with or without periods),
 * {@code NO_VALID_OUTPUT} (the model answered, but not with valid structured output), {@code TIMEOUT} or {@code ERROR}.
 * Only {@code OK} may carry periods. {@code reason} is a short code for why there are no periods when the extractor
 * did not give them (for example {@code IS_ERROR}, {@code OUT_OF_WINDOW}); it never holds text from the message or
 * from the model. {@code costUsd} is the estimated cost of the call, when a model reports one; {@code model} names the
 * model or parser that answered, when known.
 */
public record ExtractionResult(List<UnavailabilityPeriod> periods, Outcome outcome, String reason, Double costUsd,
		String model) {

	public enum Outcome {
		OK, NO_VALID_OUTPUT, TIMEOUT, ERROR
	}

	public ExtractionResult {
		periods = List.copyOf(periods);
		Objects.requireNonNull(outcome, "outcome");
		if (outcome != Outcome.OK && !periods.isEmpty()) {
			throw new IllegalArgumentException("only an OK result carries periods");
		}
	}

	/** An answer from an extractor that reports no cost and no model (the rule-based baseline). */
	public ExtractionResult(List<UnavailabilityPeriod> periods) {
		this(periods, Outcome.OK, null, null, null);
	}

	public static ExtractionResult empty() {
		return new ExtractionResult(List.of());
	}

	/** No answer: the form is shown. */
	public static ExtractionResult failed(Outcome outcome, String reason) {
		return new ExtractionResult(List.of(), outcome, reason, null, null);
	}
}
