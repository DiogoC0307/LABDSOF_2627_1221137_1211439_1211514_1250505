package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * European Portuguese words for weekdays and months, and the text normalisation the rule-based extractor works on:
 * lower case, no accents (members often type without them), punctuation separated from words.
 */
final class PortugueseDateLexicon {

	/** A weekday word; {@code plural} is true for "domingos", "às segundas", which mean every one of them. */
	record WeekdayWord(DayOfWeek day, boolean plural) {
	}

	private static final Map<String, WeekdayWord> WEEKDAYS = Map.ofEntries(
			Map.entry("segunda", new WeekdayWord(DayOfWeek.MONDAY, false)),
			Map.entry("segundas", new WeekdayWord(DayOfWeek.MONDAY, true)),
			Map.entry("terca", new WeekdayWord(DayOfWeek.TUESDAY, false)),
			Map.entry("tercas", new WeekdayWord(DayOfWeek.TUESDAY, true)),
			Map.entry("quarta", new WeekdayWord(DayOfWeek.WEDNESDAY, false)),
			Map.entry("quartas", new WeekdayWord(DayOfWeek.WEDNESDAY, true)),
			Map.entry("quinta", new WeekdayWord(DayOfWeek.THURSDAY, false)),
			Map.entry("quintas", new WeekdayWord(DayOfWeek.THURSDAY, true)),
			Map.entry("sexta", new WeekdayWord(DayOfWeek.FRIDAY, false)),
			Map.entry("sextas", new WeekdayWord(DayOfWeek.FRIDAY, true)),
			Map.entry("sabado", new WeekdayWord(DayOfWeek.SATURDAY, false)),
			Map.entry("sabados", new WeekdayWord(DayOfWeek.SATURDAY, true)),
			Map.entry("domingo", new WeekdayWord(DayOfWeek.SUNDAY, false)),
			Map.entry("domingos", new WeekdayWord(DayOfWeek.SUNDAY, true)),
			// abbreviations ("ter" is left out: it is also the verb "to have")
			Map.entry("seg", new WeekdayWord(DayOfWeek.MONDAY, false)),
			Map.entry("qua", new WeekdayWord(DayOfWeek.WEDNESDAY, false)),
			Map.entry("qui", new WeekdayWord(DayOfWeek.THURSDAY, false)),
			Map.entry("sex", new WeekdayWord(DayOfWeek.FRIDAY, false)),
			Map.entry("sab", new WeekdayWord(DayOfWeek.SATURDAY, false)),
			Map.entry("dom", new WeekdayWord(DayOfWeek.SUNDAY, false)));

	private static final List<String> MONTHS = List.of("janeiro", "fevereiro", "marco", "abril", "maio", "junho",
			"julho", "agosto", "setembro", "outubro", "novembro", "dezembro");

	private static final List<String> MONTH_ABBREVIATIONS = List.of("jan", "fev", "mar", "abr", "mai", "jun", "jul",
			"ago", "set", "out", "nov", "dez");

	private static final Pattern SENTENCE_END = Pattern.compile("[.;!?]+(?=\\s|$)|\\R+");
	private static final Pattern IS_VERB_ESTA = Pattern.compile("(?<!\\p{L})está(?!\\p{L})",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);

	private PortugueseDateLexicon() {
	}

	/** The weekday a normalised token names, or null. */
	static WeekdayWord weekday(String token) {
		return WEEKDAYS.get(token);
	}

	/** 1-12 for a full month name, 0 for anything else. */
	static int month(String token) {
		return MONTHS.indexOf(token) + 1;
	}

	/** 1-12 for a full month name or a three-letter abbreviation, 0 for anything else. */
	static int monthOrAbbreviation(String token) {
		int full = month(token);
		return full == 0 ? MONTH_ABBREVIATIONS.indexOf(token) + 1 : full;
	}

	/** Splits at sentence ends; a full stop between digits ("3.11") is not one. */
	static List<String> clauses(String raw) {
		return Arrays.stream(SENTENCE_END.split(raw)).filter(s -> !s.isBlank()).toList();
	}

	/**
	 * Lower case, accents removed, "-feira" removed, "3.11" read as "3/11", commas kept as their own token and every
	 * other punctuation turned into a space. The verb "está" is kept apart from "esta" ("this") as "estah", because
	 * the accent is the only thing that tells them apart.
	 */
	static String normalize(String raw) {
		String s = IS_VERB_ESTA.matcher(raw).replaceAll("estah");
		s = s.toLowerCase(Locale.ROOT);
		s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
		s = s.replace("º", "").replace("ª", "");
		s = s.replaceAll("-feiras?", "");
		s = s.replaceAll("(?<=\\d)\\.(?=\\d)", "/");
		s = s.replace(",", " , ");
		s = s.replaceAll("(?<!\\d)-|-(?!\\d)", " ");
		s = s.replaceAll("[^a-z0-9/\\- ,]", " ");
		return s.trim().replaceAll("\\s+", " ");
	}

	static List<String> tokens(String normalized) {
		return normalized.isEmpty() ? List.of() : Arrays.asList(normalized.split(" "));
	}
}
