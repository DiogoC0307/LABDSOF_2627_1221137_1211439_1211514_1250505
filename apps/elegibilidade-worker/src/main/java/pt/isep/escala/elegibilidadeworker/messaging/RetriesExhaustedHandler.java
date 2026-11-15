package pt.isep.escala.elegibilidadeworker.messaging;

import org.springframework.amqp.core.Message;

/**
 * What a service does when a message from {@code queue()} has failed after every retry, just before it goes to the
 * DLQ: for example, record that an evaluation failed. A failure here is logged and the message still goes to the DLQ.
 */
public interface RetriesExhaustedHandler {

	String queue();

	void handle(Message message);
}
