package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityPeriod;
import pt.isep.escala.elegibilidadeworker.extracao.baseline.PortugueseDateLexicon.WeekdayWord;

/**
 * Reads one clause that {@link RuleBasedExtractor} has already judged to be about the member's own unavailability, and
 * turns its date expressions into periods.
 *
 * <p>Two passes. First the tokens are scanned left to right for date items (a day, a range, a repeating weekday, a
 * limit such as "até dezembro", a part of the day). Then items that are only separated by link words ("e", "nem", "de"
 * ...) are grouped, so that "sexta e sábado à noite" shares the night and "sexta de manhã e sábado" does not.
 *
 * <p>Conventions (README of the evaluation dataset): a weekday alone is its next occurrence after the reference date;
 * "esta semana" runs from the reference date to Sunday; "próxima semana" is Monday to Sunday of the following week;
 * "até dezembro" is until 30 November; "nos próximos N dias" starts tomorrow. Decisions of this baseline: a repeating
 * weekday with no limit runs to the end of the next month; a limit that names a day includes that day; a date without
 * a year is its next occurrence on or after the reference date; numbers written in words are not understood.
 */
final class ClauseParser {

	private enum Kind {
		DAY, RANGE, REPEAT, LIMIT, PART
	}

	/** A date expression found in the clause, with the tokens it covers ({@code from} inclusive, {@code to} exclusive). */
	private record Item(Kind kind, int from, int to, LocalDate start, LocalDate end, Set<DayOfWeek> days,
			DayParts.Window window) {

		static Item day(int from, int to, LocalDate date) {
			return new Item(Kind.DAY, from, to, date, date, null, null);
		}

		static Item range(int from, int to, LocalDate start, LocalDate end) {
			return new Item(Kind.RANGE, from, to, start, end, null, null);
		}

		static Item repeat(int from, int to, Set<DayOfWeek> days) {
			return new Item(Kind.REPEAT, from, to, null, null, days, null);
		}

		static Item limit(int from, int to, LocalDate end) {
			return new Item(Kind.LIMIT, from, to, null, end, null, null);
		}

		static Item part(int from, int to, DayParts.Window window) {
			return new Item(Kind.PART, from, to, null, null, null, window);
		}
	}

	/** A date that may be incomplete: a day (maybe with month and year), a weekday, or a day relative to today. */
	private record Partial(Integer day, Integer month, Integer year, DayOfWeek weekday, Integer relativeDays) {
	}

	private record Endpoint(Partial value, int length) {
	}

	/** Items that belong together: some dates, one optional part of the day, one optional limit. */
	private static final class Group {
		final List<Item> dates = new ArrayList<>();
		DayParts.Window window;
		Item limit;
		int end;
	}

	private static final Pattern ISO_DATE = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
	private static final Pattern SLASH_DATE = Pattern.compile("(\\d{1,2})/(\\d{1,2})(?:/(\\d{2}|\\d{4}))?");
	private static final Pattern YEAR = Pattern.compile("\\d{4}");
	private static final Pattern DAY_NUMBER = Pattern.compile("\\d{1,2}");

	/** Words that may sit between two date items without separating them. */
	private static final Set<String> LINK = Set.of("e", ",", "nem", "ou", "a", "ao", "de", "da", "do", "na", "no",
			"pela", "toda", "o", "os", "as", "aos", "nos", "nas", "em", "para", "dia", "dias");

	/** Words before "manhã", "tarde" or "noite" that make them a part of the day ("mais tarde" is not one). */
	private static final Set<String> BEFORE_PART = Set.of("de", "a", "da", "na", "pela", "toda");

	private static final Set<String> UNITS = Set.of("dia", "dias", "semana", "semanas", "mes", "meses", "hora", "horas",
			"h", "vezes", "anos", "ano");

	private final List<String> t;
	private final LocalDate ref;
	private final DayParts dayParts;

	ClauseParser(List<String> tokens, LocalDate referenceDate, DayParts dayParts) {
		this.t = tokens;
		this.ref = referenceDate;
		this.dayParts = dayParts;
	}

