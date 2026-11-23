package pt.isep.escala.elegibilidadeworker.extracao;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Turns one ExtracaoPedida into the proposal of ExtracaoConcluida v2 (SCRUM-75), following ADR 0011:
 *
 * <ul>
 * <li>no model extractor (AI_ENABLED=false), text the model must not get (empty, over 500 characters), a timeout,
 * invalid output or an error: {@code FALLBACK}, no periods, no model; escala-api shows the form;</li>
 * <li>a model answer: {@code MODEL} with its periods (maybe none), the model it reports and the latency;</li>
 * <li>whatever the extractor checked, the date window is checked again here: one period in the past or more than 6
 * months ahead empties the whole proposal ({@link ProposalRules}, as on 06).</li>
 * </ul>
 *
 * <p>The free text is passed to the extractor and nowhere else: it is not stored and not logged.
 */
@Component
public class UnavailabilityProposer {

	public enum Method {
		MODEL, BASELINE, FALLBACK
	}

	/** What ExtracaoConcluida carries, plus the outcome and reason for the worker's own log. */
	public record Proposal(List<UnavailabilityPeriod> periods, Method method, String model, Long latencyMs,
			ExtractionResult.Outcome outcome, String reason) {

		public Proposal {
			periods = List.copyOf(periods);
			Objects.requireNonNull(method, "method");
		}

		static Proposal fallback(Long latencyMs, ExtractionResult.Outcome outcome, String reason) {
			return new Proposal(List.of(), Method.FALLBACK, null, latencyMs, outcome, reason);
		}
	}

	private final ObjectProvider<UnavailabilityExtractor> modelExtractor;

	public UnavailabilityProposer(ObjectProvider<UnavailabilityExtractor> modelExtractor) {
		this.modelExtractor = modelExtractor;
	}

	public Proposal propose(String freeText, LocalDate referenceDate) {
		if (!ProposalRules.isUsableInput(freeText)) {
			return Proposal.fallback(null, null, "UNUSABLE_INPUT");
		}
		UnavailabilityExtractor extractor = modelExtractor.getIfAvailable();
		if (extractor == null) {
			return Proposal.fallback(null, null, "AI_OFF");
		}
		long started = System.nanoTime();
		ExtractionResult result = extractor.extract(freeText, referenceDate);
		long latencyMs = (System.nanoTime() - started) / 1_000_000;
		if (result.outcome() != ExtractionResult.Outcome.OK) {
			return Proposal.fallback(latencyMs, result.outcome(), result.reason());
		}
		if (!ProposalRules.withinWindow(result.periods(), referenceDate)) {
			return new Proposal(List.of(), Method.MODEL, result.model(), latencyMs, result.outcome(), "OUT_OF_WINDOW");
		}
		return new Proposal(result.periods(), Method.MODEL, result.model(), latencyMs, result.outcome(),
				result.reason());
	}
}
