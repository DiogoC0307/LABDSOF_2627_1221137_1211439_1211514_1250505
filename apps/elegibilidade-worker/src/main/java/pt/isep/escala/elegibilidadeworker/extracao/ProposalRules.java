package pt.isep.escala.elegibilidadeworker.extracao;

import java.time.LocalDate;
import java.util.List;

/**
 * The rules every extractor applies, whatever reads the text (06 · Responsible AI Opportunity Assessment, "Expected
 * outputs" and "Difficult or adverse cases"). The rule-based baseline and the model use the same code, so they are
 * judged by the same limits.
 */
public final class ProposalRules {

	/** 06: "Very long: above 500 characters, rejected and the form shown". */
	public static final int MAX_LENGTH = 500;

	/** 06: dates "more than 6 months ahead" are rejected. */
	public static final int MAX_MONTHS_AHEAD = 6;

	private ProposalRules() {
	}

	/** Empty input gets no extraction at all (06: "no model call and the form is shown"), and neither does long input. */
	public static boolean isUsableInput(String text) {
		return text != null && !text.isBlank() && text.length() <= MAX_LENGTH;
	}

	/**
	 * True when every period starts on or after the reference date and ends at most 6 months after it. When one period
	 * does not, the WHOLE proposal is rejected and the form is shown (06; decision of 2026-11-22): a proposal that is
	 * partly right could mislead the member.
	 */
	public static boolean withinWindow(List<UnavailabilityPeriod> periods, LocalDate referenceDate) {
		LocalDate latest = referenceDate.plusMonths(MAX_MONTHS_AHEAD);
		return periods.stream()
				.allMatch(p -> !p.startDate().isBefore(referenceDate) && !p.endDate().isAfter(latest));
	}
}
