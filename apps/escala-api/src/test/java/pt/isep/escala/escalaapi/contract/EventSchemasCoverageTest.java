package pt.isep.escala.escalaapi.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * TC-SCRUM-96-09: every event schema in {@code contracts/events/} has a known place in escala-api, so a new schema, or a
 * new producer or consumer, cannot go in without its contract test.
 */
class EventSchemasCoverageTest {

	/** What escala-api does with each event, and the test that checks it against its schema. */
	static final Map<String, String> ESCALA_API = Map.of(
			"avaliacao-pedida.schema.json", "produced: TC-SCRUM-96-07",
			"avaliacao-concluida.schema.json", "consumed: TC-SCRUM-96-08",
			"extracao-pedida.schema.json", "produced: TC-SCRUM-96-17",
			"extracao-concluida.schema.json", "consumed: TC-SCRUM-96-18",
			"risco-atualizado.schema.json", "not built yet");

	@Test
	@DisplayName("TC-SCRUM-96-09 every schema in contracts/events/ is listed for escala-api, and every envelope type has a schema")
	void everySchemaIsAccountedFor() throws Exception {
		assertThat(ESCALA_API.keySet()).containsExactlyInAnyOrderElementsOf(Contracts.eventSchemaFiles());

		JsonNode envelope = new ObjectMapper()
				.readTree(Contracts.DIR.resolve("events/envelope.schema.json").toFile());
		Set<String> fromTypes = new TreeSet<>();
		for (JsonNode type : envelope.path("properties").path("type").path("enum")) {
			fromTypes.add(type.asString().replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase() + ".schema.json");
		}
		assertThat(fromTypes).isEqualTo(Contracts.eventSchemaFiles());
	}
}
