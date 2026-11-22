package pt.isep.escala.escalaapi.contract;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.dialect.Dialects;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * The contracts of SCRUM-96, read from {@code contracts/} at the root of the repository: the event schemas
 * ({@code contracts/events/*.schema.json}, JSON Schema 2020-12) and the REST contract ({@code contracts/openapi.yaml},
 * OpenAPI 3.0). Formats ({@code uuid}, {@code date-time}, {@code date}) are asserted, not only annotated.
 */
public final class Contracts {

	public static final Path DIR = Path.of("../../contracts");

	private static final String OPENAPI_IRI = "https://escala.local/contracts/openapi.json";

	private static final ObjectMapper JSON = new ObjectMapper();

	private static final SchemaRegistry EVENTS = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

	private static JsonNode openApi;
	private static SchemaRegistry openApiSchemas;

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
		List<String> errors = new ArrayList<>(validate(eventSchema(schemaFile), event));
		validate(eventSchema("envelope.schema.json"), event).forEach(e -> errors.add("envelope: " + e));
		return errors;
	}

	private static Schema eventSchema(String schemaFile) {
		try {
			return EVENTS.getSchema(Files.readString(DIR.resolve("events").resolve(schemaFile)));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** The operation of the spec for a method and a path as written in the spec ({@code /api/v1/members/{id}}). */
	public static JsonNode operation(String method, String pathTemplate) {
		return openApi().path("paths").path(pathTemplate).path(method.toLowerCase(Locale.ROOT));
	}

	/** Every (method, path) the spec declares, as {@code "GET /api/v1/members"}. */
	public static Set<String> operations() {
		Set<String> operations = new TreeSet<>();
		for (Map.Entry<String, JsonNode> path : openApi().path("paths").properties()) {
			for (String method : List.of("get", "put", "post", "delete", "patch")) {
				if (path.getValue().has(method)) {
					operations.add(method.toUpperCase(Locale.ROOT) + " " + path.getKey());
				}
			}
		}
		return operations;
	}

	/**
	 * The violations of a real response against the spec: the status must be documented for the operation, and the
	 * body must match the schema of its content type ({@code application/json} when none is given). An empty body is
	 * only allowed for a response without content.
	 */
	public static List<String> responseErrors(String method, String pathTemplate, int status, String contentType,
			String body) {
		JsonNode operation = operation(method, pathTemplate);
		if (operation.isMissingNode()) {
			return List.of(method + " " + pathTemplate + " is not in contracts/openapi.yaml");
		}
		String pointer = "/paths/" + escape(pathTemplate) + "/" + method.toLowerCase(Locale.ROOT) + "/responses";
		String key = responseKey(operation.path("responses"), status);
		if (key == null) {
			return List.of(method + " " + pathTemplate + " answered " + status + ", which the spec does not document");
		}
		pointer = resolve(pointer + "/" + escape(key));
		return contentErrors(pointer, method + " " + pathTemplate + " " + status, contentType, body);
	}

	/** The violations of a request body against the request body schema of the operation. */
	public static List<String> requestErrors(String method, String pathTemplate, String body) {
		JsonNode operation = operation(method, pathTemplate);
		if (operation.path("requestBody").isMissingNode()) {
			return List.of(method + " " + pathTemplate + " has no requestBody in contracts/openapi.yaml");
		}
		String pointer = resolve(
				"/paths/" + escape(pathTemplate) + "/" + method.toLowerCase(Locale.ROOT) + "/requestBody");
		return contentErrors(pointer, method + " " + pathTemplate + " request", "application/json", body);
	}

	private static List<String> contentErrors(String pointer, String what, String contentType, String body) {
		JsonNode content = at(pointer).path("content");
		if (content.isMissingNode() || content.isEmpty()) {
			return body == null || body.isBlank() ? List.of() : List.of(what + " has a body the spec does not describe");
		}
		String type = contentType == null ? "application/json" : contentType.split(";")[0].trim();
		if (!content.has(type)) {
			return List.of(what + " is " + type + ", the spec has " + fieldNames(content));
		}
		if (body == null || body.isBlank()) {
			return List.of(what + " has no body, the spec describes one");
		}
		Schema schema = openApiSchemas().getSchema(
				SchemaLocation.of(OPENAPI_IRI + "#" + pointer + "/content/" + escape(type) + "/schema"));
		return validate(schema, JSON.readTree(body)).stream().map(e -> what + ": " + e).toList();
	}

	private static String responseKey(JsonNode responses, int status) {
		String exact = Integer.toString(status);
		if (responses.has(exact)) {
			return exact;
		}
		String range = status / 100 + "XX";
		if (responses.has(range)) {
			return range;
		}
		return responses.has("default") ? "default" : null;
	}

	/** Follows a {@code $ref} to {@code #/components/...} at the pointer, if there is one. */
	private static String resolve(String pointer) {
		JsonNode node = at(pointer);
		String ref = node.path("$ref").asString("");
		return ref.startsWith("#/") ? resolve(ref.substring(1)) : pointer;
	}

	private static JsonNode at(String pointer) {
		return openApi().at(pointer);
	}

	private static List<String> validate(Schema schema, JsonNode instance) {
		List<Error> errors = schema.validate(instance,
				context -> context.executionConfig(config -> config.formatAssertionsEnabled(true)));
		return errors.stream().map(Error::toString).toList();
	}

	private static String escape(String token) {
		return token.replace("~", "~0").replace("/", "~1");
	}

	private static String fieldNames(JsonNode node) {
		List<String> names = new ArrayList<>();
		for (Iterator<String> it = node.propertyNames().iterator(); it.hasNext();) {
			names.add(it.next());
		}
		return names.toString();
	}

	private static synchronized JsonNode openApi() {
		if (openApi == null) {
			try {
				openApi = new YAMLMapper().readTree(Files.readString(DIR.resolve("openapi.yaml")));
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return openApi;
	}

	private static synchronized SchemaRegistry openApiSchemas() {
		if (openApiSchemas == null) {
			String document = JSON.writeValueAsString(openApi());
			openApiSchemas = SchemaRegistry.withDefaultDialect(Dialects.getOpenApi30(),
					builder -> builder.schemas(Map.of(OPENAPI_IRI, document)));
		}
		return openApiSchemas;
	}
}
