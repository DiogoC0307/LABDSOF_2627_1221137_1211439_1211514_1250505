package pt.isep.escala.elegibilidadeworker.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Cache rules that the simulator cases do not hit, plus the wiring around the poll. */
class RiskModelTest {

	private static final Duration STALENESS = Duration.ofHours(3);

	private static final Instant NOW = Instant.parse("2026-11-21T12:00:00Z");

	private static final LocalDate DAY = LocalDate.parse("2026-11-21");

	@Test
	@DisplayName("a delayed file newer than the cache replaces it; a delayed reading with no RCM does not")
	void delayed() {
		RiskSnapshot cached = snapshot(2, NOW.minus(Duration.ofHours(2)));
		Instant fileAt = NOW.minus(Duration.ofMinutes(30));
		RiskCache.Reading newer = new RiskCache.Reading(RiskOutcome.DELAYED, 5, DAY, fileAt);

		RiskSnapshot replaced = RiskCache.apply(cached, newer, NOW, STALENESS, "SIMULATOR");
		assertThat(replaced.rcm()).isEqualTo(5);
		assertThat(replaced.validAt()).isEqualTo(fileAt);
		assertThat(replaced.assumedHigher()).isFalse();

		RiskSnapshot noValue = RiskCache.apply(cached, new RiskCache.Reading(RiskOutcome.DELAYED, null, null, null),
				NOW, STALENESS, "SIMULATOR");
		assertThat(noValue.rcm()).isEqualTo(2);

		RiskSnapshot noClock = new RiskSnapshot("1312", 3, DAY, null, false, RiskOutcome.OK, NOW, "SIMULATOR");
		RiskSnapshot filled = RiskCache.apply(noClock, newer, NOW, STALENESS, "SIMULATOR");
		assertThat(filled.rcm()).isEqualTo(5);
		assertThat(filled.validAt()).isEqualTo(fileAt);
	}

	@Test
	@DisplayName("with nothing cached, an unusable poll publishes nothing and raises the warning")
	void empty() {
		for (RiskOutcome outcome : new RiskOutcome[] { RiskOutcome.UNAVAILABLE, RiskOutcome.INCOMPLETE,
				RiskOutcome.INVALID, RiskOutcome.DELAYED }) {
			RiskSnapshot next = RiskCache.apply(null, new RiskCache.Reading(outcome, null, null, null), NOW, STALENESS,
					"SIMULATOR");
			assertThat(next.rcm()).isNull();
			assertThat(next.dico()).isNull();
			assertThat(next.assumedHigher()).isTrue();
			assertThat(next.publishable()).isFalse();
			assertThat(next.lastOutcome()).isEqualTo(outcome);
		}

		RiskSnapshot broken = new RiskSnapshot("1312", 3, DAY, null, false, RiskOutcome.OK, NOW, "SIMULATOR");
		RiskSnapshot kept = RiskCache.apply(broken, new RiskCache.Reading(RiskOutcome.UNAVAILABLE, null, null, null),
				NOW, STALENESS, "IPMA");
		assertThat(kept.rcm()).isNull();
		assertThat(kept.dico()).isEqualTo("1312");
		assertThat(RiskSnapshot.empty("1312", RiskOutcome.OK, NOW, "IPMA").publishable()).isFalse();
	}

	@Test
	@DisplayName("a usable poll replaces the cache and the age in the event is never negative")
	void okAndAge() throws Exception {
		RiskSnapshot next = RiskCache.apply(snapshot(1, NOW.minus(Duration.ofHours(5))),
				new RiskCache.Reading(RiskOutcome.OK, 4, DAY, NOW), NOW, STALENESS, "IPMA");
		assertThat(next.rcm()).isEqualTo(4);
		assertThat(next.validAt()).isEqualTo(NOW);
		assertThat(next.assumedHigher()).isFalse();
		assertThat(next.publishable()).isTrue();
		assertThat(next.source()).isEqualTo("IPMA");

		String event = RiscoAtualizadoJson.of(new ObjectMapper(), UUID.randomUUID(), next, NOW.minusSeconds(5),
				"age-zero");
		assertThat(new ObjectMapper().readTree(event).path("dataAgeHours").asDouble()).isEqualTo(0.0);
	}

	@Test
	@DisplayName("the observation row round-trips the snapshot, and the real host is IPMA")
	void observationAndProperties() {
		RiskSnapshot snapshot = snapshot(4, NOW);
		RiskObservation row = new RiskObservation();
		row.copy(snapshot);
		assertThat(row.snapshot()).isEqualTo(snapshot);
		assertThat(new RiskObservation(snapshot).snapshot()).isEqualTo(snapshot);

		RiskProperties properties = new RiskProperties();
		assertThat(properties.isEnabled()).isTrue();
		assertThat(properties.getBaseUrl()).isEqualTo(URI.create("https://api.ipma.pt"));
		assertThat(properties.getDico()).isEqualTo("1312");
		assertThat(properties.getStaleness()).isEqualTo(Duration.ofHours(3));
		assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(5));
		assertThat(properties.getRetries()).isEqualTo(2);
		assertThat(properties.source()).isEqualTo("IPMA");

		properties.setEnabled(false);
		properties.setBaseUrl(URI.create("http://ipma-sim:8090"));
		properties.setDico("0101");
		properties.setStaleness(Duration.ofHours(1));
		properties.setTimeout(Duration.ofSeconds(1));
		properties.setRetries(0);
		assertThat(properties.isEnabled()).isFalse();
		assertThat(properties.getDico()).isEqualTo("0101");
		assertThat(properties.getStaleness()).isEqualTo(Duration.ofHours(1));
		assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(1));
		assertThat(properties.getRetries()).isEqualTo(0);
		assertThat(properties.source()).isEqualTo("SIMULATOR");
		properties.setBaseUrl(URI.create("https://API.IPMA.PT"));
		assertThat(properties.source()).isEqualTo("IPMA");
		properties.setBaseUrl(URI.create("file:///tmp/rcm.json"));
		assertThat(properties.source()).isEqualTo("SIMULATOR");
	}

	@Test
	@DisplayName("the HTTP client used by the poll is created")
	void client() {
		assertThat(new IpmaClientConfig().ipmaClient()).isNotNull();
	}

	private static RiskSnapshot snapshot(int rcm, Instant validAt) {
		return new RiskSnapshot("1312", rcm, DAY, validAt, false, RiskOutcome.OK, validAt, "SIMULATOR");
	}
}