	List<UnavailabilityPeriod> periods() {
		List<Item> items = new ArrayList<>();
		int i = 0;
		while (i < t.size()) {
			i += Math.max(scanAt(i, items), 1);
		}
		return toPeriods(group(items));
	}

	// ---- scanning -------------------------------------------------------------------------------------------

	/** Tries each date expression at token {@code i}; returns how many tokens it used, 0 if none matched. */
	private int scanAt(int i, List<Item> out) {
		int used = range(i, out);
		if (used == 0) {
			used = limit(i, out);
		}
		if (used == 0) {
			used = todayPart(i, out);
		}
		if (used == 0) {
			used = part(i, out);
		}
		if (used == 0) {
			used = weekend(i, out);
		}
		if (used == 0) {
			used = week(i, out);
		}
		if (used == 0) {
			used = nextN(i, out);
		}
		if (used == 0) {
			used = relativeDay(i, out);
		}
		if (used == 0) {
			used = dayList(i, out);
		}
		if (used == 0) {
			used = weekday(i, out);
		}
		if (used == 0) {
			used = date(i, out);
		}
		return used;
	}

	private String at(int i) {
		return i >= 0 && i < t.size() ? t.get(i) : "";
	}

	/** "de 3 a 7 de novembro", "do dia 3 ao dia 7", "de segunda a sexta", "entre 3 e 7 de novembro". */
	private int range(int i, List<Item> out) {
		String w = at(i);
		boolean between = w.equals("entre");
		if (!(between || w.equals("de") || w.equals("do") || w.equals("da") || w.equals("desde"))) {
			return 0;
		}
		Endpoint first = endpoint(i + 1, true);
		if (first == null) {
			return 0;
		}
		int j = i + 1 + first.length();
		String connector = at(j);
		if (!(connector.equals("a") || connector.equals("ao") || connector.equals("ate")
				|| (between && connector.equals("e")))) {
			return 0;
		}
		Endpoint second = endpoint(j + 1, true);
		if (second == null) {
			return 0;
		}
		int length = j + 1 + second.length() - i;
		Partial from = first.value();
		Partial to = second.value();
		if (from.day() != null && from.month() == null && to.day() != null && to.month() != null) {
			// "de 3 a 7 de novembro": the month of the end is the month of the start; "de 28 a 2 de novembro" starts
			// in the month before
			int month = to.month();
			Integer year = to.year();
			if (from.day() > to.day()) {
				month--;
				if (month == 0) {
					month = 12;
					year = year == null ? null : year - 1;
				}
			}
			from = new Partial(from.day(), month, year, null, null);
		}
		LocalDate start = resolve(from, ref);
		LocalDate end = start == null ? null : resolve(to, start);
		if (start != null && end != null && !end.isBefore(start)) {
			out.add(Item.range(i, i + length, start, end));
		}
		return length;
	}

	/** "até dezembro" (until the end of November), "até dia 15 de novembro", "até sexta". */
	private int limit(int i, List<Item> out) {
		if (!at(i).equals("ate")) {
			return 0;
		}
		int j = i + 1;
		if (at(j).equals("ao") || at(j).equals("a") || at(j).equals("o")) {
			j++;
		}
		int month = PortugueseDateLexicon.month(at(j));
		if (month != 0) {
			YearMonth monthBefore = YearMonth.of(ref.getYear(), month).minusMonths(1);
			LocalDate end = monthBefore.atEndOfMonth();
			if (end.isBefore(ref)) {
				end = YearMonth.of(ref.getYear() + 1, month).minusMonths(1).atEndOfMonth();
			}
			out.add(Item.limit(i, j + 1, end));
			return j + 1 - i;
		}
		Endpoint endpoint = endpoint(j, false);
		if (endpoint == null) {
			return 0;
		}
		LocalDate end = resolve(endpoint.value(), ref);
		if (end != null) {
			out.add(Item.limit(i, j + endpoint.length(), end));
		}
		return j + endpoint.length() - i;
	}

