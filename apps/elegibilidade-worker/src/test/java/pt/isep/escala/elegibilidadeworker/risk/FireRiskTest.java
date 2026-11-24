package pt.isep.escala.elegibilidadeworker.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.isep.escala.elegibilidadeworker.contract.Contracts;
import tools.jackson.databind.ObjectMapper;

/**
 * TC-SCRUM-76-01 to 03. The HTTP calls go to {@code tools/ipma-sim} (SCRUM-144), never to api.ipma.pt.
 */
class FireRiskTest {

	private static final Duration STALENESS = Duration.ofHours(3);

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	private static final Clock MORNING = Clock.fixed(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC);

	private static final ObjectMapper JSON = new ObjectMapper();

	@Test
	@DisplayName("TC-SCRUM-76-01 the simulator returns RCM 4 and the event is a RiscoAtualizado for that value")
	void rcm4FromTheSimulator() throws Exception {
		try (Simulator sim = Simulator.start("ok", "4")) {
			RiskCache.Reading reading = new IpmaClient().fetch(sim.base, "1312", TIMEOUT, 2, STALENESS, MORNING);
			assertThat(reading.outcome()).isEqualTo(RiskOutcome.OK);
			assertThat(reading.rcm()).isEqualTo(4);
			assertThat(reading.forecastDate()).isEqualTo(LocalDate.parse("2026-11-21"));

			RiskSnapshot next = RiskCache.apply(blank(), reading, MORNING.instant(), STALENESS, "SIMULATOR");
			assertThat(next.rcm()).isEqualTo(4);
			assertThat(next.assumedHigher()).isFalse();
			assertThat(next.publishable()).isTrue();

			String event = RiscoAtualizadoJson.of(JSON, UUID.randomUUID(), next, MORNING.instant(), "tc-scrum-76-01");
			assertThat(Contracts.eventErrors("risco-atualizado.schema.json", event)).as(event).isEmpty();
			assertThat(event).contains("\"rcm\":4").contains("\"source\":\"SIMULATOR\"").contains("\"assumedHigher\":false");
		}
	}

	@Test
	@DisplayName("TC-SCRUM-76-02 after 3 h without an answer the last RCM is kept and marked as possibly higher")
	void staleKeepsTheLastValue() throws Exception {
		Instant later = MORNING.instant().plus(STALENESS);
		RiskSnapshot cached = new RiskSnapshot("1312", 4, LocalDate.parse("2026-11-21"), MORNING.instant(), false,
				RiskOutcome.OK, MORNING.instant(), "SIMULATOR");
		try (Simulator sim = Simulator.start("unavailable", null)) {
			RiskCache.Reading reading = new IpmaClient().fetch(sim.base, "1312", TIMEOUT, 2, STALENESS,
					Clock.fixed(later, ZoneOffset.UTC));
			assertThat(reading.outcome()).isEqualTo(RiskOutcome.UNAVAILABLE);

			RiskSnapshot next = RiskCache.apply(cached, reading, later, STALENESS, "SIMULATOR");
			assertThat(next.rcm()).isEqualTo(4);
			assertThat(next.validAt()).isEqualTo(MORNING.instant());
			assertThat(next.assumedHigher()).isTrue();

			String event = RiscoAtualizadoJson.of(JSON, UUID.randomUUID(), next, later, "tc-scrum-76-02");
			assertThat(Contracts.eventErrors("risco-atualizado.schema.json", event)).as(event).isEmpty();
			assertThat(JSON.readTree(event).path("dataAgeHours").asDouble()).isEqualTo(3.0);
			assertThat(event).contains("\"assumedHigher\":true");
		}
	}

	@Test
	@DisplayName("TC-SCRUM-76-03 the IPMA tests use tools/ipma-sim and never api.ipma.pt")
	void usesTheSimulator() throws Exception {
		try (Simulator sim = Simulator.start("ok", "1")) {
			assertThat(sim.directory.getFileName().toString()).isEqualTo("ipma-sim");
			assertThat(Files.isRegularFile(sim.directory.resolve("server.js"))).isTrue();
			assertThat(sim.base.getHost()).isNotEqualTo("api.ipma.pt");
			assertThat(sim.base.toString()).doesNotContain("api.ipma.pt");

			RiskCache.Reading reading = new IpmaClient().fetch(sim.base, "1312", TIMEOUT, 2, STALENESS, MORNING);
			assertThat(reading.outcome()).isEqualTo(RiskOutcome.OK);
			assertThat(reading.rcm()).isEqualTo(1);
		}
	}

