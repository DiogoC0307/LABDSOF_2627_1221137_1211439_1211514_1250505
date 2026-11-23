package pt.isep.escala.elegibilidadeworker.extracao.baseline;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult;
import pt.isep.escala.elegibilidadeworker.extracao.ProposalRules;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityExtractor;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityPeriod;

/**
 * The non-AI baseline of 06 · Responsible AI Opportunity Assessment (SCRUM-99): a small rule-based parser for the
 * patterns the page lists (weekdays, "fds", "hoje/amanhã", "esta/próxima semana", dates such as "3 de novembro" or
 * "3/11", ranges, "manhã/tarde/noite", "posso/não posso"). It runs in the same process, with no network and no model.
 *
 * <p>It only extracts what the member cannot do. The rules it applies, in order:
 * <ol>
 * <li>Empty input, or more than 500 characters: nothing.</li>
 * <li>Words that try to instruct the system (prompt injection): nothing, for the whole message. A rule-based parser
 * cannot be instructed, but a message that tries to is not one to trust.</li>
 * <li>Each clause (split at full stops, "mas" and a comma before "posso") is judged on its own: vague ("talvez",
 * "se calhar", "não sei se") gives nothing; about someone else ("o João não pode") gives nothing; about when the member
 * CAN serve ("só posso quinta") gives nothing; anything else is unavailability and its dates are extracted.</li>
 * <li>A date in the past or more than 6 months ahead rejects the whole message (06: such output is rejected and the
 * form is shown).</li>
 * </ol>
 *
 * <p>Declared limitations: numbers written in words ("dia dez"), explicit hours ("depois das 18h"), a part of the day
 * with no date ("à noite não posso"), and reasons that are spread over two clauses.
 */
public final class RuleBasedExtractor implements UnavailabilityExtractor {

	private static final Set<String> NEGATION = Set.of("nao", "n", "nunca", "nem");
	private static final String POSSO = "posso";
	private static final String CONSIGO = "consigo";

	private static final Set<String> CAN = Set.of(POSSO, CONSIGO, "disponivel", "disponiveis", "livre", "livres");
	private static final Set<String> VAGUE_WORDS = Set.of("talvez", "calhar", "provavelmente", "possivelmente", "acho",
			"depende");
	private static final Set<String> FIRST_PERSON = Set.of(POSSO, CONSIGO, "estou", "vou", "tenho", "fico", "sou",
			"eu", "podemos", "preciso");
	private static final Set<String> THIRD_PERSON = Set.of("pode", "podem", "tem", "vai", "vao", "estao", "fica",
			"ficam", "consegue", "vem", "ele", "ela", "eles", "elas", "estah");
	private static final Set<String> ABSENT_AFTER_ESTA = Set.of("fora", "ausente", "doente", "indisponivel", "ferias");
	private static final Set<String> INSTRUCTIONS = Set.of("ignora", "ignore", "ignorar", "ignorando", "esquece",
			"esquecer", "desconsidera", "instrucoes", "instrucao", "prompt", "apaga", "elimina");
	/** Words after a comma that start a clause about when the member CAN serve ("..., só posso sábado"). */
	private static final Set<String> BEFORE_CAN = Set.of(POSSO, "so", CONSIGO, "apenas");
	private static final Set<String> CONTRAST = Set.of("mas", "porem", "contudo");

	private enum Kind {
		UNAVAILABLE, AVAILABLE, SOMEONE_ELSE, VAGUE
	}

	private final DayParts dayParts;

	public RuleBasedExtractor() {
		this(DayParts.DEFAULT);
	}

	public RuleBasedExtractor(DayParts dayParts) {
		this.dayParts = Objects.requireNonNull(dayParts, "dayParts");
	}

	@Override
	public ExtractionResult extract(String text, LocalDate referenceDate) {
		Objects.requireNonNull(referenceDate, "referenceDate");
		if (!ProposalRules.isUsableInput(text)) {
			return ExtractionResult.empty();
		}
		List<String> allTokens = PortugueseDateLexicon.tokens(PortugueseDateLexicon.normalize(text));
		if (allTokens.stream().anyMatch(INSTRUCTIONS::contains)) {
			return ExtractionResult.empty();
		}
		List<UnavailabilityPeriod> periods = new ArrayList<>();
		for (String clause : PortugueseDateLexicon.clauses(text)) {
			List<String> tokens = PortugueseDateLexicon.tokens(PortugueseDateLexicon.normalize(clause));
			for (List<String> part : splitAtContrast(tokens)) {
				if (classify(part) == Kind.UNAVAILABLE) {
					periods.addAll(new ClauseParser(part, referenceDate, dayParts).periods());
				}
			}
		}
		if (!ProposalRules.withinWindow(periods, referenceDate)) {
			return ExtractionResult.empty();
		}
		return new ExtractionResult(periods.stream().distinct().sorted(ORDER).toList());
	}

	private static final Comparator<UnavailabilityPeriod> ORDER = Comparator
			.comparing(UnavailabilityPeriod::startDate)
			.thenComparing(UnavailabilityPeriod::endDate)
			.thenComparing(UnavailabilityPeriod::startTime, Comparator.nullsFirst(Comparator.naturalOrder()));

	/** Splits at "mas" and at a comma before "posso" / "só posso", so "não posso sexta, mas posso sábado" is two. */
	private static List<List<String>> splitAtContrast(List<String> tokens) {
		List<List<String>> parts = new ArrayList<>();
		List<String> current = new ArrayList<>();
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			boolean commaBeforeCan = token.equals(",") && i + 1 < tokens.size()
					&& BEFORE_CAN.contains(tokens.get(i + 1));
			if (CONTRAST.contains(token) || commaBeforeCan) {
				parts.add(current);
				current = new ArrayList<>();
			}
			else {
				current.add(token);
			}
		}
		parts.add(current);
		return parts;
	}

	private static Kind classify(List<String> tokens) {
		if (tokens.stream().anyMatch(VAGUE_WORDS::contains) || containsPair(tokens, "nao", "sei")) {
			return Kind.VAGUE;
		}
		boolean negated = tokens.stream().anyMatch(NEGATION::contains);
		boolean firstPerson = tokens.stream().anyMatch(FIRST_PERSON::contains);
		if (!firstPerson && (tokens.stream().anyMatch(THIRD_PERSON::contains) || isAbsentThirdPerson(tokens))) {
			return Kind.SOMEONE_ELSE;
		}
		if (!negated && tokens.stream().anyMatch(CAN::contains)) {
			return Kind.AVAILABLE;
		}
		return Kind.UNAVAILABLE;
	}

	/** "esta fora": "está" typed without its accent, followed by a word that says someone is away. */
	private static boolean isAbsentThirdPerson(List<String> tokens) {
		for (int i = 0; i + 1 < tokens.size(); i++) {
			if (tokens.get(i).equals("esta") && ABSENT_AFTER_ESTA.contains(tokens.get(i + 1))) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsPair(List<String> tokens, String first, String second) {
		for (int i = 0; i + 1 < tokens.size(); i++) {
			if (tokens.get(i).equals(first) && tokens.get(i + 1).equals(second)) {
				return true;
			}
		}
		return false;
	}
}
