package pt.isep.escala.escalaapi.piquete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
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
import java.util.List;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;

/**
 * A piquete as a shift the supervisor defines (SCRUM-65, D1): day, period, start, end and the crew it needs per role,
 * created with POST and changed with PUT. Days are counted from today in Lisbon, so the tests hold on any date.
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

	@Autowired
	TransactionTemplate transactions;

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

	/** A shift that needs that many crew members, and no crew chief or driver. */
	private static String shift(LocalDate day, String period, String startsAt, String endsAt, int crewMembers) {
		return shift(day, period, startsAt, endsAt, crew(0, 0, crewMembers));
	}

	private static String shift(LocalDate day, String period, String startsAt, String endsAt, String crew) {
		return """
				{"dutyDate":"%s","period":"%s","startsAt":"%s","endsAt":"%s","crew":%s}"""
				.formatted(day, period, startsAt, endsAt, crew);
	}

	private static String crew(int crewChiefs, int drivers, int crewMembers) {
		return """
				[{"role":"CREW_CHIEF","count":%d},{"role":"DRIVER","count":%d},{"role":"CREW_MEMBER","count":%d}]"""
				.formatted(crewChiefs, drivers, crewMembers);
	}

	private String dayShift(LocalDate day, String crew) {
		return shift(day, "DAY", at(day, "08:00"), at(day, "20:00"), crew);
	}

	private List<SlotStatus> statuses(UUID id, SlotRole role) {
		return transactions.execute(tx -> piquetes.findById(id).orElseThrow().getSlots().stream()
				.filter(slot -> slot.getRole() == role)
				.map(Slot::getStatus)
				.toList());
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
		return saved(dutyDate, SlotStatus.OPEN);
	}

	/** A piquete with one CREW_MEMBER slot for each status given. */
	private UUID saved(LocalDate dutyDate, SlotStatus... statuses) {
		Piquete piquete = new Piquete(UUID.randomUUID(), dutyDate);
		for (SlotStatus status : statuses) {
			piquete.addSlot(new Slot(UUID.randomUUID(), SlotRole.CREW_MEMBER, status, UUID.randomUUID()));
		}
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
				.andExpect(jsonPath("$.slots.length()").value(4));

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
				.andExpect(jsonPath("$.errors[*].field").value(hasItem("crew")));
	}

	@Test
	void aShiftHasOneSlotForEachPersonOfEachRole() throws Exception {
		LocalDate day = inDays(10);
		mockMvc.perform(create(dayShift(day, crew(1, 1, 3))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.minimumCrew").value(5))
				.andExpect(jsonPath("$.slots[*].role").value(
						contains("CREW_CHIEF", "DRIVER", "CREW_MEMBER", "CREW_MEMBER", "CREW_MEMBER")))
				.andExpect(jsonPath("$.slots[*].status").value(
						contains("PENDING", "PENDING", "PENDING", "PENDING", "PENDING")));
	}

	@Test
	void aRoleLeftOutNeedsNobody() throws Exception {
		LocalDate day = inDays(10);
		mockMvc.perform(create(dayShift(day, "[{\"role\":\"DRIVER\",\"count\":2}]")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.minimumCrew").value(2))
				.andExpect(jsonPath("$.slots[*].role").value(contains("DRIVER", "DRIVER")));
	}

	@Test
	void theCrewIsCheckedAsAWhole() throws Exception {
		LocalDate day = inDays(12);
		String[][] cases = {
				{"[]", "required"},
				{crew(0, 0, 0), "too_small"},
				{crew(10, 10, 31), "too_large"},
				{"[{\"role\":\"DRIVER\",\"count\":1},{\"role\":\"DRIVER\",\"count\":1}]", "duplicate"},
				{"[{\"role\":\"DRIVER\",\"count\":-1},{\"role\":\"CREW_MEMBER\",\"count\":2}]", "invalid"},
		};
		for (String[] c : cases) {
			mockMvc.perform(create(dayShift(day, c[0])))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors[0].field").value("crew"))
					.andExpect(jsonPath("$.errors[0].code").value(c[1]));
		}
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
				.andExpect(jsonPath("$.errors[0].field").value("crew"))
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
		UUID slotId = transactions.execute(tx -> piquetes.findById(id).orElseThrow().getSlots().getFirst().getId());
		LocalDate moved = day.plusDays(1);

		mockMvc.perform(change(id, shift(moved, "NIGHT", at(moved, "20:00"), at(moved.plusDays(1), "08:00"), 1),
						MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.dutyDate").value(moved.toString()))
				.andExpect(jsonPath("$.period").value("NIGHT"))
				.andExpect(jsonPath("$.startsAt").value(utc(moved, "20:00")))
				.andExpect(jsonPath("$.minimumCrew").value(1))
				.andExpect(jsonPath("$.slots.length()").value(1))
				.andExpect(jsonPath("$.slots[0].id").value(slotId.toString()));
	}

	@Test
	void aRoleThatNeedsMorePeopleGetsNewSlots() throws Exception {
		LocalDate day = inDays(14);
		UUID id = saved(day, SlotStatus.FILLED);

		mockMvc.perform(change(id, dayShift(day, crew(1, 0, 3)), MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.minimumCrew").value(4))
				.andExpect(jsonPath("$.slots.length()").value(4));

		assertThat(statuses(id, SlotRole.CREW_CHIEF)).containsExactly(SlotStatus.PENDING);
		assertThat(statuses(id, SlotRole.CREW_MEMBER))
				.containsExactlyInAnyOrder(SlotStatus.FILLED, SlotStatus.PENDING, SlotStatus.PENDING);
	}

	@Test
	void aRoleThatNeedsFewerPeopleLosesItsOpenSlotsFirst() throws Exception {
		LocalDate day = inDays(14);
		UUID id = saved(day, SlotStatus.FILLED, SlotStatus.ELIGIBLE, SlotStatus.OPEN, SlotStatus.PROPOSED);

		mockMvc.perform(change(id, dayShift(day, crew(0, 0, 3)), MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots.length()").value(3));
		assertThat(statuses(id, SlotRole.CREW_MEMBER))
				.containsExactlyInAnyOrder(SlotStatus.FILLED, SlotStatus.ELIGIBLE, SlotStatus.PROPOSED);

		mockMvc.perform(change(id, dayShift(day, crew(0, 2, 2)), MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.minimumCrew").value(4));
		assertThat(statuses(id, SlotRole.CREW_MEMBER)).containsExactlyInAnyOrder(SlotStatus.FILLED, SlotStatus.PROPOSED);
		assertThat(statuses(id, SlotRole.DRIVER)).containsExactly(SlotStatus.PENDING, SlotStatus.PENDING);
	}

	@Test
	void aRoleCannotNeedFewerPeopleThanAreProposedOrHoldingItsSlots() throws Exception {
		LocalDate day = inDays(14);
		UUID id = saved(day, SlotStatus.FILLED, SlotStatus.PROPOSED, SlotStatus.OPEN);

		mockMvc.perform(change(id, dayShift(day, crew(1, 0, 1)), MockTokens.supervisor()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("crew"))
				.andExpect(jsonPath("$.errors[0].code").value("below_filled"));
		assertThat(statuses(id, SlotRole.CREW_MEMBER)).hasSize(3);
		assertThat(statuses(id, SlotRole.CREW_CHIEF)).isEmpty();
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
