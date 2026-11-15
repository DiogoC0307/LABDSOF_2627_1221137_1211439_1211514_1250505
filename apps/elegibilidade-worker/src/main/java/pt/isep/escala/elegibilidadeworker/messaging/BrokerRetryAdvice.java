package pt.isep.escala.elegibilidadeworker.messaging;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.listener.ListenerExecutionFailedException;
import pt.isep.escala.elegibilidadeworker.logging.SafeListenerErrorHandler;
import pt.isep.escala.elegibilidadeworker.logging.SafeLogValues;

/**
 * Wraps every listener call (ADR 0005, ADR 0009; SCRUM-95). When the listener fails:
 * <ul>
 * <li>a malformed message is rejected at once, so the broker dead-letters it to the queue's DLQ;</li>
 * <li>otherwise, while the message's retry count is below the number of stages, it is published to the next retry
 * queue (confirmed by the broker) and the original is acknowledged; the retry queue's TTL is the delay;</li>
 * <li>once the count is used up, the queue's {@link RetriesExhaustedHandler} runs and the message is rejected to
 * the DLQ.</li>
 * </ul>
 * The listener thread never waits for a retry: the message waits in the broker.
 */
public class BrokerRetryAdvice implements MethodInterceptor {

	private static final Logger log = LoggerFactory.getLogger(BrokerRetryAdvice.class);

	private final RetryPublisher retryPublisher;
	private final List<String> stages;
	private final Map<String, RetriesExhaustedHandler> exhaustedHandlers;

	public BrokerRetryAdvice(RetryPublisher retryPublisher, List<String> stages, List<RetriesExhaustedHandler> exhaustedHandlers) {
		this.retryPublisher = retryPublisher;
		this.stages = List.copyOf(stages);
		this.exhaustedHandlers = exhaustedHandlers.stream()
				.collect(Collectors.toMap(RetriesExhaustedHandler::queue, Function.identity()));
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		try {
			return invocation.proceed();
		} catch (Throwable failure) {
			Message message = messageOf(invocation);
			if (message == null || isMalformed(failure)) {
				throw rejected(failure, message);
			}
			String queue = message.getMessageProperties().getConsumerQueue();
			int count = retryCount(message);
			if (count < this.stages.size()) {
				this.retryPublisher.publish(message, queue + "." + this.stages.get(count), count + 1);
				log.warn("listener failed, retry {} of {} {} exception={}", count + 1, this.stages.size(),
						SafeListenerErrorHandler.describe(message), SafeLogValues.exceptionClasses(failure));
				return null;
			}
			retriesExhausted(queue, message);
			throw rejected(failure, message);
		}
	}

	private void retriesExhausted(String queue, Message message) {
		RetriesExhaustedHandler handler = this.exhaustedHandlers.get(queue);
		if (handler == null) {
			return;
		}
		try {
			handler.handle(message);
		} catch (RuntimeException e) {
			log.warn("retries exhausted, handler failed {} exception={}", SafeListenerErrorHandler.describe(message),
					SafeLogValues.exceptionClasses(e));
		}
	}

	private static Message messageOf(MethodInvocation invocation) {
		for (Object argument : invocation.getArguments()) {
			if (argument instanceof Message message) {
				return message;
			}
		}
		return null;
	}

	static boolean isMalformed(Throwable failure) {
		for (Throwable current = failure; current != null; current = current.getCause()) {
			if (current instanceof MalformedEventException) {
				return true;
			}
			if (current.getCause() == current) {
				break;
			}
		}
		return false;
	}

	static int retryCount(Message message) {
		Object count = message.getMessageProperties().getHeader(RetryPublisher.RETRY_COUNT_HEADER);
		return count instanceof Number number ? number.intValue() : 0;
	}

	/** Rejected without requeue, so the broker dead-letters it; the failed message is kept for the log line. */
	private static ListenerExecutionFailedException rejected(Throwable failure, Message message) {
		return new ListenerExecutionFailedException("rejected to the dead-letter queue",
				new AmqpRejectAndDontRequeueException("rejected to the dead-letter queue", failure), message);
	}
}
