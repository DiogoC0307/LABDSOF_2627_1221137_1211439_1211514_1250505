package pt.isep.escala.escalaapi.piquete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;

@SpringBootTest(properties = {
		"spring.rabbitmq.dynamic=true",
		"escala.shift-confirmation.poll-ms=3600000"
})
@AutoConfigureMockMvc
@Testcontainers
class ShiftConfirmationIT {

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

	@Autowired
	SlotRepository slots;

	@BeforeEach
	void clean() {
		piquetes.deleteAll();
	}

	private Slot saveShiftSlot(Instant shiftStartsAt) {
		Piquete piquete = new Piquete(UUID.randomUUID(), shiftStartsAt.atZone(ZoneOffset.UTC).toLocalDate());
		Slot slot = new Slot(UUID.randomUUID(), SlotRole.CREW_MEMBER, SlotStatus.FILLED, UUID.randomUUID(), shiftStartsAt);
		piquete.addSlot(slot);
		piquetes.save(piquete);
		return slot;
	}

	@Test
	@DisplayName("TC-SCRUM-70-01 shift tomorrow at 20:00 sends confirmation prompt at 20:00 today")
	void ac1Prompts24HoursBeforeShift() {
		Instant shiftTomorrowAt2000 = Instant.parse("2026-11-26T20:00:00Z");
		Instant nowTodayAt2000 = Instant.parse("2026-11-25T20:00:00Z");
		Slot slot = saveShiftSlot(shiftTomorrowAt2000);

		var service = new ShiftConfirmationService(slots, Clock.fixed(nowTodayAt2000, ZoneOffset.UTC), 24);
		List<PresencePrompt> prompts = service.dispatchDuePrompts();

		assertThat(prompts).hasSize(1);
		assertThat(prompts.get(0).slotId()).isEqualTo(slot.getId());
		assertThat(prompts.get(0).message())
				.isEqualTo("Confirm your shift tomorrow at 20:00: I'm coming / I can't come");
	}

	@Test
	@DisplayName("TC-SCRUM-70-02 when advance is set to 12 h, prompt is sent at 12 h before shift, not at 24 h")
	void ac2PromptsAt12HoursWhenConfigured() {
		Instant shiftTomorrowAt2000 = Instant.parse("2026-11-26T20:00:00Z");
		saveShiftSlot(shiftTomorrowAt2000);

		var at24h = new ShiftConfirmationService(
				slots, Clock.fixed(Instant.parse("2026-11-25T20:00:00Z"), ZoneOffset.UTC), 12);
		assertThat(at24h.dispatchDuePrompts()).isEmpty();

		var at12h = new ShiftConfirmationService(
				slots, Clock.fixed(Instant.parse("2026-11-26T08:00:00Z"), ZoneOffset.UTC), 12);
		assertThat(at12h.dispatchDuePrompts()).hasSize(1);
	}

	@Test
	@DisplayName("TC-SCRUM-70-03 member answers I'm coming then later presses I can't come: slot is freed")
	void ac3CannotComeAfterComingFreesSlot() throws Exception {
		Slot slot = saveShiftSlot(Instant.parse("2026-11-26T20:00:00Z"));
		String url = "/api/v1/slots/" + slot.getId() + "/presence";

		mockMvc.perform(post(url)
						.header(HttpHeaders.AUTHORIZATION, MockTokens.member())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\":\"COMING\"}"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.presenceStatus").value("COMING"))
				.andExpect(jsonPath("$.slotStatus").value("FILLED"))
				.andExpect(jsonPath("$.slotFreed").value(false));

		mockMvc.perform(post(url)
						.header(HttpHeaders.AUTHORIZATION, MockTokens.member())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\":\"CANNOT_COME\"}"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.presenceStatus").value("CANNOT_COME"))
				.andExpect(jsonPath("$.slotStatus").value("OPEN"))
				.andExpect(jsonPath("$.slotFreed").value(true));
	}
}