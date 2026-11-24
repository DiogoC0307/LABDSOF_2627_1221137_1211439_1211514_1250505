package pt.isep.escala.escalaapi.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import pt.isep.escala.escalaapi.messaging.MalformedEventException;
import pt.isep.escala.escalaapi.outbox.ProcessedEvent;
import pt.isep.escala.escalaapi.outbox.ProcessedEventRepository;
import tools.jackson.databind.json.JsonMapper;

/** RiscoAtualizado updates the display row and never the slots. A duplicate or a bad body is stored as already seen. */
@ExtendWith(MockitoExtension.class)
class RiscoAtualizadoListenerTest {

	private static final Instant AT = Instant.parse("2026-11-21T10:00:00Z");

	@Mock
	private ProcessedEventRepository processed;

	@Mock
	private DailyRiskRepository risks;

	@Test
	@DisplayName("a duplicate event is ignored")
	void duplicate() {
		UUID eventId = UUID.randomUUID();
		when(processed.existsById(eventId)).thenReturn(true);

		listener().onRisco(message(body(eventId, 4, "IPMA", AT, false)));

		verify(risks, never()).save(any());
		verify(processed, never()).save(any());
	}

	@Test
	@DisplayName("an RCM outside 1 to 5, or a source other than IPMA or the simulator, is rejected")
	void rejected() {
		listener().onRisco(message(body(UUID.randomUUID(), 0, "IPMA", AT, false)));
		listener().onRisco(message(body(UUID.randomUUID(), 6, "IPMA", AT, false)));
		listener().onRisco(message(body(UUID.randomUUID(), 4, "WEB", AT, false)));

		verify(processed, times(3)).save(any(ProcessedEvent.class));
		verify(risks, never()).findById(any());
	}

	@Test
	@DisplayName("the first event inserts the row; a later one replaces it; an older one leaves it")
	void insertAndUpdate() {
		when(risks.findById("1312")).thenReturn(Optional.empty());
		listener().onRisco(message(body(UUID.randomUUID(), 4, "IPMA", AT, false)));

		ArgumentCaptor<DailyRisk> inserted = ArgumentCaptor.forClass(DailyRisk.class);
		verify(risks).save(inserted.capture());
		assertThat(inserted.getValue().getDico()).isEqualTo("1312");
		assertThat(inserted.getValue().getRcm()).isEqualTo(4);
		assertThat(inserted.getValue().getForecastDate()).isEqualTo(LocalDate.parse("2026-11-21"));
		assertThat(inserted.getValue().getFetchedAt()).isEqualTo(AT);
		assertThat(inserted.getValue().getSource()).isEqualTo("IPMA");
		assertThat(inserted.getValue().isAssumedHigher()).isFalse();
		assertThat(inserted.getValue().getUpdatedAt()).isEqualTo(AT);

		DailyRisk current = inserted.getValue();
		when(risks.findById("1312")).thenReturn(Optional.of(current));
		listener().onRisco(message(body(UUID.randomUUID(), 5, "SIMULATOR", AT.plusSeconds(60), true)));
		assertThat(current.getRcm()).isEqualTo(5);
		assertThat(current.getSource()).isEqualTo("SIMULATOR");
		assertThat(current.isAssumedHigher()).isTrue();
		assertThat(current.getUpdatedAt()).isEqualTo(AT.plusSeconds(60));

		listener().onRisco(message(body(UUID.randomUUID(), 1, "IPMA", AT, false)));
		assertThat(current.getRcm()).isEqualTo(5);
		verify(processed, times(3)).save(any(ProcessedEvent.class));
	}

	@Test
	@DisplayName("a body that is not the event fails before any row is written")
	void malformed() {
		assertThatThrownBy(() -> listener().onRisco(message("not-json")))
				.isInstanceOf(MalformedEventException.class);
		verify(risks, never()).save(any());
	}

	private RiscoAtualizadoListener listener() {
		return new RiscoAtualizadoListener(JsonMapper.builder().build(), processed, risks);
	}

	private static Message message(String json) {
		return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
	}

	private static String body(UUID eventId, int rcm, String source, Instant occurredAt, boolean assumedHigher) {
		return """
				{"eventId":"%s","type":"RiscoAtualizado","schemaVersion":1,"occurredAt":"%s","traceId":"tc-76",\
				"dico":"1312","forecastDate":"2026-11-21","rcm":%d,"fetchedAt":"%s","source":"%s",\
				"assumedHigher":%s,"dataAgeHours":0}""".formatted(eventId, occurredAt, rcm, occurredAt, source,
				assumedHigher);
	}
}
