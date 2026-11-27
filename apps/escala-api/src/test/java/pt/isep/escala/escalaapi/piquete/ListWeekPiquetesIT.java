package pt.isep.escala.escalaapi.piquete;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;

/** {@code GET /api/v1/piquetes?week=}: the week's schedule, Monday to Sunday (SCRUM-65). Each test uses its own week. */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
class ListWeekPiquetesIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	PiqueteRepository piquetes;

	@Test
	void aWeekWithNoPiquetesIsAnEmptySchedule() throws Exception {
		mockMvc.perform(get("/api/v1/piquetes").param("week", "2030-W10")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void theWeekHasItsPiquetesFromMondayToSundayInDateOrder() throws Exception {
		// 2026-W48 runs from Monday 2026-11-23 to Sunday 2026-11-29.
		save(LocalDate.parse("2026-11-29"));
		save(LocalDate.parse("2026-11-22")); // Sunday of the week before
		save(LocalDate.parse("2026-11-23"));
		save(LocalDate.parse("2026-11-30")); // Monday of the week after
		save(LocalDate.parse("2026-11-26"));

		mockMvc.perform(get("/api/v1/piquetes").param("week", "2026-W48")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].dutyDate").value("2026-11-23"))
				.andExpect(jsonPath("$[1].dutyDate").value("2026-11-26"))
				.andExpect(jsonPath("$[2].dutyDate").value("2026-11-29"));
	}

	@Test
	void aMemberCanReadTheWeek() throws Exception {
		save(LocalDate.parse("2031-03-05"));

		mockMvc.perform(get("/api/v1/piquetes").param("week", "2031-W10")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void anInvalidWeekIsBadRequestNamingTheField() throws Exception {
		mockMvc.perform(get("/api/v1/piquetes").param("week", "2026-48")
						.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("week"))
				.andExpect(jsonPath("$.errors[0].code").value("invalid_week"));
	}

	@Test
	void withoutATokenTheWeekIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/piquetes").param("week", "2026-W48"))
				.andExpect(status().isUnauthorized());
	}

	private void save(LocalDate dutyDate) {
		Piquete piquete = new Piquete(UUID.randomUUID(), dutyDate);
		piquete.addSlot(new Slot(UUID.randomUUID(), SlotRole.CREW_MEMBER, SlotStatus.OPEN, UUID.randomUUID()));
		piquetes.save(piquete);
	}
}
