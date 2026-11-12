package pt.isep.escala.escalaapi.messaging;

import org.springframework.amqp.listener.ConditionalRejectingErrorHandler;
import pt.isep.escala.escalaapi.logging.SafeListenerErrorHandler;

/**
 * The SCRUM-94 error handler (no body in the log line) that also treats a {@link MalformedEventException} as fatal,
 * so a malformed message is rejected to the DLQ even if it reaches the handler without {@link BrokerRetryAdvice}, and
 * never drops a message.
 */
public class EventListenerErrorHandler extends SafeListenerErrorHandler {

	public EventListenerErrorHandler() {
		super(new ConditionalRejectingErrorHandler.DefaultExceptionStrategy() {

			@Override
			protected boolean isUserCauseFatal(Throwable cause) {
				return BrokerRetryAdvice.isMalformed(cause);
			}
		});
		// By default a fatal message that has been dead-lettered before (it has x-death, e.g. replayed from the DLQ)
		// is acknowledged and dropped; it must go back to the DLQ instead.
		setDiscardFatalsWithXDeath(false);
	}
}
