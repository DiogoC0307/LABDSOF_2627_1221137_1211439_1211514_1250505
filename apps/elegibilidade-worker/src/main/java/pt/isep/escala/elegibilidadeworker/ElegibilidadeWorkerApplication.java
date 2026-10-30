package pt.isep.escala.elegibilidadeworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ElegibilidadeWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ElegibilidadeWorkerApplication.class, args);
	}

}
