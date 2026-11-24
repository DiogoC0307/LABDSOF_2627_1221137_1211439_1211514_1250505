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

	/** SCRUM-75: the worker consumes ExtracaoPedida and publishes ExtracaoConcluida (definitions.json has both). */
	@Bean
	Declarables workerExtracaoPedida() {
		return EventQueues.queue("worker.extracao-pedida", "extracao.pedida");
	}

	@Bean
	Declarables apiExtracaoConcluida() {
		return EventQueues.queue("api.extracao-concluida", "extracao.concluida");
	}

	/** SCRUM-76: the worker publishes RiscoAtualizado; escala-api consumes it (definitions.json). */
	@Bean
	Declarables apiRiscoAtualizado() {
		return EventQueues.queue("api.risco-atualizado", "risco.atualizado");
	}
}
