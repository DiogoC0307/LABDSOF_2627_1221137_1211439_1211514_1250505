package pt.isep.escala.elegibilidadeworker.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

/** The worker declares the queue escala-api consumes for RiscoAtualizado. */
class RiscoAtualizadoQueueTest {

	@Test
	@DisplayName("the queue is api.risco-atualizado on risco.atualizado")
	void queue() {
		var declarables = new RabbitTopology().apiRiscoAtualizado().getDeclarables();
		assertThat(declarables).anyMatch(item -> item instanceof Queue queue
				&& "api.risco-atualizado".equals(queue.getName()));
		assertThat(declarables).anyMatch(item -> item instanceof Binding binding
				&& "risco.atualizado".equals(binding.getRoutingKey()));
	}
}
