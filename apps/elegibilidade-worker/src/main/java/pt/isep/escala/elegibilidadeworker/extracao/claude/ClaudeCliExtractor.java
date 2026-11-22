package pt.isep.escala.elegibilidadeworker.extracao.claude;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult.Outcome;
import pt.isep.escala.elegibilidadeworker.extracao.ProposalRules;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityExtractor;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityPeriod;
import pt.isep.escala.elegibilidadeworker.extracao.baseline.DayParts;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Extraction with Claude Haiku through the Claude Code CLI (ADR 0016), one process per message:
 * {@code claude -p --model haiku --output-format json --json-schema <schema>}.
 *
 * <ul>
 * <li><b>Isolation:</b> each call runs in a new, empty working directory, with no tools ({@code --tools ""}), no MCP
 * server ({@code --strict-mcp-config}), no slash commands, no saved session, and our own system prompt.</li>
 * <li><b>Input:</b> the reference date and the message go on standard input, never on the command line.</li>
 * <li><b>Environment:</b> the child process gets a short allow-list of variables and the login token
 * ({@code CLAUDE_CODE_OAUTH_TOKEN}). Nothing named {@code ANTHROPIC_*} is passed, and the extractor refuses to be
 * created when {@code ANTHROPIC_API_KEY} is set, because the CLI would use it instead of the token.</li>
 * <li><b>Output:</b> an answer with {@code is_error: true} is {@code NO_VALID_OUTPUT} whatever the exit code; so is
 * anything that is not JSON, has no {@code structured_output}, or does not match the schema exactly (no free-text
 * field). Periods in the past or more than 6 months ahead reject the whole proposal ({@link ProposalRules}).</li>
 * <li><b>Time limit:</b> after {@code timeout} the process and its children are killed: {@code TIMEOUT}.</li>
 * <li><b>Nothing is logged</b>, and no result or exception carries the message, the model's output or the token.</li>
 * </ul>
 *
 * <p>No Spring here; {@link AiExtractionConfiguration} creates it only when {@code AI_ENABLED=true} (ADR 0011).
 */
public final class ClaudeCliExtractor implements UnavailabilityExtractor {

	public static final String TOKEN_VARIABLE = "CLAUDE_CODE_OAUTH_TOKEN";
	static final String API_KEY_VARIABLE = "ANTHROPIC_API_KEY";

	/** Variables the CLI may need from the worker's environment; everything else is left out. */
	static final Set<String> PASSED_THROUGH = Set.of("PATH", "HOME", "LANG", "LC_ALL", "TZ", "TMPDIR", "TEMP", "TMP",
			"SystemRoot", "CLAUDE_CONFIG_DIR", "DISABLE_AUTOUPDATER", "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC",
			"DISABLE_TELEMETRY");

	private static final int MAX_OUTPUT_BYTES = 1_000_000;
	private static final Pattern TIME = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");
	private static final String PERIODS = "periods";
	private static final Set<String> PERIOD_FIELDS = Set.of("startDate", "endDate", "weekdays", "startTime",
			"endTime");
	private static final Comparator<UnavailabilityPeriod> ORDER = Comparator
			.comparing(UnavailabilityPeriod::startDate)
			.thenComparing(UnavailabilityPeriod::endDate)
			.thenComparing(UnavailabilityPeriod::startTime, Comparator.nullsFirst(Comparator.naturalOrder()));

	/**
	 * {@code command} is how the CLI is started (normally {@code ["claude"]}); {@code model} is the CLI's model alias;
	 * {@code timeout} is 10 s in the product (ADR 0016) and longer when it is measured.
	 */
	public record Settings(List<String> command, String model, Duration timeout, DayParts dayParts) {

		public static final Duration PRODUCT_TIMEOUT = Duration.ofSeconds(10);

