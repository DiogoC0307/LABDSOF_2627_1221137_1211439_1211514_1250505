package pt.isep.escala.elegibilidadeworker.avaliacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.isep.escala.elegibilidadeworker.contract.Contracts;
import tools.jackson.databind.ObjectMapper;

/**
 * TC-SCRUM-96-10 without a broker: every AvaliacaoConcluida the worker writes, from the listener (ELIGIBLE) and after
 * the retries (FAILED, EvaluationFailedOnExhaustedRetries), comes from {@link AvaliacaoConcluidaJson} and matches the
 * schema.
 */
class AvaliacaoConcluidaContractTest {

	@ParameterizedTest
	@ValueSource(strings = { "ELIGIBLE", "FAILED" })
	@DisplayName("TC-SCRUM-96-10 AvaliacaoConcluidaJson writes an AvaliacaoConcluida that matches the schema, for each result the worker sends")
	void matchesTheSchema(String result) {
		String json = AvaliacaoConcluidaJson.of(new ObjectMapper(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), "contract-96-10", result);
		assertThat(Contracts.eventErrors("avaliacao-concluida.schema.json", json)).as(json).isEmpty();
	}
}
