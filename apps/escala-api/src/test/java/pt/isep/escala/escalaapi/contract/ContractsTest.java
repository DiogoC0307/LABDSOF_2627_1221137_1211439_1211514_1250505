package pt.isep.escala.escalaapi.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TC-SCRUM-96-13: the OpenAPI checks of {@link Contracts} catch what they must, so ApiContractIT is not vacuous. */
class ContractsTest {

	@Test
	@DisplayName("TC-SCRUM-96-13 a body of the wrong shape, an undocumented status, an unknown path and a bad request body are all reported")
	void reportsDrift() {
		assertThat(Contracts.responseErrors("GET", "/api/v1/piquetes", 200, "application/json", "{\"not\":\"a list\"}"))
				.isNotEmpty();
		assertThat(Contracts.responseErrors("GET", "/api/v1/piquetes", 200, "application/json", "[]")).isEmpty();
		assertThat(Contracts.responseErrors("GET", "/api/v1/piquetes", 418, "application/json", "{}"))
				.singleElement().asString().contains("does not document");
		assertThat(Contracts.responseErrors("GET", "/api/v1/nothing-here", 200, "application/json", "[]"))
				.singleElement().asString().contains("is not in contracts/openapi.yaml");
		assertThat(Contracts.responseErrors("GET", "/api/v1/piquetes", 200, "text/plain", "[]")).isNotEmpty();
		assertThat(Contracts.requestErrors("POST", "/api/v1/piquetes", "{\"dutyDate\":\"2026-12-01\"}")).isEmpty();
		assertThat(Contracts.requestErrors("POST", "/api/v1/piquetes", "{\"dutyDate\":\"not a date\"}")).isNotEmpty();
	}
}
