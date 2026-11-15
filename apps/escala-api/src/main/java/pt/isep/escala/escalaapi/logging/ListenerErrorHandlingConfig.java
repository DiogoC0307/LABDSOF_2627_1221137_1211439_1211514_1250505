package pt.isep.escala.escalaapi.logging;

import java.util.Arrays;
import java.util.List;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.isep.escala.escalaapi.messaging.BrokerRetryAdvice;
import pt.isep.escala.escalaapi.messaging.EventListenerErrorHandler;
import pt.isep.escala.escalaapi.messaging.RetriesExhaustedHandler;
import pt.isep.escala.escalaapi.messaging.RetryPublisher;

@Configuration
class ListenerErrorHandlingConfig {

	// Same name as Spring Boot's factory, so it replaces it; Boot's configurer still applies every spring.rabbitmq.listener setting.
	@Bean
	SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
			SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
			RetryPublisher retryPublisher, List<RetriesExhaustedHandler> exhaustedHandlers,
			@Value("${escala.messaging.retry-stages}") String retryStages) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setErrorHandler(new EventListenerErrorHandler());
		factory.setAdviceChain(new BrokerRetryAdvice(retryPublisher,
				Arrays.stream(retryStages.split(",")).map(String::trim).toList(), exhaustedHandlers));
		// A refused login (for example while the broker is still creating its users) must not stop the listener for good.
		factory.setContainerCustomizer(container -> container.setPossibleAuthenticationFailureFatal(false));
		return factory;
	}
}