	/** "esta noite", "esta tarde", "esta manhã": today, with that part of the day. */
	private int todayPart(int i, List<Item> out) {
		if (!(at(i).equals("esta") || at(i).equals("nesta")) || partOf(at(i + 1)) == null) {
			return 0;
		}
		out.add(Item.day(i, i + 1, ref));
		out.add(Item.part(i + 1, i + 2, dayParts.window(partOf(at(i + 1)))));
		return 2;
	}

	/** "de manhã", "à tarde", "à noite". Without the word before, "tarde" is just late ("mais tarde"). */
	private int part(int i, List<Item> out) {
		DayParts.Part part = partOf(at(i));
		if (part == null || !BEFORE_PART.contains(at(i - 1))) {
			return 0;
		}
		out.add(Item.part(i, i + 1, dayParts.window(part)));
		return 1;
	}

	private static DayParts.Part partOf(String token) {
		return switch (token) {
			case "manha" -> DayParts.Part.MORNING;
			case "tarde" -> DayParts.Part.AFTERNOON;
			case "noite" -> DayParts.Part.NIGHT;
			default -> null;
		};
	}

	/** "fds", "fim de semana", "fins de semana" (every weekend, when plural). */
	private int weekend(int i, List<Item> out) {
		int length;
		boolean plural;
		if (at(i).equals("fds")) {
			length = 1;
			plural = false;
		}
		else if ((at(i).equals("fim") || at(i).equals("fins")) && at(i + 1).equals("de") && at(i + 2).equals("semana")) {
			length = 3;
			plural = at(i).equals("fins");
		}
		else {
			return 0;
		}
		String before = at(i - 1);
		if (before.equals("aos") || before.equals("nos") || before.equals("os") || before.equals("todos")
				|| before.equals("todas")) {
			plural = true;
		}
		if (plural) {
			out.add(Item.repeat(i, i + length, EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)));
			return length;
		}
		DayOfWeek today = ref.getDayOfWeek();
		boolean following = before.equals("proximo");
		if (!following && today == DayOfWeek.SATURDAY) {
			out.add(Item.range(i, i + length, ref, ref.plusDays(1)));
		}
		else if (!following && today == DayOfWeek.SUNDAY) {
			out.add(Item.range(i, i + length, ref, ref));
		}
		else {
			LocalDate saturday = ref.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
			out.add(Item.range(i, i + length, saturday, saturday.plusDays(1)));
		}
		return length;
	}

	/** "esta semana", "próxima semana", "semana que vem", "para a semana". */
	private int week(int i, List<Item> out) {
		String w = at(i);
		if ((w.equals("esta") || w.equals("nesta")) && at(i + 1).equals("semana")) {
			out.add(Item.range(i, i + 2, ref, ref.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))));
			return 2;
		}
		int length = 0;
		if ((w.equals("proxima") || w.equals("seguinte")) && at(i + 1).equals("semana")) {
			length = 2;
		}
		else if (w.equals("semana") && at(i + 1).equals("que") && at(i + 2).equals("vem")) {
			length = 3;
		}
		else if (w.equals("semana") && at(i + 1).equals("seguinte")) {
			length = 2;
		}
		else if (w.equals("para") && at(i + 1).equals("a") && at(i + 2).equals("semana")) {
			length = 3;
		}
		if (length == 0) {
			return 0;
		}
		LocalDate monday = ref.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
		out.add(Item.range(i, i + length, monday, monday.plusDays(6)));
		return length;
	}

	/** "nos próximos 3 dias": from tomorrow. "nas próximas 2 semanas": from tomorrow, 7 days each. */
	private int nextN(int i, List<Item> out) {
		if (!(at(i).equals("proximos") || at(i).equals("proximas")) || !at(i + 1).matches("\\d{1,3}")) {
			return 0;
		}
		int n = Integer.parseInt(at(i + 1));
		int days;
		if (at(i + 2).equals("dias")) {
			days = n;
		}
		else if (at(i + 2).equals("semanas")) {
			days = 7 * n;
		}
		else {
			return 0;
		}
		if (days >= 1) {
			out.add(Item.range(i, i + 3, ref.plusDays(1), ref.plusDays(days)));
		}
		return 3;
	}

	/** "hoje", "amanhã", "depois de amanhã" (and the past ones, which the extractor then rejects). */
	private int relativeDay(int i, List<Item> out) {
		int offset;
		int length = 1;
		switch (at(i)) {
			case "hoje" -> offset = 0;
			case "amanha" -> offset = 1;
			case "ontem" -> offset = -1;
			case "anteontem" -> offset = -2;
			case "depois" -> {
				if (!(at(i + 1).equals("de") && at(i + 2).equals("amanha"))) {
					return 0;
				}
				offset = 2;
				length = 3;
			}
			default -> {
				return 0;
			}
		}
		out.add(Item.day(i, i + length, ref.plusDays(offset)));
		return length;
	}

	/** "dia 10", "dias 10 e 12 de novembro": one day each; without a month, the next one. */
	private int dayList(int i, List<Item> out) {
		if (!(at(i).equals("dia") || at(i).equals("dias")) || !isDay(at(i + 1))) {
			return 0;
		}
		List<Integer> days = new ArrayList<>();
		days.add(Integer.parseInt(at(i + 1)));
		int j = i + 2;
		while ((at(j).equals("e") || at(j).equals(",")) && isDay(at(j + 1))) {
			days.add(Integer.parseInt(at(j + 1)));
			j += 2;
		}
		Integer month = null;
		Integer year = null;
		if (at(j).equals("de") && PortugueseDateLexicon.monthOrAbbreviation(at(j + 1)) != 0) {
			month = PortugueseDateLexicon.monthOrAbbreviation(at(j + 1));
			j += 2;
		}
		else if (PortugueseDateLexicon.month(at(j)) != 0) {
			month = PortugueseDateLexicon.month(at(j));
			j++;
		}
		if (month != null && at(j).equals("de") && YEAR.matcher(at(j + 1)).matches()) {
			year = Integer.parseInt(at(j + 1));
			j += 2;
		}
		for (int day : days) {
			LocalDate date = resolve(new Partial(day, month, year, null, null), ref);
			if (date != null) {
				out.add(Item.day(i, j, date));
			}
		}
		return j - i;
	}

	/**
	 * A weekday: "quinta" is its next occurrence after the reference date; "domingos" is every Sunday. A weekday that
	 * is followed by its date ("sexta, dia 6 de novembro") is that date only, so it is left to {@link #date}.
	 */
	private int weekday(int i, List<Item> out) {
		WeekdayWord word = PortugueseDateLexicon.weekday(at(i));
		if (word == null) {
			return 0;
		}
		if (word.plural()) {
			out.add(Item.repeat(i, i + 1, EnumSet.of(word.day())));
			return 1;
		}
		int j = at(i + 1).equals(",") ? i + 2 : i + 1;
		Endpoint following = endpoint(j, false);
		boolean followedByDate = (at(j).equals("dia") && isDay(at(j + 1)))
				|| (following != null && following.value().day() != null);
		if (!followedByDate) {
			out.add(Item.day(i, i + 1, ref.with(TemporalAdjusters.next(word.day()))));
		}
		return 1;
	}

	/** "3 de novembro", "3/11", "3/11/2026", "2026-11-03". */
	private int date(int i, List<Item> out) {
		Endpoint endpoint = endpoint(i, false);
		if (endpoint == null || endpoint.value().day() == null) {
			return 0;
		}
		LocalDate date = resolve(endpoint.value(), ref);
		if (date != null) {
			out.add(Item.day(i, i + endpoint.length(), date));
		}
		return endpoint.length();
	}

	// ---- endpoints and dates --------------------------------------------------------------------------------

	private static boolean isDay(String token) {
		if (!DAY_NUMBER.matcher(token).matches()) {
			return false;
		}
		int n = Integer.parseInt(token);
		return n >= 1 && n <= 31;
	}

	/**
	 * A date expression at token {@code i}: "3/11", "dia 3 de novembro", "3 de novembro de 2026", "sexta", "amanhã".
	 * {@code bareDay} also accepts a bare day number ("de 3 a 7") unless it is followed by a unit ("3 dias").
	 */
	private Endpoint endpoint(int i, boolean bareDay) {
		String w = at(i);
		if (w.isEmpty()) {
			return null;
		}
		Matcher iso = ISO_DATE.matcher(w);
		if (iso.matches()) {
			return new Endpoint(new Partial(Integer.parseInt(iso.group(3)), Integer.parseInt(iso.group(2)),
					Integer.parseInt(iso.group(1)), null, null), 1);
		}
		Matcher slash = SLASH_DATE.matcher(w);
		if (slash.matches()) {
			int day = Integer.parseInt(slash.group(1));
			int month = Integer.parseInt(slash.group(2));
			if (day < 1 || day > 31 || month < 1 || month > 12) {
				return null;
			}
			Integer year = null;
			if (slash.group(3) != null) {
				int y = Integer.parseInt(slash.group(3));
				year = slash.group(3).length() == 2 ? 2000 + y : y;
			}
			return new Endpoint(new Partial(day, month, year, null, null), 1);
		}
		switch (w) {
			case "hoje":
				return new Endpoint(new Partial(null, null, null, null, 0), 1);
			case "amanha":
				return new Endpoint(new Partial(null, null, null, null, 1), 1);
			case "depois":
				if (at(i + 1).equals("de") && at(i + 2).equals("amanha")) {
					return new Endpoint(new Partial(null, null, null, null, 2), 3);
				}
				return null;
			default:
				break;
		}
		WeekdayWord word = PortugueseDateLexicon.weekday(w);
		if (word != null) {
			return word.plural() ? null : new Endpoint(new Partial(null, null, null, word.day(), null), 1);
		}
		int k = i;
		boolean hasDayWord = w.equals("dia");
		if (hasDayWord) {
			k++;
		}
		if (!isDay(at(k))) {
			return null;
		}
		int day = Integer.parseInt(at(k));
		int next = k + 1;
		Integer month = null;
		Integer year = null;
		if (at(next).equals("de") && PortugueseDateLexicon.monthOrAbbreviation(at(next + 1)) != 0) {
			month = PortugueseDateLexicon.monthOrAbbreviation(at(next + 1));
			next += 2;
		}
		else if (PortugueseDateLexicon.monthOrAbbreviation(at(next)) != 0) {
			month = PortugueseDateLexicon.monthOrAbbreviation(at(next));
			next++;
		}
		if (month != null) {
			if (at(next).equals("de") && YEAR.matcher(at(next + 1)).matches()) {
				year = Integer.parseInt(at(next + 1));
				next += 2;
			}
			return new Endpoint(new Partial(day, month, year, null, null), next - i);
		}
		if (hasDayWord || (bareDay && !UNITS.contains(at(next)))) {
			return new Endpoint(new Partial(day, null, null, null, null), next - i);
		}
		return null;
	}

	/**
	 * The date a partial date stands for: a weekday is its next occurrence after {@code from}; a day with a month is
	 * its next occurrence on or after {@code from}; a day alone is the next such day of a month; null if no such date
	 * exists ("31 de novembro").
	 */
	private LocalDate resolve(Partial p, LocalDate from) {
		if (p.relativeDays() != null) {
			return ref.plusDays(p.relativeDays());
		}
		if (p.weekday() != null) {
			return from.with(TemporalAdjusters.next(p.weekday()));
		}
		int day = p.day();
		if (p.month() != null && p.year() != null) {
			return safeDate(p.year(), p.month(), day);
		}
		if (p.month() != null) {
			for (int year = from.getYear(); year <= from.getYear() + 1; year++) {
				LocalDate date = safeDate(year, p.month(), day);
				if (date != null && !date.isBefore(from)) {
					return date;
				}
			}
			return null;
		}
		YearMonth month = YearMonth.from(from);
		for (int k = 0; k < 24; k++) {
			YearMonth candidate = month.plusMonths(k);
			if (day <= candidate.lengthOfMonth() && !candidate.atDay(day).isBefore(from)) {
				return candidate.atDay(day);
			}
		}
		return null;
	}

	private static LocalDate safeDate(int year, int month, int day) {
		YearMonth yearMonth = YearMonth.of(year, month);
		return day <= yearMonth.lengthOfMonth() ? yearMonth.atDay(day) : null;
	}

	// ---- grouping and periods -------------------------------------------------------------------------------

	/** True when the tokens from {@code from} up to {@code to} are all link words (or there are none). */
	private boolean connected(int from, int to) {
		for (int k = from; k < to; k++) {
			if (!LINK.contains(at(k))) {
				return false;
			}
		}
		return true;
	}

	private List<Group> group(List<Item> items) {
		List<Group> groups = new ArrayList<>();
		Group current = null;
		Item pendingPart = null;
		for (Item item : items) {
			switch (item.kind()) {
				case PART -> {
					if (current != null && current.window == null && current.limit == null
							&& connected(current.end, item.from())) {
						current.window = item.window();
						current.end = item.to();
					}
					else {
						pendingPart = item;
					}
				}
				case LIMIT -> {
					if (current != null && current.limit == null && connected(current.end, item.from())) {
						current.limit = item;
						current.end = item.to();
					}
					else {
						Group group = new Group();
						if (pendingPart != null && connected(pendingPart.to(), item.from())) {
							group.window = pendingPart.window();
							pendingPart = null;
						}
						group.limit = item;
						group.end = item.to();
						groups.add(group);
						current = group;
					}
				}
				default -> {
					if (current != null && current.window == null && current.limit == null
							&& connected(current.end, item.from())) {
						current.dates.add(item);
						current.end = Math.max(current.end, item.to());
					}
					else {
						Group group = new Group();
						if (pendingPart != null && connected(pendingPart.to(), item.from())) {
							group.window = pendingPart.window();
						}
						pendingPart = null;
						group.dates.add(item);
						group.end = item.to();
						groups.add(group);
						current = group;
					}
				}
			}
		}
		return groups;
	}

	private List<UnavailabilityPeriod> toPeriods(List<Group> groups) {
		List<UnavailabilityPeriod> periods = new ArrayList<>();
		for (Group group : groups) {
			LocalTime startTime = group.window == null ? null : group.window.start();
			LocalTime endTime = group.window == null ? null : group.window.end();
			LocalDate limitEnd = group.limit == null ? null : group.limit.end();
			if (group.dates.isEmpty()) {
				// "estou fora até dezembro": from the reference date to the limit
				if (limitEnd != null && !limitEnd.isBefore(ref)) {
					periods.add(new UnavailabilityPeriod(ref, limitEnd, Set.of(), startTime, endTime));
				}
				continue;
			}
			Set<DayOfWeek> repeating = EnumSet.noneOf(DayOfWeek.class);
			List<Item> singles = new ArrayList<>();
			for (Item date : group.dates) {
				if (date.kind() == Kind.REPEAT) {
					repeating.addAll(date.days());
				}
				else {
					singles.add(date);
				}
			}
			if (!repeating.isEmpty()) {
				LocalDate end = limitEnd != null ? limitEnd : ref.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
				if (!end.isBefore(ref)) {
					periods.add(new UnavailabilityPeriod(ref, end, repeating, startTime, endTime));
				}
			}
			for (Item single : singles) {
				LocalDate end = single.end();
				boolean limited = limitEnd != null && repeating.isEmpty() && singles.size() == 1
						&& single.kind() == Kind.DAY && !limitEnd.isBefore(single.start());
				periods.add(new UnavailabilityPeriod(single.start(), limited ? limitEnd : end, Set.of(), startTime,
						endTime));
			}
		}
		return periods;
	}
}
