package pt.isep.escala.elegibilidadeworker.extracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pt.isep.escala.elegibilidadeworker.contract.Contracts;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityProposer.Method;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityProposer.Proposal;
import tools.jackson.databind.ObjectMapper;

/**
 * TC-SCRUM-96-14 without a broker: every kind of proposal the worker answers with, written by
 * {@link ExtracaoConcluidaJson}, matches {@code contracts/events/extracao-concluida.schema.json} (version 2).
 */
class ExtracaoConcluidaContractTest {

	static Stream<Arguments> proposals() {
		LocalDate monday = LocalDate.of(2026, 12, 7);
		return Stream.of(
				Arguments.of("FALLBACK, no periods",
						new Proposal(List.of(), Method.FALLBACK, null, null, ExtractionResult.Outcome.TIMEOUT,
								"TIMEOUT")),
				Arguments.of("MODEL, whole days",
						new Proposal(List.of(UnavailabilityPeriod.ofDays(monday, monday.plusDays(2))), Method.MODEL,
								"claude-haiku", 1830L, ExtractionResult.Outcome.OK, "OK")),
				Arguments.of("MODEL, weekdays and a window across midnight",
						new Proposal(List.of(new UnavailabilityPeriod(monday, monday.plusWeeks(3),
								Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), LocalTime.of(20, 0), LocalTime.of(8, 0)),
								UnavailabilityPeriod.ofDays(monday.plusDays(1), monday.plusDays(1))), Method.MODEL,
								"claude-haiku", 2210L, ExtractionResult.Outcome.OK, "OK")),
				Arguments.of("BASELINE",
						new Proposal(List.of(UnavailabilityPeriod.ofDays(monday, monday)), Method.BASELINE,
								"rule-based v0", 1L, ExtractionResult.Outcome.OK, "OK")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("proposals")
	@DisplayName("TC-SCRUM-96-14 ExtracaoConcluidaJson writes an ExtracaoConcluida v2 that matches the schema, for every kind of proposal")
	void matchesTheSchema(String kind, Proposal proposal) {
		String json = ExtracaoConcluidaJson.of(new ObjectMapper(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), "contract-96-14", proposal);
		assertThat(Contracts.eventErrors("extracao-concluida.schema.json", json)).as(kind + ": " + json).isEmpty();
	}
}