		public Settings {
			command = List.copyOf(command);
			if (command.isEmpty()) {
				throw new IllegalArgumentException("command is empty");
			}
			Objects.requireNonNull(model, "model");
			Objects.requireNonNull(dayParts, "dayParts");
			if (timeout == null || timeout.isNegative() || timeout.isZero()) {
				throw new IllegalArgumentException("timeout must be positive");
			}
		}

		public static Settings defaults() {
			return new Settings(List.of("claude"), "haiku", PRODUCT_TIMEOUT, DayParts.DEFAULT);
		}

		public Settings withTimeout(Duration newTimeout) {
			return new Settings(command, model, newTimeout, dayParts);
		}

		public Settings withCommand(List<String> newCommand) {
			return new Settings(newCommand, model, timeout, dayParts);
		}
	}

	private final Settings settings;
	private final Map<String, String> environment;
	private final ExtractionPrompt prompt;
	private final ObjectMapper mapper = JsonMapper.builder().build();

	/** {@code environment} is the worker's own environment ({@link System#getenv()} in the product). */
	public ClaudeCliExtractor(Settings settings, Map<String, String> environment) {
		this.settings = Objects.requireNonNull(settings, "settings");
		if (environment.containsKey(API_KEY_VARIABLE)) {
			throw new IllegalStateException(API_KEY_VARIABLE + " is set; the worker never uses it, because the CLI "
					+ "would use it instead of " + TOKEN_VARIABLE + " (ADR 0016). Unset it.");
		}
		this.environment = Map.copyOf(environment);
		this.prompt = new ExtractionPrompt(settings.dayParts());
	}

	public static ClaudeCliExtractor fromSystemEnvironment(Settings settings) {
		return new ClaudeCliExtractor(settings, System.getenv());
	}

	/** True when the login token is in the environment (its value is never exposed). */
	public boolean hasToken() {
		String token = environment.get(TOKEN_VARIABLE);
		return token != null && !token.isBlank();
	}

	public Settings settings() {
		return settings;
	}

	/** The command line of one call; the message is not on it. */
	List<String> commandLine() {
		List<String> line = new ArrayList<>(settings.command());
		line.addAll(List.of("-p", "--model", settings.model(), "--output-format", "json", "--json-schema",
				ExtractionPrompt.OUTPUT_SCHEMA, "--system-prompt", prompt.systemPrompt(), "--tools", "",
				"--strict-mcp-config", "--disable-slash-commands", "--no-session-persistence"));
		return line;
	}

	@Override
	public ExtractionResult extract(String text, LocalDate referenceDate) {
		Objects.requireNonNull(referenceDate, "referenceDate");
		if (!ProposalRules.isUsableInput(text)) {
			return ExtractionResult.empty();
		}
		if (!hasToken()) {
			return ExtractionResult.failed(Outcome.ERROR, "NO_TOKEN");
		}
		Path callDir = null;
		Process process = null;
		try {
			callDir = privateTempDirectory();
			Path workDir = Files.createDirectory(callDir.resolve("work"));
			Path stdout = callDir.resolve("stdout.json");
			Path stderr = callDir.resolve("stderr.txt");
			ProcessBuilder builder = new ProcessBuilder(commandLine()).directory(workDir.toFile())
					.redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
			Map<String, String> childEnvironment = builder.environment();
			childEnvironment.clear();
			childEnvironment.putAll(childEnvironment());
			process = builder.start();
			writeInput(process, prompt.userMessage(text, referenceDate));
			if (!process.waitFor(settings.timeout().toMillis(), TimeUnit.MILLISECONDS)) {
				kill(process);
				return ExtractionResult.failed(Outcome.TIMEOUT, "TIMEOUT");
			}
			// stderr is never read: it may echo the message or the environment
			return parse(readCapped(stdout), referenceDate);
		}
		catch (IOException e) {
			kill(process);
			return ExtractionResult.failed(Outcome.ERROR, "PROCESS");
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			kill(process);
			return ExtractionResult.failed(Outcome.ERROR, "INTERRUPTED");
		}
		finally {
			deleteQuietly(callDir);
		}
	}

