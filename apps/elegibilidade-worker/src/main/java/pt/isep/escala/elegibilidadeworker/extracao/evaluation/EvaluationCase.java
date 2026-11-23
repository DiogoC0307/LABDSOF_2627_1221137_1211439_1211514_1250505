package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityPeriod;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * One line of the evaluation dataset (JSONL): {@code id}, {@code category} (normal / difficult / adverse),
 * {@code input}, {@code referenceDate}, {@code expected.periods} and an optional {@code note}. The format is described
 * in the README next to the dataset.
 *
 * <p>When a line cannot be read, the error names the line number and nothing else: the dataset is not echoed into logs.
 */
public record EvaluationCase(String id, String category, String input, LocalDate referenceDate,
		List<UnavailabilityPeriod> expectedPeriods, String note) {

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm[:ss]");

	public EvaluationCase {
		expectedPeriods = List.copyOf(expectedPeriods);
	}

	public static EvaluationCase fromJson(String line, ObjectMapper mapper) {
		JsonNode root = mapper.readTree(line);
		JsonNode periods = required(required(root, "expected"), "periods");
		if (!periods.isArray()) {
			throw new IllegalArgumentException("expected.periods is not a list");
		}
		List<UnavailabilityPeriod> expected = new ArrayList<>();
		for (JsonNode period : periods) {
			expected.add(period(period));
		}
		JsonNode note = root.get("note");
		return new EvaluationCase(text(root, "id"), text(root, "category"), text(root, "input"),
				LocalDate.parse(text(root, "referenceDate")), expected,
				note == null || note.isNull() ? null : note.asString());
	}

	/** Reads every non-blank line of a JSONL stream, in order. */
	public static List<EvaluationCase> readAll(InputStream jsonl, ObjectMapper mapper) throws IOException {
		List<EvaluationCase> cases = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(jsonl, StandardCharsets.UTF_8))) {
			String line;
			int number = 0;
			while ((line = reader.readLine()) != null) {
				number++;
				if (line.isBlank()) {
					continue;
				}
				try {
					cases.add(fromJson(line, mapper));
				}
				catch (JacksonException e) {
					throw new IllegalArgumentException("line " + number + ": not valid JSON");
				}
				catch (RuntimeException e) {
					throw new IllegalArgumentException("line " + number + ": not a valid evaluation case");
				}
			}
		}
		return cases;
	}

	private static UnavailabilityPeriod period(JsonNode node) {
		Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
		JsonNode days = node.get("weekdays");
		if (days != null && days.isArray()) {
			for (JsonNode day : days) {
				weekdays.add(weekday(day.asString()));
			}
		}
		return new UnavailabilityPeriod(LocalDate.parse(text(node, "startDate")), LocalDate.parse(text(node, "endDate")),
				weekdays, optionalTime(node, "startTime"), optionalTime(node, "endTime"));
	}

	private static DayOfWeek weekday(String code) {
		String upper = code.strip().toUpperCase(java.util.Locale.ROOT);
		for (DayOfWeek day : DayOfWeek.values()) {
			if (day.name().equals(upper) || day.name().substring(0, 3).equals(upper)) {
				return day;
			}
		}
		throw new IllegalArgumentException("unknown weekday");
	}

	/** A time as "08:00"; "24:00" is read as midnight. */
	private static LocalTime optionalTime(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asString().strip();
		return text.equals("24:00") ? LocalTime.MIDNIGHT : LocalTime.parse(text, TIME);
	}

	private static JsonNode required(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			throw new IllegalArgumentException("missing field " + field);
		}
		return value;
	}

	private static String text(JsonNode node, String field) {
		return required(node, field).asString();
	}
}
