package pt.isep.escala.escalaapi.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TC-SCRUM-96-13: the OpenAPI checks of {@link Contracts} catch what they must, so ApiContractIT is not vacuous. */
class ContractsTest {

	@Test
	@DisplayName("TC-SCRUM-96-13 a body of the wrong shape, an undocumented status, an unknown path, a bad request body and an error without a body are all reported")
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
		// since SCRUM-88 every error has a JSON body: an empty one is reported, and so is an error the spec leaves without one
		assertThat(Contracts.responseErrors("GET", "/api/v1/piquetes", 401, "application/json", "")).singleElement()
				.asString().contains("is an error without a body");
		assertThat(Contracts.responseErrors("POST", "/assignments/{id}/confirm", 409, null, "")).singleElement()
				.asString().contains("the spec describes without a body");
		assertThat(Contracts.errorResponsesWithoutBody("POST", "/assignments/{id}/confirm"))
				.contains("POST /assignments/{id}/confirm 409");
		assertThat(Contracts.errorResponsesWithoutBody("GET", "/api/v1/piquetes")).isEmpty();
	}

	@Test
	@DisplayName("A shift's crew is a count per role (SCRUM-65): a known role with a count passes, an unknown role does not")
	void describesTheCrewOfAShift() {
		String shift = """
				{"dutyDate":"2026-12-01","period":"DAY","startsAt":"2026-12-01T08:00:00Z","endsAt":"2026-12-01T20:00:00Z",\
				"crew":[%s]}""";
		assertThat(Contracts.requestErrors("POST", "/api/v1/piquetes",
				shift.formatted("{\"role\":\"CREW_CHIEF\",\"count\":1},{\"role\":\"CREW_MEMBER\",\"count\":3}"))).isEmpty();
		assertThat(Contracts.requestErrors("POST", "/api/v1/piquetes",
				shift.formatted("{\"role\":\"PILOT\",\"count\":1}"))).isNotEmpty();
		assertThat(Contracts.requestErrors("POST", "/api/v1/piquetes",
				shift.formatted("{\"role\":\"DRIVER\",\"count\":-1}"))).isNotEmpty();
	}
}
