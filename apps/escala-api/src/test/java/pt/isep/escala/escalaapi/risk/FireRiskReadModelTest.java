package pt.isep.escala.escalaapi.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The supervisor read model: labels, age and the stale warning. */
class FireRiskReadModelTest {

	private static final Instant NOON = Instant.parse("2026-11-21T12:00:00Z");

	private static final LocalDate DAY = LocalDate.parse("2026-11-21");

	private final DailyRiskRepository risks = mock(DailyRiskRepository.class);

	private final RiskQuery query = new RiskQuery(risks, Clock.fixed(NOON, ZoneOffset.UTC), "1312", Duration.ofHours(3));

	@Test
	@DisplayName("with no observation yet the answer names the municipality and carries no RCM")
	void none() {
		when(risks.findById("1312")).thenReturn(Optional.empty());

		TodayRisk today = new RiskController(query).today();

		assertThat(today).isEqualTo(TodayRisk.none("1312"));
		assertThat(today.rcm()).isNull();
		assertThat(today.label()).isNull();
		assertThat(today.fetchedAt()).isNull();
		assertThat(today.dataAgeHours()).isNull();
		assertThat(today.source()).isEqualTo("IPMA");
		assertThat(today.assumedHigher()).isFalse();
	}

	@Test
	@DisplayName("a fresh value keeps its label and is not marked higher")
	void fresh() {
		when(risks.findById("1312")).thenReturn(Optional.of(risk(4, NOON.minus(Duration.ofHours(1)), false)));

		TodayRisk today = query.today();

		assertThat(today.dico()).isEqualTo("1312");
		assertThat(today.rcm()).isEqualTo(4);
		assertThat(today.label()).isEqualTo("very high");
		assertThat(today.fetchedAt()).isEqualTo(NOON.minus(Duration.ofHours(1)));
		assertThat(today.dataAgeHours()).isEqualTo(1.0);
		assertThat(today.source()).isEqualTo("IPMA");
		assertThat(today.assumedHigher()).isFalse();
	}

	@Test
	@DisplayName("age at the corps threshold raises the warning, and a stored warning stays even when the value is fresh")
	void warning() {
		when(risks.findById("1312")).thenReturn(Optional.of(risk(2, NOON.minus(Duration.ofHours(3)), false)));
		assertThat(query.today().assumedHigher()).isTrue();
		assertThat(query.today().dataAgeHours()).isEqualTo(3.0);

		when(risks.findById("1312")).thenReturn(Optional.of(risk(2, NOON.plus(Duration.ofMinutes(30)), true)));
		TodayRisk future = query.today();
		assertThat(future.assumedHigher()).isTrue();
		assertThat(future.dataAgeHours()).isEqualTo(0.0);
	}

	@Test
	@DisplayName("the five RCM labels, and no label outside 1 to 5")
	void labels() {
		assertThat(RiskQuery.label(1)).isEqualTo("low");
		assertThat(RiskQuery.label(2)).isEqualTo("moderate");
		assertThat(RiskQuery.label(3)).isEqualTo("high");
		assertThat(RiskQuery.label(4)).isEqualTo("very high");
		assertThat(RiskQuery.label(5)).isEqualTo("maximum");
		assertThat(RiskQuery.label(0)).isNull();
		assertThat(RiskQuery.label(6)).isNull();
	}

	@Test
	@DisplayName("an older event is ignored; a later one, and one at the same instant, replaces the row")
	void apply() {
		DailyRisk row = new DailyRisk();
		assertThat(row.getDico()).isNull();
		assertThat(row.apply(3, DAY, NOON, "IPMA", false, NOON)).isTrue();
		assertThat(row.getRcm()).isEqualTo(3);
		assertThat(row.getForecastDate()).isEqualTo(DAY);
		assertThat(row.getFetchedAt()).isEqualTo(NOON);
		assertThat(row.getSource()).isEqualTo("IPMA");
		assertThat(row.isAssumedHigher()).isFalse();
		assertThat(row.getUpdatedAt()).isEqualTo(NOON);

		assertThat(row.apply(1, DAY, NOON, "SIMULATOR", true, NOON.minusSeconds(1))).isFalse();
		assertThat(row.getRcm()).isEqualTo(3);

		assertThat(row.apply(5, DAY.plusDays(1), NOON, "SIMULATOR", true, NOON)).isTrue();
		assertThat(row.getRcm()).isEqualTo(5);
		assertThat(row.getForecastDate()).isEqualTo(DAY.plusDays(1));
		assertThat(row.getSource()).isEqualTo("SIMULATOR");
		assertThat(row.isAssumedHigher()).isTrue();
	}

	private static DailyRisk risk(int rcm, Instant fetchedAt, boolean assumedHigher) {
		return new DailyRisk("1312", rcm, DAY, fetchedAt, "IPMA", assumedHigher, fetchedAt);
	}
}
