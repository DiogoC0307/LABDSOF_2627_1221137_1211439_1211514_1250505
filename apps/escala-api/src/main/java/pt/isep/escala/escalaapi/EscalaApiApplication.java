package pt.isep.escala.escalaapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EscalaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EscalaApiApplication.class, args);
	}

}
