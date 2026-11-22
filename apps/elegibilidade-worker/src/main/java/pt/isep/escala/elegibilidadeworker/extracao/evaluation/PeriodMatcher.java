package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import java.util.List;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityPeriod;

/** Compares expected and actual periods for one message. Not implemented yet. */
public final class PeriodMatcher {

	private PeriodMatcher() {
	}

	/** All fields of all periods match, in any order. */
	public static boolean exact(List<UnavailabilityPeriod> expected, List<UnavailabilityPeriod> actual) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	/** Start and end date of every expected period appear in the actual periods. */
	public static boolean datesRight(List<UnavailabilityPeriod> expected, List<UnavailabilityPeriod> actual) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	/** None missed, none invented. */
	public static boolean countRight(List<UnavailabilityPeriod> expected, List<UnavailabilityPeriod> actual) {
		throw new UnsupportedOperationException("not implemented yet");
	}
}
