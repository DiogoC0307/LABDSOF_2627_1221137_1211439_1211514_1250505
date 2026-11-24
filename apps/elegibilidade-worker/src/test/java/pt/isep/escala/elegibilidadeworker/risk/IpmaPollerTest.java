package pt.isep.escala.elegibilidadeworker.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEvent;
import pt.isep.escala.elegibilidadeworker.outbox.OutboxEventRepository;
import tools.jackson.databind.json.JsonMapper;

/** The hourly poll is off in tests, writes no event when there is no value, and publishes when there is one. */
@ExtendWith(MockitoExtension.class)
class IpmaPollerTest {

	private static final Instant NOW = Instant.parse("2026-11-21T12:00:00Z");

	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Mock
	private RiskProperties properties;

	@Mock
	private IpmaClient client;

	@Mock
	private RiskObservationRepository observations;

	@Mock
	private OutboxEventRepository outbox;

	@Test
	@DisplayName("while the poll is disabled it does not call IPMA")
	void disabled() {
		when(properties.isEnabled()).thenReturn(false);

		poller().scheduled();

		verify(client, never()).fetch(any(), any(), any(), anyInt(), any(), any());
		verify(outbox, never()).save(any());
	}

	@Test
	@DisplayName("an answer with no usable RCM is stored and not published")
	void unpublished() {
		stubProperties();
		when(client.fetch(any(), any(), any(), anyInt(), any(), any()))
				.thenReturn(new RiskCache.Reading(RiskOutcome.UNAVAILABLE, null, null, null));
		when(observations.findById("1312")).thenReturn(Optional.empty());

		poller().poll();

		verify(observations).save(any(RiskObservation.class));
		verify(outbox, never()).save(any());
	}

	@Test
	@DisplayName("a usable reading replaces the stored observation and publishes RiscoAtualizado")
	void published() throws Exception {
		when(properties.isEnabled()).thenReturn(true);
		stubProperties();
		LocalDate day = LocalDate.parse("2026-11-21");
		when(client.fetch(any(), any(), any(), anyInt(), any(), any()))
				.thenReturn(new RiskCache.Reading(RiskOutcome.OK, 4, day, NOW));
		RiskSnapshot cached = new RiskSnapshot("1312", 1, day, NOW.minus(Duration.ofHours(1)), false, RiskOutcome.OK,
				NOW.minus(Duration.ofHours(1)), "SIMULATOR");
		when(observations.findById("1312")).thenReturn(Optional.of(new RiskObservation(cached)));

		poller().scheduled();

		verify(observations).save(any(RiskObservation.class));
		ArgumentCaptor<OutboxEvent> event = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outbox).save(event.capture());
		assertThat(event.getValue().getRoutingKey()).isEqualTo(IpmaPoller.ROUTING_KEY);
		assertThat(event.getValue().getPayload()).contains("\"rcm\":4").contains("\"source\":\"SIMULATOR\"");
	}

	private void stubProperties() {
		when(properties.getBaseUrl()).thenReturn(URI.create("http://ipma-sim:8090"));
		when(properties.getDico()).thenReturn("1312");
		when(properties.getTimeout()).thenReturn(Duration.ofSeconds(5));
		when(properties.getRetries()).thenReturn(2);
		when(properties.getStaleness()).thenReturn(Duration.ofHours(3));
		when(properties.source()).thenReturn("SIMULATOR");
	}

	private IpmaPoller poller() {
		return new IpmaPoller(properties, client, observations, outbox, JsonMapper.builder().build(), CLOCK);
	}
}
