package pt.isep.escala.elegibilidadeworker.contract;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The event contracts of SCRUM-96, read from {@code contracts/events/} at the root of the repository (JSON Schema
 * 2020-12). Formats ({@code uuid}, {@code date-time}, {@code date}) are asserted, not only annotated. escala-api has
 * its own copy of this helper: the services share no library (ADR 0002).
 */
public final class Contracts {

	public static final Path DIR = Path.of("../../contracts");

	private static final ObjectMapper JSON = new ObjectMapper();

	private static final SchemaRegistry EVENTS = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

	private Contracts() {
	}

	/** The schema files in {@code contracts/events/}, without the envelope. */
	public static Set<String> eventSchemaFiles() {
		try (var files = Files.list(DIR.resolve("events"))) {
			Set<String> names = new TreeSet<>();
			files.map(f -> f.getFileName().toString()).filter(n -> n.endsWith(".schema.json"))
					.filter(n -> !"envelope.schema.json".equals(n)).forEach(names::add);
			return names;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** The violations of {@code json} against {@code contracts/events/<schemaFile>} and against the envelope. */
	public static List<String> eventErrors(String schemaFile, String json) {
		JsonNode event = JSON.readTree(json);
		List<String> errors = new ArrayList<>(validate(schema(schemaFile), event));
		validate(schema("envelope.schema.json"), event).forEach(e -> errors.add("envelope: " + e));
		return errors;
	}

	private static Schema schema(String schemaFile) {
		try {
			return EVENTS.getSchema(Files.readString(DIR.resolve("events").resolve(schemaFile)));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static List<String> validate(Schema schema, JsonNode instance) {
		List<Error> errors = schema.validate(instance,
				context -> context.executionConfig(config -> config.formatAssertionsEnabled(true)));
		return errors.stream().map(Error::toString).toList();
	}
}
