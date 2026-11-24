package pt.isep.escala.elegibilidadeworker.risk;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * One poll of the RCM and warnings feeds (ADR 0010). Timeout 5 s and two retries. The base URL is the real service or
 * {@code tools/ipma-sim}; this class does not choose which.
 */
class IpmaClient {

	static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");

	private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String RCM_D0 = "/open-data/forecast/meteorology/rcm/rcm-d0.json";

	private final HttpClient http;

	private final ObjectMapper json = JsonMapper.builder().build();

	IpmaClient(HttpClient http) {
		this.http = http;
	}

	IpmaClient() {
		this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
	}

	/** Today's RCM for {@code dico}. Also requests d1, d2 and the warnings feed; only d0 decides the outcome. */
	RiskCache.Reading fetch(URI baseUrl, String dico, Duration timeout, int retries, Duration staleness, Clock clock) {
		byte[] body = get(baseUrl.resolve(RCM_D0), timeout, retries);
		ignore(baseUrl.resolve("/open-data/forecast/meteorology/rcm/rcm-d1.json"), timeout);
		ignore(baseUrl.resolve("/open-data/forecast/meteorology/rcm/rcm-d2.json"), timeout);
		ignore(baseUrl.resolve("/open-data/forecast/warnings/warnings_www.json"), timeout);
		if (body == null) {
			return new RiskCache.Reading(RiskOutcome.UNAVAILABLE, null, null, null);
		}
		return read(body, dico, staleness, clock);
	}

	private RiskCache.Reading read(byte[] body, String dico, Duration staleness, Clock clock) {
		JsonNode root;
		try {
			root = json.readTree(new String(body, StandardCharsets.UTF_8));
		}
		catch (Exception e) {
			return new RiskCache.Reading(RiskOutcome.INVALID, null, null, null);
		}
		if (root == null || !root.isObject()) {
			return new RiskCache.Reading(RiskOutcome.INVALID, null, null, null);
		}
		JsonNode municipality = root.path("local").path(dico);
		if (municipality.isMissingNode() || municipality.isNull()) {
			return new RiskCache.Reading(RiskOutcome.INCOMPLETE, null, null, null);
		}
		JsonNode rcmNode = municipality.path("data").path("rcm");
		if (!rcmNode.isNumber()) {
			return new RiskCache.Reading(RiskOutcome.INVALID, null, null, null);
		}
		int rcm = rcmNode.asInt();
		if (rcm < 1 || rcm > 5) {
			return new RiskCache.Reading(RiskOutcome.INVALID, null, null, null);
		}
		LocalDate forecast;
		Instant fileAt;
		try {
			forecast = LocalDate.parse(root.path("dataPrev").asString());
			LocalDateTime fileLocal = LocalDateTime.parse(root.path("fileDate").asString(), FILE_DATE);
			fileAt = fileLocal.atZone(LISBON).toInstant();
		}
		catch (DateTimeParseException e) {
			return new RiskCache.Reading(RiskOutcome.INVALID, null, null, null);
		}
		Instant now = clock.instant();
		LocalDate today = LocalDate.now(clock.withZone(LISBON));
		boolean oldFile = !fileAt.isAfter(now.minus(staleness));
		if (forecast.isBefore(today) || oldFile) {
			return new RiskCache.Reading(RiskOutcome.DELAYED, rcm, forecast, fileAt);
		}
		return new RiskCache.Reading(RiskOutcome.OK, rcm, forecast, fileAt);
	}

	private byte[] get(URI uri, Duration timeout, int retries) {
		int attempts = Math.max(1, retries + 1);
		for (int attempt = 0; attempt < attempts; attempt++) {
			try {
				HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
				HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					return response.body();
				}
			}
			catch (IOException e) {
				// timeout or the simulator refused: try again, then treat the feed as unavailable
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}
		return null;
	}

	private void ignore(URI uri, Duration timeout) {
		get(uri, timeout, 0);
	}
}
