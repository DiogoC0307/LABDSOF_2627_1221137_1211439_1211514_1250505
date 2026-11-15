package pt.isep.escala.elegibilidadeworker.outbox;

import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.isep.escala.elegibilidadeworker.messaging.EventQueues;

@Configuration
class RabbitTopology {

	@Bean
	Declarables exchanges() {
		return EventQueues.exchanges();
	}

	@Bean
	Declarables workerAvaliacaoPedida() {
		return EventQueues.queue("worker.avaliacao-pedida", "avaliacao.pedida");
	}

	@Bean
	Declarables apiAvaliacaoConcluida() {
		return EventQueues.queue("api.avaliacao-concluida", "avaliacao.concluida");
	}
}
