package pt.isep.escala.elegibilidadeworker.extracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityProposer.Method;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityProposer.Proposal;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TC-SCRUM-75-22: ExtracaoConcluida v2 as the worker writes it, against the fields of
 * {@code contracts/events/extracao-concluida.schema.json}: every required field, no field the schema does not have,
 * periods in its format. (A full JSON Schema validation in both services is SCRUM-96.)
 */
class ExtracaoConcluidaJsonTest {

	private static final ObjectMapper MAPPER = JsonMapper.builder().build();

	private static JsonNode schema() throws IOException {
		return MAPPER.readTree(Files.readString(Path.of("..", "..", "contracts", "events",
				"extracao-concluida.schema.json")));
	}

	private static Set<String> names(JsonNode object) {
		Set<String> names = new HashSet<>();
		object.properties().forEach(entry -> names.add(entry.getKey()));
		return names;
	}

	private static JsonNode write(Proposal proposal) {
		return MAPPER.readTree(ExtracaoConcluidaJson.of(MAPPER, UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), "trace-1", proposal));
	}

	@Test
	@DisplayName("TC-SCRUM-75-22 a MODEL answer has every required field of v2, no other field, and periods in the schema's format")
	void modelAnswer() throws IOException {
		var sundays = new UnavailabilityPeriod(LocalDate.parse("2026-10-26"), LocalDate.parse("2026-11-30"),
				Set.of(DayOfWeek.SUNDAY), LocalTime.of(8, 0), LocalTime.of(14, 0));
		var friday = UnavailabilityPeriod.ofDays(LocalDate.parse("2026-10-30"), LocalDate.parse("2026-10-30"));
		JsonNode event = write(new Proposal(List.of(sundays, friday), Method.MODEL, "claude-haiku-x", 2310L,
				ExtractionResult.Outcome.OK, null));
		JsonNode schema = schema();

		Set<String> required = new HashSet<>();
		schema.get("required").forEach(name -> required.add(name.asText()));
		assertThat(names(event)).containsAll(required).isSubsetOf(names(schema.get("properties")));
		assertThat(event.get("type").asText()).isEqualTo("ExtracaoConcluida");
		assertThat(event.get("schemaVersion").asInt()).isEqualTo(2);
		assertThat(event.get("method").asText()).isEqualTo("MODEL");
		assertThat(event.get("model").asText()).isEqualTo("claude-haiku-x");
		assertThat(event.get("latencyMs").asLong()).isEqualTo(2310L);

		Set<String> periodFields = names(schema.get("$defs").get("period").get("properties"));
		JsonNode first = event.get("periods").get(0);
		assertThat(names(first)).isSubsetOf(periodFields);
		assertThat(first.get("weekdays").get(0).asText()).isEqualTo("SUN");
		assertThat(first.get("startTime").asText()).isEqualTo("08:00");
		assertThat(first.get("endTime").asText()).isEqualTo("14:00");
		// absent, not empty or null, when the period has no weekdays and no times
		assertThat(names(event.get("periods").get(1))).containsExactlyInAnyOrder("startDate", "endDate");
	}

	@Test
	@DisplayName("TC-SCRUM-75-22 a FALLBACK answer has no periods, model null and latencyMs null")
	void fallback() {
		JsonNode event = write(Proposal.fallback(null, null, "AI_OFF"));
		assertThat(event.get("method").asText()).isEqualTo("FALLBACK");
		assertThat(event.get("periods")).isEmpty();
		assertThat(event.get("model").isNull()).isTrue();
		assertThat(event.get("latencyMs").isNull()).isTrue();
		// the worker's reason and outcome are for its log, not for the event
		assertThat(event.has("reason")).isFalse();
		assertThat(event.has("outcome")).isFalse();
	}
}
