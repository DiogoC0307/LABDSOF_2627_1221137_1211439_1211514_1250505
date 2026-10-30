package pt.isep.escala.escalaapi.piquete;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class CreatePiqueteIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	WebApplicationContext webApplicationContext;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void postWithoutDutyDateIsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/piquetes").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void postCreatesPiqueteWithOneSlotAndLocation() throws Exception {
		mockMvc.perform(post("/api/v1/piquetes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dutyDate\":\"2026-11-02\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/v1/piquetes/")))
				.andExpect(jsonPath("$.dutyDate").value("2026-11-02"))
				.andExpect(jsonPath("$.slots.length()").value(1));

		mockMvc.perform(get("/api/v1/piquetes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].dutyDate").value("2026-11-02"));
	}
}
