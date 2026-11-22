package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityPeriod;

/**
 * Compares the expected and the actual periods of one message, with the three measures of 06 · Responsible AI
 * Opportunity Assessment ("Relevant quality measures"). Order never matters; repeated periods do.
 */
public final class PeriodMatcher {

	private record Dates(LocalDate start, LocalDate end) {
	}

	private PeriodMatcher() {
	}

	/** Exact match per message: all fields of all periods match. */
	public static boolean exact(List<UnavailabilityPeriod> expected, List<UnavailabilityPeriod> actual) {
		return counts(expected, Function.identity()).equals(counts(actual, Function.identity()));
	}

	/** Date accuracy: the start and end of every expected period are found among the actual periods. */
	public static boolean datesRight(List<UnavailabilityPeriod> expected, List<UnavailabilityPeriod> actual) {
		Function<UnavailabilityPeriod, Dates> dates = p -> new Dates(p.startDate(), p.endDate());
		Map<Dates, Long> available = counts(actual, dates);
		for (Map.Entry<Dates, Long> wanted : counts(expected, dates).entrySet()) {
			if (available.getOrDefault(wanted.getKey(), 0L) < wanted.getValue()) {
				return false;
			}
		}
		return true;
	}

	/** Period count: none missed, none invented. */
	public static boolean countRight(List<UnavailabilityPeriod> expected, List<UnavailabilityPeriod> actual) {
		return expected.size() == actual.size();
	}

	private static <K> Map<K, Long> counts(List<UnavailabilityPeriod> periods, Function<UnavailabilityPeriod, K> key) {
		return periods.stream().collect(Collectors.groupingBy(key, HashMap::new, Collectors.counting()));
	}
}
