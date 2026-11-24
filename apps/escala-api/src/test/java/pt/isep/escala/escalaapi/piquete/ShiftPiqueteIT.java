package pt.isep.escala.escalaapi.piquete;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;

/**
 * A piquete as a shift the supervisor defines (SCRUM-65, D1): day, period, start, end and minimum crew, created with
 * POST and changed with PUT. Days are counted from today in Lisbon, so the tests hold on any date.
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
class ShiftPiqueteIT {

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

	private static LocalDate inDays(int days) {
		return LocalDate.now(PiqueteService.LISBON).plusDays(days);
	}

	/** The instant of a Lisbon wall-clock time, written with its offset as the PWA may send it. */
	private static String at(LocalDate day, String time) {
		return ZonedDateTime.of(day, LocalTime.parse(time), PiqueteService.LISBON).toOffsetDateTime().toString();
	}

	private static String utc(LocalDate day, String time) {
		return ZonedDateTime.of(day, LocalTime.parse(time), PiqueteService.LISBON).toInstant().toString();
	}

	private static String week(LocalDate day) {
		return "%d-W%02d".formatted(day.get(IsoFields.WEEK_BASED_YEAR), day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
	}

	private static String shift(LocalDate day, String period, String startsAt, String endsAt, int minimumCrew) {
		return """
				{"dutyDate":"%s","period":"%s","startsAt":"%s","endsAt":"%s","minimumCrew":%d}"""
				.formatted(day, period, startsAt, endsAt, minimumCrew);
	}

	private static MockHttpServletRequestBuilder create(String body) {
		return post("/api/v1/piquetes").contentType(MediaType.APPLICATION_JSON).content(body)
				.header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor());
	}

	private static MockHttpServletRequestBuilder change(UUID id, String body, String token) {
		return put("/api/v1/piquetes/" + id).contentType(MediaType.APPLICATION_JSON).content(body)
				.header(HttpHeaders.AUTHORIZATION, token);
	}

	private UUID saved(LocalDate dutyDate) {
		Piquete piquete = new Piquete(UUID.randomUUID(), dutyDate);
		piquete.addSlot(new Slot(UUID.randomUUID(), SlotRole.CREW_MEMBER, SlotStatus.OPEN, UUID.randomUUID()));
		return piquetes.save(piquete).getId();
	}

	@Test
	void theSupervisorCreatesAShiftAndItShowsInItsWeekWithItsTimes() throws Exception {
		LocalDate day = inDays(10);
		mockMvc.perform(create(shift(day, "DAY", at(day, "08:00"), at(day, "20:00"), 4)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dutyDate").value(day.toString()))
				.andExpect(jsonPath("$.period").value("DAY"))
				.andExpect(jsonPath("$.startsAt").value(utc(day, "08:00")))
				.andExpect(jsonPath("$.endsAt").value(utc(day, "20:00")))
				.andExpect(jsonPath("$.minimumCrew").value(4))
				.andExpect(jsonPath("$.slots.length()").value(1));

		mockMvc.perform(get("/api/v1/piquetes").param("week", week(day))
						.header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.dutyDate == '%s')].startsAt".formatted(day)).value(hasItem(utc(day, "08:00"))));
	}

	@Test
	void aNightShiftEndsTheNextMorning() throws Exception {
		LocalDate day = inDays(11);
		mockMvc.perform(create(shift(day, "NIGHT", at(day, "20:00"), at(day.plusDays(1), "08:00"), 3)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.period").value("NIGHT"))
				.andExpect(jsonPath("$.endsAt").value(utc(day.plusDays(1), "08:00")));
	}

	@Test
	void aShiftWithTimesNeedsEveryField() throws Exception {
		mockMvc.perform(create("{\"period\":\"DAY\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(hasItem("dutyDate")))
				.andExpect(jsonPath("$.errors[*].field").value(hasItem("startsAt")))
				.andExpect(jsonPath("$.errors[*].field").value(hasItem("endsAt")))
				.andExpect(jsonPath("$.errors[*].field").value(hasItem("minimumCrew")));
	}

	@Test
	void aShiftThatEndsBeforeItStartsIsRefused() throws Exception {
		LocalDate day = inDays(12);
		mockMvc.perform(create(shift(day, "DAY", at(day, "13:00"), at(day, "12:00"), 2)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("endsAt"))
				.andExpect(jsonPath("$.errors[0].code").value("ends_before_start"));
	}

	@Test
	void aShiftLongerThanADayIsRefused() throws Exception {
		LocalDate day = inDays(12);
		mockMvc.perform(create(shift(day, "DAY", at(day, "08:00"), at(day.plusDays(1), "09:00"), 2)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].code").value("too_long"));
	}

	@Test
	void aShiftStartsOnItsOwnDay() throws Exception {
		LocalDate day = inDays(12);
		mockMvc.perform(create(shift(day, "DAY", at(day.plusDays(1), "08:00"), at(day.plusDays(1), "20:00"), 2)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("startsAt"))
				.andExpect(jsonPath("$.errors[0].code").value("not_on_duty_date"));
	}

	@Test
	void aShiftNeedsAtLeastOneFirefighter() throws Exception {
		LocalDate day = inDays(12);
		mockMvc.perform(create(shift(day, "DAY", at(day, "08:00"), at(day, "20:00"), 0)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("minimumCrew"))
				.andExpect(jsonPath("$.errors[0].code").value("too_small"));
	}

	@Test
	void aShiftCannotBeCreatedOnADayThatHasPassed() throws Exception {
		LocalDate day = inDays(-3);
		mockMvc.perform(create(shift(day, "DAY", at(day, "08:00"), at(day, "20:00"), 2)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("dutyDate"))
				.andExpect(jsonPath("$.errors[0].code").value("in_the_past"));
	}

	@Test
	void theSupervisorChangesAShiftAndKeepsItsSlots() throws Exception {
		LocalDate day = inDays(14);
		UUID id = saved(day);
		LocalDate moved = day.plusDays(1);

		mockMvc.perform(change(id, shift(moved, "NIGHT", at(moved, "20:00"), at(moved.plusDays(1), "08:00"), 5),
						MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.dutyDate").value(moved.toString()))
				.andExpect(jsonPath("$.period").value("NIGHT"))
				.andExpect(jsonPath("$.startsAt").value(utc(moved, "20:00")))
				.andExpect(jsonPath("$.minimumCrew").value(5))
				.andExpect(jsonPath("$.slots.length()").value(1));
	}

	@Test
	void changingAShiftChecksItAsCreatingOneDoes() throws Exception {
		LocalDate day = inDays(15);
		UUID id = saved(day);

		mockMvc.perform(change(id, shift(day, "DAY", at(day, "13:00"), at(day, "12:00"), 2), MockTokens.supervisor()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].code").value("ends_before_start"));
	}

	@Test
	void aShiftOnADayThatHasPassedCannotChange() throws Exception {
		LocalDate past = inDays(-2);
		UUID id = saved(past);
		LocalDate day = inDays(5);

		mockMvc.perform(change(id, shift(day, "DAY", at(day, "08:00"), at(day, "20:00"), 2), MockTokens.supervisor()))
				.andExpect(status().isConflict());
	}

	@Test
	void changingAShiftThatDoesNotExistIsNotFound() throws Exception {
		LocalDate day = inDays(5);
		mockMvc.perform(change(UUID.randomUUID(), shift(day, "DAY", at(day, "08:00"), at(day, "20:00"), 2),
						MockTokens.supervisor()))
				.andExpect(status().isNotFound());
	}

	@Test
	void onlyTheSupervisorChangesAShift() throws Exception {
		LocalDate day = inDays(16);
		UUID id = saved(day);
		String body = shift(day, "DAY", at(day, "08:00"), at(day, "20:00"), 2);

		mockMvc.perform(change(id, body, MockTokens.member())).andExpect(status().isForbidden());
		mockMvc.perform(change(id, body, MockTokens.admin())).andExpect(status().isForbidden());
		mockMvc.perform(put("/api/v1/piquetes/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
	}
}
