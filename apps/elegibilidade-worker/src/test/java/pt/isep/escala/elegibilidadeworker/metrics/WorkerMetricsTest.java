package pt.isep.escala.elegibilidadeworker.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import pt.isep.escala.elegibilidadeworker.messaging.BrokerRetryAdvice;
import pt.isep.escala.elegibilidadeworker.messaging.MalformedEventException;
import pt.isep.escala.elegibilidadeworker.messaging.RetryPublisher;

/** The worker's metrics without a broker or a database (SCRUM-97). */
class WorkerMetricsTest {

	@Test
	@DisplayName("TC-SCRUM-97-08 an escala.* meter with a tag outside the allowed keys is dropped (no IDs, no free text, T-07)")
	void tagPolicy() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		registry.config().meterFilter(new MetricsConfiguration().escalaTagPolicy());

		registry.counter("escala.evaluations.completed", Tags.of("result", "ELIGIBLE", "source", "listener"))
				.increment();
		registry.counter("escala.evaluations.completed", Tags.of("memberId", "00000000-0000-4000-8000-000000000001"))
				.increment();
		registry.counter("escala.outbox.pending", Tags.of("eventId", "a4f1")).increment();
		registry.counter("jvm.other", Tags.of("anything", "x")).increment();

		assertThat(registry.getMeters()).extracting(meter -> meter.getId().getName() + meter.getId().getTags())
				.anyMatch(id -> id.contains("result=ELIGIBLE"))
				.noneMatch(id -> id.contains("memberId"))
				.noneMatch(id -> id.contains("eventId"));
		// the policy is only for escala.* meters
		assertThat(registry.find("jvm.other").counter()).isNotNull();
		assertThat(MetricsConfiguration.breaksTagPolicy(new Meter.Id("escala.x", Tags.of("queue", "q"), null, null,
				Meter.Type.COUNTER))).isFalse();
	}

	@Test
	@DisplayName("TC-SCRUM-97-08 a result outside the contract's values is OTHER, never the value itself")
	void resultValues() {
		assertThat(MetricTags.result("ELIGIBLE")).isEqualTo("ELIGIBLE");
		assertThat(MetricTags.result("INELIGIBLE")).isEqualTo("INELIGIBLE");
		assertThat(MetricTags.result("FAILED")).isEqualTo("FAILED");
		assertThat(MetricTags.result("membro 123 com consulta")).isEqualTo(MetricTags.OTHER);
		assertThat(MetricTags.result(null)).isEqualTo(MetricTags.OTHER);
	}

	@Test
	@DisplayName("TC-SCRUM-97-09 the worker counts evaluations by result and source, and times their processing")
	void evaluationMetrics() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		EvaluationMetrics metrics = new EvaluationMetrics(registry);

		metrics.completed("ELIGIBLE", EvaluationMetrics.LISTENER);
		metrics.completed("ELIGIBLE", EvaluationMetrics.LISTENER);
		metrics.completed("FAILED", EvaluationMetrics.RETRIES_EXHAUSTED);
		metrics.processed(Duration.ofMillis(40));

		assertThat(registry.get("escala.evaluations.completed").tags("result", "ELIGIBLE", "source", "listener")
				.counter().count()).isEqualTo(2.0);
		assertThat(registry.get("escala.evaluations.completed").tags("result", "FAILED", "source", "retries_exhausted")
				.counter().count()).isEqualTo(1.0);
		assertThat(registry.get("escala.evaluation.processing").timer().count()).isEqualTo(1);
	}

	@Test
	@DisplayName("TC-SCRUM-97-06 every message rejected to the DLQ is reported with its queue: malformed, or after the last retry; a retry is not")
	void deadLetterCallback() throws Throwable {
		List<String> deadLettered = new ArrayList<>();
		RetryPublisher retryPublisher = mock(RetryPublisher.class);
		BrokerRetryAdvice advice = new BrokerRetryAdvice(retryPublisher, List.of("retry.5s"), List.of(),
				deadLettered::add);

		// malformed: straight to the DLQ
		assertThatThrownBy(() -> advice.invoke(failing(message(0), new MalformedEventException("not JSON"))))
				.isNotNull();
		// first failure: a retry, not the DLQ
		advice.invoke(failing(message(0), new IllegalStateException("database down")));
		// after the last retry: the DLQ
		assertThatThrownBy(() -> advice.invoke(failing(message(1), new IllegalStateException("database down"))))
				.isNotNull();

		assertThat(deadLettered).containsExactly("worker.avaliacao-pedida", "worker.avaliacao-pedida");
	}

	private static Message message(int retryCount) {
		MessageProperties properties = new MessageProperties();
		properties.setConsumerQueue("worker.avaliacao-pedida");
		if (retryCount > 0) {
			properties.setHeader(RetryPublisher.RETRY_COUNT_HEADER, retryCount);
		}
		return new Message("{}".getBytes(), properties);
	}

	private static MethodInvocation failing(Message message, RuntimeException failure) throws Throwable {
		MethodInvocation invocation = mock(MethodInvocation.class);
		when(invocation.getArguments()).thenReturn(new Object[] { message });
		when(invocation.proceed()).thenThrow(failure);
		return invocation;
	}
}
