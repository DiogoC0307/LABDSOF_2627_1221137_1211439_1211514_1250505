package pt.isep.escala.escalaapi.outbox;

import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.isep.escala.escalaapi.messaging.EventQueues;

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

	// SCRUM-75: free-text extraction. Both are already in infra/rabbitmq/definitions.json (TC-SCRUM-95-05).
	@Bean
	Declarables workerExtracaoPedida() {
		return EventQueues.queue("worker.extracao-pedida", "extracao.pedida");
	}

	@Bean
	Declarables apiExtracaoConcluida() {
		return EventQueues.queue("api.extracao-concluida", "extracao.concluida");
	}
}
