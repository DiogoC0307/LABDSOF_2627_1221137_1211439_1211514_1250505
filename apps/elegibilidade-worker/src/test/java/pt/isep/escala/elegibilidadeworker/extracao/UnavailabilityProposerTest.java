package pt.isep.escala.elegibilidadeworker.extracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import pt.isep.escala.elegibilidadeworker.extracao.ExtractionResult.Outcome;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityProposer.Method;
import pt.isep.escala.elegibilidadeworker.extracao.UnavailabilityProposer.Proposal;

/** TC-SCRUM-75-21: MODEL or FALLBACK (ADR 0011), and the date window checked again in the worker. */
class UnavailabilityProposerTest {

	private static final LocalDate REF = LocalDate.parse("2026-10-26");
	private static final UnavailabilityPeriod FRIDAY = UnavailabilityPeriod.ofDays(LocalDate.parse("2026-10-30"),
			LocalDate.parse("2026-10-30"));

	private final AtomicInteger calls = new AtomicInteger();

	private UnavailabilityProposer proposer(UnavailabilityExtractor extractor) {
		StaticListableBeanFactory beans = new StaticListableBeanFactory();
		if (extractor != null) {
			beans.addBean("model", (UnavailabilityExtractor) (text, ref) -> {
				calls.incrementAndGet();
				return extractor.extract(text, ref);
			});
		}
		return new UnavailabilityProposer(beans.getBeanProvider(UnavailabilityExtractor.class));
	}

	@Test
	@DisplayName("TC-SCRUM-75-21 with AI off (no model extractor) the answer is FALLBACK, no periods, no model, no latency")
	void aiOff() {
		Proposal proposal = proposer(null).propose("não posso sexta", REF);
		assertThat(proposal.method()).isEqualTo(Method.FALLBACK);
		assertThat(proposal.periods()).isEmpty();
		assertThat(proposal.model()).isNull();
		assertThat(proposal.latencyMs()).isNull();
		assertThat(proposal.reason()).isEqualTo("AI_OFF");
	}

	@Test
	@DisplayName("TC-SCRUM-75-21 a model answer is MODEL with its periods, the model it reports and the latency")
	void modelAnswer() {
		Proposal proposal = proposer((text, ref) -> new ExtractionResult(List.of(FRIDAY), Outcome.OK, null, 0.001,
				"claude-haiku-x")).propose("não posso sexta", REF);
		assertThat(proposal.method()).isEqualTo(Method.MODEL);
		assertThat(proposal.periods()).containsExactly(FRIDAY);
		assertThat(proposal.model()).isEqualTo("claude-haiku-x");
		assertThat(proposal.latencyMs()).isNotNull().isNotNegative();
	}

	@Test
	@DisplayName("TC-SCRUM-75-21 an empty model answer is MODEL with no periods: the model was asked and found nothing")
	void emptyModelAnswer() {
		Proposal proposal = proposer((text, ref) -> new ExtractionResult(List.of(), Outcome.OK, null, null, "haiku"))
				.propose("só posso sexta", REF);
		assertThat(proposal.method()).isEqualTo(Method.MODEL);
		assertThat(proposal.periods()).isEmpty();
	}

	@ParameterizedTest(name = "TC-SCRUM-75-21 {0} from the model is FALLBACK: no periods, no model")
	@EnumSource(value = Outcome.class, names = { "NO_VALID_OUTPUT", "TIMEOUT", "ERROR" })
	void failedCallIsFallback(Outcome outcome) {
		Proposal proposal = proposer((text, ref) -> ExtractionResult.failed(outcome, outcome.name()))
				.propose("não posso sexta", REF);
		assertThat(proposal.method()).isEqualTo(Method.FALLBACK);
		assertThat(proposal.periods()).isEmpty();
		assertThat(proposal.model()).isNull();
		assertThat(proposal.latencyMs()).isNotNull();
	}

	@Test
	@DisplayName("TC-SCRUM-75-21 the worker checks the window again: one period out of it empties the whole proposal")
	void windowCheckedAgain() {
		var past = UnavailabilityPeriod.ofDays(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-01"));
		Proposal proposal = proposer((text, ref) -> new ExtractionResult(List.of(FRIDAY, past), Outcome.OK, null, null,
				"haiku")).propose("não posso sexta", REF);
		assertThat(proposal.method()).isEqualTo(Method.MODEL);
		assertThat(proposal.periods()).isEmpty();
		assertThat(proposal.reason()).isEqualTo("OUT_OF_WINDOW");
	}

	@Test
	@DisplayName("TC-SCRUM-75-21 empty text or text over 500 characters never reaches the model: FALLBACK")
	void unusableInputNotSent() {
		UnavailabilityProposer proposer = proposer((text, ref) -> ExtractionResult.empty());
		assertThat(proposer.propose(" ", REF).method()).isEqualTo(Method.FALLBACK);
		assertThat(proposer.propose("x".repeat(501), REF).method()).isEqualTo(Method.FALLBACK);
		assertThat(calls).hasValue(0);
	}
}
