package pt.isep.escala.elegibilidadeworker.extracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEvent;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** TC-SCRUM-75-29: when the retries are exhausted the worker still answers, with FALLBACK, so the form is shown. */
class ExtractionFallbackOnExhaustedRetriesTest {

	private final ObjectMapper mapper = JsonMapper.builder().build();

	@Test
	@DisplayName("TC-SCRUM-75-29 after the last retry an ExtracaoPedida is answered FALLBACK with no periods, without the free text")
	void answersFallback() {
		OutboxEventRepository outbox = mock(OutboxEventRepository.class);
		var handler = new ExtractionFallbackOnExhaustedRetries(mapper, outbox);
		UUID eventId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		String body = ExtracaoPedidaIT.pedido(eventId, requestId, "trace-75-29", "TEXTOLIVRE-29 não posso sexta");

		assertThat(handler.queue()).isEqualTo("worker.extracao-pedida");
		handler.handle(new Message(body.getBytes(StandardCharsets.UTF_8), new MessageProperties()));

		ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outbox).save(saved.capture());
		assertThat(saved.getValue().getRoutingKey()).isEqualTo("extracao.concluida");
		JsonNode event = mapper.readTree(saved.getValue().getPayload());
		assertThat(event.get("requestEventId").asText()).isEqualTo(eventId.toString());
		assertThat(event.get("unavailabilityRequestId").asText()).isEqualTo(requestId.toString());
		assertThat(event.get("method").asText()).isEqualTo("FALLBACK");
		assertThat(event.get("periods")).isEmpty();
		assertThat(saved.getValue().getPayload()).doesNotContain("TEXTOLIVRE");
		assertThat(Instant.parse(event.get("occurredAt").asText())).isNotNull();
	}
}