	@Test
	@DisplayName("§6.5 delayed, incomplete and invalid bodies keep the last valid RCM")
	void unusableBodies() throws Exception {
		RiskSnapshot cached = new RiskSnapshot("1312", 2, LocalDate.parse("2026-11-21"), MORNING.instant(), false,
				RiskOutcome.OK, MORNING.instant(), "SIMULATOR");
		try (Simulator delayed = Simulator.start("delayed", "5")) {
			RiskCache.Reading reading = new IpmaClient().fetch(delayed.base, "1312", TIMEOUT, 2, STALENESS, MORNING);
			assertThat(reading.outcome()).isEqualTo(RiskOutcome.DELAYED);
			RiskSnapshot next = RiskCache.apply(cached, reading, MORNING.instant(), STALENESS, "SIMULATOR");
			assertThat(next.rcm()).isEqualTo(2);
			assertThat(next.assumedHigher()).isFalse();
		}
		try (Simulator incomplete = Simulator.start("incomplete", null)) {
			RiskCache.Reading reading = new IpmaClient().fetch(incomplete.base, "1312", TIMEOUT, 2, STALENESS, MORNING);
			assertThat(reading.outcome()).isEqualTo(RiskOutcome.INCOMPLETE);
			assertThat(RiskCache.apply(cached, reading, MORNING.instant(), STALENESS, "SIMULATOR").assumedHigher())
					.isTrue();
		}
		try (Simulator invalid = Simulator.start("invalid", null)) {
			RiskCache.Reading reading = new IpmaClient().fetch(invalid.base, "1312", TIMEOUT, 2, STALENESS, MORNING);
			assertThat(reading.outcome()).isEqualTo(RiskOutcome.INVALID);
			RiskSnapshot next = RiskCache.apply(cached, reading, MORNING.instant(), STALENESS, "SIMULATOR");
			assertThat(next.rcm()).isEqualTo(2);
			assertThat(next.assumedHigher()).isTrue();
		}
	}

	private static RiskSnapshot blank() {
		return new RiskSnapshot("1312", null, null, null, false, RiskOutcome.UNAVAILABLE, MORNING.instant(),
				"SIMULATOR");
	}

	/** A tools/ipma-sim process on a free port. */
	private static final class Simulator implements AutoCloseable {

		final URI base;
		final Path directory;
		private final Process process;

		private Simulator(URI base, Path directory, Process process) {
			this.base = base;
			this.directory = directory;
			this.process = process;
		}

		static Simulator start(String mode, String rcm) throws Exception {
			Path directory = Path.of("tools", "ipma-sim");
			if (!Files.isDirectory(directory)) {
				directory = Path.of("..", "..", "tools", "ipma-sim");
			}
			ProcessBuilder builder = new ProcessBuilder("node", "server.js");
			builder.directory(directory.toFile());
			builder.environment().put("IPMA_SIM_MODE", mode);
			builder.environment().put("PORT", "0");
			if (rcm == null) {
				builder.environment().remove("IPMA_SIM_RCM");
			}
			else {
				builder.environment().put("IPMA_SIM_RCM", rcm);
			}
			Process process = builder.start();
			Thread stderr = new Thread(() -> {
				try {
					process.getErrorStream().transferTo(java.io.OutputStream.nullOutputStream());
				}
				catch (Exception ignored) {
					// the process is being stopped
				}
			});
			stderr.setDaemon(true);
			stderr.start();
			String line;
			BufferedReader out = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
			line = out.readLine();
			if (line == null || !line.startsWith("listening ")) {
				process.destroyForcibly();
				throw new IllegalStateException("tools/ipma-sim did not start (" + mode + ")");
			}
			int port = Integer.parseInt(line.substring("listening ".length()).trim());
			return new Simulator(URI.create("http://127.0.0.1:" + port), directory.toAbsolutePath().normalize(), process);
		}

		@Override
		public void close() {
			process.destroyForcibly();
		}
	}
}
