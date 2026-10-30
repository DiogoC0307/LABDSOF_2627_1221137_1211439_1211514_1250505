package pt.isep.escala.elegibilidadeworker.outbox;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RabbitTopology {

	@Bean
	TopicExchange escalaEvents() {
		return new TopicExchange(OutboxPublisher.EXCHANGE, true, false);
	}

	@Bean
	Queue workerAvaliacaoPedida() {
		return QueueBuilder.durable("worker.avaliacao-pedida").build();
	}

	@Bean
	Queue apiAvaliacaoConcluida() {
		return QueueBuilder.durable("api.avaliacao-concluida").build();
	}

	@Bean
	Binding bindAvaliacaoPedida() {
		return BindingBuilder.bind(workerAvaliacaoPedida()).to(escalaEvents()).with("avaliacao.pedida");
	}

	@Bean
	Binding bindAvaliacaoConcluida() {
		return BindingBuilder.bind(apiAvaliacaoConcluida()).to(escalaEvents()).with("avaliacao.concluida");
	}
}