	/** The allow-listed variables plus the token. On Windows the map is case-insensitive, as the platform is. */
	Map<String, String> childEnvironment() {
		Map<String, String> child = new HashMap<>();
		for (Map.Entry<String, String> variable : environment.entrySet()) {
			boolean allowed = PASSED_THROUGH.stream().anyMatch(name -> name.equalsIgnoreCase(variable.getKey()));
			if (allowed && !variable.getKey().toUpperCase(java.util.Locale.ROOT).startsWith("ANTHROPIC")) {
				child.put(variable.getKey(), variable.getValue());
			}
		}
		child.put(TOKEN_VARIABLE, environment.get(TOKEN_VARIABLE));
		return child;
	}

	private static void writeInput(Process process, String input) {
		try (OutputStream in = process.getOutputStream()) {
			in.write(input.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e) {
			// the CLI stopped reading (it failed early); its output says what happened
		}
	}

	private static byte[] readCapped(Path file) throws IOException {
		try (InputStream in = Files.newInputStream(file)) {
			return in.readNBytes(MAX_OUTPUT_BYTES);
		}
	}

	private static void kill(Process process) {
		if (process == null) {
			return;
		}
		process.descendants().forEach(ProcessHandle::destroyForcibly);
		process.destroyForcibly();
		try {
			process.waitFor(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * A new folder for one call that only this process's user can read or write (the message goes through it): owner-only
	 * POSIX permissions where the file system has them (Linux, the container); elsewhere (Windows, for local tests) an
	 * ACL that allows the owner only.
	 */
	private static Path privateTempDirectory() throws IOException {
		if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
			return Files.createTempDirectory("claude-call-",
					PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
		}
		Path tmp = Path.of(System.getProperty("java.io.tmpdir"));
		if (Files.getFileAttributeView(tmp, AclFileAttributeView.class) == null) {
			throw new IOException("cannot restrict the call folder to its owner");
		}
		List<AclEntry> ownerOnly = List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW)
				.setPrincipal(Files.getOwner(tmp)).setPermissions(EnumSet.allOf(AclEntryPermission.class))
				.setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT).build());
		return Files.createTempDirectory("claude-call-", new FileAttribute<List<AclEntry>>() {
			@Override
			public String name() {
				return "acl:acl";
			}

			@Override
			public List<AclEntry> value() {
				return ownerOnly;
			}
		});
	}

	private static void deleteQuietly(Path dir) {
		if (dir == null) {
			return;
		}
		try (Stream<Path> paths = Files.walk(dir)) {
			paths.sorted(Comparator.reverseOrder()).forEach(ClaudeCliExtractor::deleteIfExists);
		}
		catch (IOException e) {
			// a temporary folder left behind is not worth failing the extraction for
		}
	}

	private static void deleteIfExists(Path path) {
		try {
			Files.deleteIfExists(path);
		}
		catch (IOException e) {
			// as above: the rest of the folder is still deleted
		}
	}

	// ---- output ---------------------------------------------------------------------------------------------

	private ExtractionResult parse(byte[] output, LocalDate referenceDate) {
		JsonNode root;
		try {
			root = mapper.readTree(output);
		}
		catch (JacksonException e) {
			return ExtractionResult.failed(Outcome.NO_VALID_OUTPUT, "NOT_JSON");
		}
		if (root == null || !root.isObject()) {
			return ExtractionResult.failed(Outcome.NO_VALID_OUTPUT, "NOT_JSON");
		}
		JsonNode costNode = root.get("total_cost_usd");
		Double cost = costNode != null && costNode.isNumber() ? costNode.asDouble() : null;
		String model = modelOf(root);
		JsonNode isError = root.get("is_error");
		if (isError == null || !isError.isBoolean()) {
			return new ExtractionResult(List.of(), Outcome.NO_VALID_OUTPUT, "NOT_A_RESULT", cost, model);
		}
		if (isError.booleanValue()) {
			// is_error: true counts as no valid output whatever the exit code (0 or not)
			return new ExtractionResult(List.of(), Outcome.NO_VALID_OUTPUT, "IS_ERROR", cost, model);
		}
		JsonNode structured = root.get("structured_output");
		if (structured == null || !structured.isObject()) {
			return new ExtractionResult(List.of(), Outcome.NO_VALID_OUTPUT, "NO_STRUCTURED_OUTPUT", cost, model);
		}
		List<UnavailabilityPeriod> periods;
		try {
			periods = periods(structured);
		}
		catch (RuntimeException e) {
			return new ExtractionResult(List.of(), Outcome.NO_VALID_OUTPUT, "SCHEMA", cost, model);
		}
		if (!ProposalRules.withinWindow(periods, referenceDate)) {
			return new ExtractionResult(List.of(), Outcome.OK, "OUT_OF_WINDOW", cost, model);
		}
		return new ExtractionResult(periods.stream().distinct().sorted(ORDER).toList(), Outcome.OK, null, cost, model);
	}

	/** The model that answered, from {@code modelUsage}; otherwise the alias that was asked for. */
	private String modelOf(JsonNode root) {
		JsonNode usage = root.get("modelUsage");
		if (usage == null || !usage.isObject()) {
			return settings.model();
		}
		return usage.properties().stream().findFirst().map(Map.Entry::getKey).orElse(settings.model());
	}

	/** Exactly the schema of 06: an object with only {@code periods}; each period with only the known fields. */
	private static List<UnavailabilityPeriod> periods(JsonNode structured) {
		if (structured.size() != 1 || !structured.has(PERIODS) || !structured.get(PERIODS).isArray()) {
			throw new IllegalArgumentException("not the period list");
		}
		List<UnavailabilityPeriod> periods = new ArrayList<>();
		for (JsonNode node : structured.get(PERIODS)) {
			periods.add(period(node));
		}
		return periods;
	}

	private static UnavailabilityPeriod period(JsonNode node) {
		if (!node.isObject()) {
			throw new IllegalArgumentException("a period is not an object");
		}
		boolean unknownField = node.properties().stream().anyMatch(field -> !PERIOD_FIELDS.contains(field.getKey()));
		if (unknownField) {
			throw new IllegalArgumentException("unknown field");
		}
		return new UnavailabilityPeriod(date(node.get("startDate")), date(node.get("endDate")),
				weekdays(node.get("weekdays")), time(node.get("startTime")), time(node.get("endTime")));
	}

	/** The weekday codes of a repeating period; none when the field is absent or null; each at most once. */
	private static Set<DayOfWeek> weekdays(JsonNode days) {
		Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
		if (days == null || days.isNull()) {
			return weekdays;
		}
		if (!days.isArray()) {
			throw new IllegalArgumentException("weekdays is not a list");
		}
		for (JsonNode day : days) {
			if (!weekdays.add(weekday(day.asString()))) {
				throw new IllegalArgumentException("a weekday is repeated");
			}
		}
		return weekdays;
	}

	private static LocalDate date(JsonNode node) {
		if (node == null || !node.isString()) {
			throw new IllegalArgumentException("a date is missing");
		}
		return LocalDate.parse(node.asString());
	}

	private static LocalTime time(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		if (!node.isString() || !TIME.matcher(node.asString()).matches()) {
			throw new IllegalArgumentException("not a time");
		}
		return LocalTime.parse(node.asString());
	}

	private static DayOfWeek weekday(String code) {
		for (DayOfWeek day : DayOfWeek.values()) {
			if (day.name().substring(0, 3).equals(code)) {
				return day;
			}
		}
		throw new IllegalArgumentException("not a weekday code");
	}
}
