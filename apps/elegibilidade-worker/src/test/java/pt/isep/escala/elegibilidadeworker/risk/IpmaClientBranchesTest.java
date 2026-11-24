package pt.isep.escala.elegibilidadeworker.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Readings the fixture server does not produce: bad bodies, a retry, and a poll that is interrupted. */
class IpmaClientBranchesTest {

	private static final Duration STALENESS = Duration.ofHours(3);

	private static final Duration TIMEOUT = Duration.ofSeconds(2);

	private static final Clock NOON = Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC);

	@Test
	@DisplayName("a body that is not today's RCM for the municipality is invalid, incomplete or delayed")
	void bodies() throws Exception {
		assertThat(read("not-json").outcome()).isEqualTo(RiskOutcome.INVALID);
		assertThat(read("[]").outcome()).isEqualTo(RiskOutcome.INVALID);
		assertThat(read("null").outcome()).isEqualTo(RiskOutcome.INVALID);
		assertThat(read(rcm("2026-11-21", "2026-11-21 11:00:00", "\"alto\"", true)).outcome())
				.isEqualTo(RiskOutcome.INVALID);
		assertThat(read(rcm("2026-11-21", "2026-11-21 11:00:00", "9", true)).outcome()).isEqualTo(RiskOutcome.INVALID);
		assertThat(read(rcm("yesterday", "2026-11-21 11:00:00", "4", true)).outcome()).isEqualTo(RiskOutcome.INVALID);
		assertThat(read(rcm("2026-11-21", "not-a-date", "4", true)).outcome()).isEqualTo(RiskOutcome.INVALID);
		assertThat(read(rcm("2026-11-21", "2026-11-21 11:00:00", "4", false)).outcome())
				.isEqualTo(RiskOutcome.INCOMPLETE);
		assertThat(read(rcm("2026-11-20", "2026-11-21 11:00:00", "4", true)).outcome()).isEqualTo(RiskOutcome.DELAYED);
		assertThat(read(rcm("2026-11-21", "2026-11-21 08:00:00", "3", true)).outcome()).isEqualTo(RiskOutcome.DELAYED);

		RiskCache.Reading ok = read(rcm("2026-11-21", "2026-11-21 11:30:00", "4", true));
		assertThat(ok.outcome()).isEqualTo(RiskOutcome.OK);
		assertThat(ok.rcm()).isEqualTo(4);
	}

	@Test
	@DisplayName("a feed that fails twice and then answers is accepted; one that never answers is unavailable")
	void retries() throws Exception {
		try (Feed feed = new Feed()) {
			feed.failUntil = 2;
			feed.body = rcm("2026-11-21", "2026-11-21 11:30:00", "2", true).getBytes(StandardCharsets.UTF_8);
			RiskCache.Reading recovered = new IpmaClient().fetch(feed.base, "1312", TIMEOUT, 2, STALENESS, NOON);
			assertThat(recovered.outcome()).isEqualTo(RiskOutcome.OK);
			assertThat(recovered.rcm()).isEqualTo(2);

			feed.failUntil = Integer.MAX_VALUE;
			RiskCache.Reading down = new IpmaClient().fetch(feed.base, "1312", TIMEOUT, 2, STALENESS, NOON);
			assertThat(down.outcome()).isEqualTo(RiskOutcome.UNAVAILABLE);
			assertThat(down.rcm()).isNull();
		}
	}

	@Test
	@DisplayName("an interrupted poll stops and leaves the feed unavailable")
	@SuppressWarnings("unchecked")
	void interrupted() throws Exception {
		HttpClient http = mock(HttpClient.class);
		when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenThrow(new InterruptedException("stop"));

		RiskCache.Reading reading = new IpmaClient(http).fetch(URI.create("http://127.0.0.1:9"), "1312", TIMEOUT, 2,
				STALENESS, NOON);

		assertThat(reading.outcome()).isEqualTo(RiskOutcome.UNAVAILABLE);
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
		Thread.interrupted();
	}

	@Test
	@DisplayName("a connection failure is retried and then treated as unavailable")
	@SuppressWarnings("unchecked")
	void connectionRefused() throws Exception {
		HttpClient http = mock(HttpClient.class);
		when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("down"));

		RiskCache.Reading reading = new IpmaClient(http).fetch(URI.create("http://127.0.0.1:9"), "1312", TIMEOUT, 2,
				STALENESS, NOON);

		assertThat(reading.outcome()).isEqualTo(RiskOutcome.UNAVAILABLE);
		assertThat(reading.rcm()).isNull();
	}

	private static RiskCache.Reading read(String body) throws Exception {
		try (Feed feed = new Feed()) {
			feed.body = body.getBytes(StandardCharsets.UTF_8);
			return new IpmaClient().fetch(feed.base, "1312", TIMEOUT, 0, STALENESS, NOON);
		}
	}

	private static String rcm(String dataPrev, String fileDate, String rcm, boolean withMunicipality) {
		String local = withMunicipality ? "{\"1312\":{\"data\":{\"rcm\":" + rcm + "}}}" : "{}";
		return "{\"dataPrev\":\"" + dataPrev + "\",\"fileDate\":\"" + fileDate + "\",\"local\":" + local + "}";
	}

	/** Serves rcm-d0 and answers 204 for the other feeds the poll also requests. */
	private static final class Feed implements AutoCloseable {

		final URI base;
		private final HttpServer server;
		private final AtomicInteger d0Hits = new AtomicInteger();
		volatile byte[] body = new byte[0];
		volatile int failUntil = 0;

		Feed() throws IOException {
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			server.createContext("/", exchange -> {
				try {
					if (!exchange.getRequestURI().getPath().endsWith("/rcm-d0.json")) {
						exchange.sendResponseHeaders(204, -1);
						return;
					}
					int hit = d0Hits.incrementAndGet();
					if (hit <= failUntil) {
						exchange.sendResponseHeaders(503, -1);
						return;
					}
					exchange.sendResponseHeaders(200, body.length);
					exchange.getResponseBody().write(body);
				}
				finally {
					exchange.close();
				}
			});
			server.start();
			base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
		}

		@Override
		public void close() {
			server.stop(0);
		}
	}
}
