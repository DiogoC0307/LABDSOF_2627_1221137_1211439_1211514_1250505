package pt.isep.escala.elegibilidadeworker.risk;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RiskProperties.class)
class IpmaClientConfig {

	@Bean
	IpmaClient ipmaClient() {
		return new IpmaClient(HttpClient.newHttpClient());
	}
}
