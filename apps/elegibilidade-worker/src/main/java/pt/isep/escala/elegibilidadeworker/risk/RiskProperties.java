package pt.isep.escala.elegibilidadeworker.risk;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Where and how often the worker asks IPMA (ADR 0010). The staleness threshold is the corps setting (ADR 0013). */
@ConfigurationProperties(prefix = "escala.risk")
class RiskProperties {

	private boolean enabled = true;

	private URI baseUrl = URI.create("https://api.ipma.pt");

	private String dico = "1312";

	private Duration staleness = Duration.ofHours(3);

	private Duration timeout = Duration.ofSeconds(5);

	private int retries = 2;

	boolean isEnabled() {
		return enabled;
	}

	void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	URI getBaseUrl() {
		return baseUrl;
	}

	void setBaseUrl(URI baseUrl) {
		this.baseUrl = baseUrl;
	}

	String getDico() {
		return dico;
	}

	void setDico(String dico) {
		this.dico = dico;
	}

	Duration getStaleness() {
		return staleness;
	}

	void setStaleness(Duration staleness) {
		this.staleness = staleness;
	}

	Duration getTimeout() {
		return timeout;
	}

	void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	int getRetries() {
		return retries;
	}

	void setRetries(int retries) {
		this.retries = retries;
	}

	/** IPMA when the host is the real service; the simulator otherwise (tests and the evaluation profile). */
	String source() {
		String host = baseUrl.getHost();
		return host != null && host.equalsIgnoreCase("api.ipma.pt") ? "IPMA" : "SIMULATOR";
	}
}
