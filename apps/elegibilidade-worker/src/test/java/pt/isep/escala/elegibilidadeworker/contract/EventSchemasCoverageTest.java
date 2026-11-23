package pt.isep.escala.elegibilidadeworker.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TC-SCRUM-96-12: every event schema in {@code contracts/events/} has a known place in the worker, so a new schema, or
 * a new producer or consumer, cannot go in without its contract test.
 */
class EventSchemasCoverageTest {

	/** What the worker does with each event, and the test that checks it against its schema. */
	static final Map<String, String> WORKER = Map.of(
			"avaliacao-pedida.schema.json", "consumed: TC-SCRUM-96-11",
			"avaliacao-concluida.schema.json", "produced: TC-SCRUM-96-10",
			"extracao-pedida.schema.json", "consumed: TC-SCRUM-96-15",
			"extracao-concluida.schema.json", "produced: TC-SCRUM-96-14",
			"risco-atualizado.schema.json", "not built yet");

	@Test
	@DisplayName("TC-SCRUM-96-12 every schema in contracts/events/ is listed for the worker")
	void everySchemaIsAccountedFor() {
		assertThat(WORKER.keySet()).containsExactlyInAnyOrderElementsOf(Contracts.eventSchemaFiles());
	}
}
