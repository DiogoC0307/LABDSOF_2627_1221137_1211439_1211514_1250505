package pt.isep.escala.elegibilidadeworker.messaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads the Grafana dashboard of SCRUM-97 ({@code infra/observability/dashboards/escala-operations.json}) for
 * TC-SCRUM-97-10. Every series selector in it names its service with the {@code application} label, so a test can
 * check that each metric the dashboard asks a service for is in that service's Prometheus output.
 */
final class DashboardQueries {

	static final Path DASHBOARD = Path.of("../../infra/observability/dashboards/escala-operations.json");

	private static final Pattern SELECTOR = Pattern.compile("([a-zA-Z_:][a-zA-Z0-9_:]*)\\{([^}]*)}");
	private static final Pattern APPLICATION = Pattern.compile("application(=~|=)\"([^\"]*)\"");

	private final JsonNode dashboard;
	private final List<String> expressions = new ArrayList<>();

	DashboardQueries() throws IOException {
		dashboard = new ObjectMapper().readTree(Files.readString(DASHBOARD));
		for (JsonNode panel : dashboard.path("panels")) {
			for (JsonNode target : panel.path("targets")) {
				expressions.add(target.path("expr").asString());
			}
		}
	}

	JsonNode json() {
		return dashboard;
	}

	List<String> expressions() {
		return expressions;
	}

	/** Metric names whose selector has no {@code application} label. */
	Set<String> selectorsWithoutApplication() {
		Set<String> names = new TreeSet<>();
		forEachSelector((name, labels) -> {
			if (!APPLICATION.matcher(labels).find()) {
				names.add(name);
			}
		});
		return names;
	}

	/** Metric names the dashboard asks {@code application} for (an exact match, or a regex that matches it). */
	Set<String> metricsOf(String application) {
		Set<String> names = new TreeSet<>();
		forEachSelector((name, labels) -> {
			Matcher matcher = APPLICATION.matcher(labels);
			if (matcher.find() && ("=".equals(matcher.group(1)) ? matcher.group(2).equals(application)
					: Pattern.matches(matcher.group(2), application))) {
				names.add(name);
			}
		});
		return names;
	}

	/** Every metric name in the dashboard. */
	Set<String> allMetrics() {
		Set<String> names = new TreeSet<>();
		forEachSelector((name, labels) -> names.add(name));
		return names;
	}

	/** The series names in a Prometheus text output. */
	static Set<String> seriesNames(String scrape) {
		Set<String> names = new TreeSet<>();
		for (String line : scrape.split("\n")) {
			if (!line.isBlank() && !line.startsWith("#")) {
				int end = line.indexOf('{');
				names.add(end < 0 ? line.substring(0, line.indexOf(' ')) : line.substring(0, end));
			}
		}
		return names;
	}

	private void forEachSelector(BiConsumer<String, String> action) {
		for (String expression : expressions) {
			Matcher matcher = SELECTOR.matcher(expression);
			while (matcher.find()) {
				action.accept(matcher.group(1), matcher.group(2));
			}
		}
	}
}
