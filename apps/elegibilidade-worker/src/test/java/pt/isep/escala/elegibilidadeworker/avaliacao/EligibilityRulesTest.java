package pt.isep.escala.elegibilidadeworker.avaliacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.isep.escala.elegibilidadeworker.messaging.MalformedEventException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class EligibilityRulesTest {

	private static final ObjectMapper JSON = new ObjectMapper();

	private static final String SATURDAY_MORNING = """
			{"slotId":"5f0c3a52-8d1e-4a5e-9a51-3a3c1f3b9d10","start":"2026-12-05T06:00:00Z","end":"2026-12-05T14:00:00Z"}
			""";

	@Test
	@DisplayName("A free member on the active roster is ELIGIBLE")
	void aFreeActiveMemberIsEligible() {
		assertThat(EligibilityRules.violated(slot(), candidate(true, "[]", "[]"))).isEmpty();
		assertThat(EligibilityRules.result(EligibilityRules.violated(slot(), candidate(true, "[]", "[]"))))
				.isEqualTo("ELIGIBLE");
	}

	@Test
	@DisplayName("TC-SCRUM-65-04 a member who is not on the active roster breaks RN01")
	void notOnTheActiveRoster() {
		assertThat(EligibilityRules.violated(slot(), candidate(false, "[]", "[]"))).containsExactly("RN01");
	}

	@Test
	@DisplayName("TC-SCRUM-65-17 a member on another shift at the same time breaks RN05")
	void anotherShiftAtTheSameTime() {
		String other = "[{\"start\":\"2026-12-05T10:00:00Z\",\"end\":\"2026-12-05T18:00:00Z\"}]";
		assertThat(EligibilityRules.violated(slot(), candidate(true, "[]", other))).containsExactly("RN05");
	}

	@Test
	@DisplayName("TC-SCRUM-65-18 a shift that ends exactly when this one starts is not an overlap")
	void aShiftThatEndsWhenThisOneStarts() {
		String other = "[{\"start\":\"2026-12-04T22:00:00Z\",\"end\":\"2026-12-05T06:00:00Z\"}]";
		assertThat(EligibilityRules.violated(slot(), candidate(true, "[]", other))).isEmpty();
	}

	@Test
	@DisplayName("TC-SCRUM-65-19 a night shift from 20:00 to 08:00 overlaps a shift from 06:00 the next morning")
	void aNightShiftOverlapsTheNextMorning() {
		String night = "[{\"start\":\"2026-12-04T20:00:00Z\",\"end\":\"2026-12-05T08:00:00Z\"}]";
		assertThat(EligibilityRules.violated(slot(), candidate(true, "[]", night))).containsExactly("RN05");
	}

	@Test
	@DisplayName("TC-SCRUM-65-02 a member who can't come during the shift breaks RN06")
	void unavailableDuringTheShift() {
		String saturday = "[{\"start\":\"2026-12-05T00:00:00Z\",\"end\":\"2026-12-06T00:00:00Z\"}]";
		assertThat(EligibilityRules.violated(slot(), candidate(true, saturday, "[]"))).containsExactly("RN06");
	}

	@Test
	@DisplayName("TC-SCRUM-65-21 a period that ends exactly when the shift starts is not an overlap")
	void unavailableUntilTheShiftStarts() {
		String night = "[{\"start\":\"2026-12-04T22:00:00Z\",\"end\":\"2026-12-05T06:00:00Z\"}]";
		assertThat(EligibilityRules.violated(slot(), candidate(true, night, "[]"))).isEmpty();
	}

	@Test
	@DisplayName("Every broken rule is listed, in rule order, and the result is INELIGIBLE")
	void everyBrokenRuleIsListed() {
		String period = "[{\"start\":\"2026-12-05T12:00:00Z\",\"end\":\"2026-12-05T13:00:00Z\"}]";
		JsonNode candidate = candidate(false, period, period);
		assertThat(EligibilityRules.violated(slot(), candidate)).containsExactly("RN01", "RN05", "RN06");
		assertThat(EligibilityRules.result(EligibilityRules.violated(slot(), candidate))).isEqualTo("INELIGIBLE");
	}

	@Test
	@DisplayName("A period that is not an instant is a malformed event, not a guess")
	void anUnreadablePeriodIsMalformed() {
		String broken = "[{\"start\":\"Saturday\",\"end\":\"2026-12-05T13:00:00Z\"}]";
		assertThatThrownBy(() -> EligibilityRules.violated(slot(), candidate(true, broken, "[]")))
				.isInstanceOf(MalformedEventException.class);
	}

	private static JsonNode slot() {
		return JSON.readTree(SATURDAY_MORNING);
	}

	private static JsonNode candidate(boolean inActiveRoster, String unavailabilities, String otherAssignments) {
		return JSON.readTree("""
				{"memberId":"00000000-0000-4000-8000-000000000001","inActiveRoster":%s,"withinAgeLimit":true,
				 "certifications":[],"licenceClasses":[],"unavailabilities":%s,"otherAssignments":%s}
				""".formatted(inActiveRoster, unavailabilities, otherAssignments));
	}
}
