package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Where "manhã", "tarde" and "noite" start, as configuration (ADR 0014: operational rules are configuration, not
 * constants). The defaults are the conventions of the evaluation dataset: morning 08:00-14:00, afternoon 14:00-20:00,
 * night 20:00-08:00 (it crosses midnight).
 */
public record DayParts(LocalTime morningStart, LocalTime afternoonStart, LocalTime nightStart) {

	public static final DayParts DEFAULT = new DayParts(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0));

	public enum Part {
		MORNING, AFTERNOON, NIGHT
	}

	/** A time window; it crosses midnight when {@code end} is before {@code start}. */
	public record Window(LocalTime start, LocalTime end) {
	}

	public DayParts {
		Objects.requireNonNull(morningStart, "morningStart");
		Objects.requireNonNull(afternoonStart, "afternoonStart");
		Objects.requireNonNull(nightStart, "nightStart");
		if (!morningStart.isBefore(afternoonStart) || !afternoonStart.isBefore(nightStart)) {
			throw new IllegalArgumentException("expected morningStart < afternoonStart < nightStart");
		}
	}

	public Window window(Part part) {
		return switch (part) {
			case MORNING -> new Window(morningStart, afternoonStart);
			case AFTERNOON -> new Window(afternoonStart, nightStart);
			case NIGHT -> new Window(nightStart, morningStart);
		};
	}
}
