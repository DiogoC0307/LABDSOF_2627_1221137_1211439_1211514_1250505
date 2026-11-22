package pt.isep.escala.escalaapi.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.isep.escala.escalaapi.auth.MockTokens;
import tools.jackson.databind.ObjectMapper;

/**
 * Only the admin edits members and certifications, and the contact is seen only by the admin and the member
 * themself (SCRUM-77 AC5; access-control matrix and data minimisation on 09).
 */
@SpringBootTest(properties = "spring.rabbitmq.dynamic=true")
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class MemberAccessIT {

	static final String CONTACT = "contact-marker-77@example.test";

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	@DisplayName("TC-SCRUM-77-05 a member or a supervisor editing a member's certifications gets 403 and nothing changes")
	void onlyTheAdminEditsCertifications() throws Exception {
		UUID id = MemberRequests.register(mockMvc, objectMapper, MemberRequests.driverWithTas("Synthetic Access One", null));
		Map<String, Object> before = certification(id);

		for (String token : List.of(MockTokens.member(), MockTokens.supervisor(), MockTokens.bearer(id, "MEMBER"))) {
			mockMvc.perform(put("/api/v1/members/" + id + "/certifications")
							.header(HttpHeaders.AUTHORIZATION, token)
							.contentType(MediaType.APPLICATION_JSON)
							.content(MemberRequests.qualifications("2027-03-01", "2030-02-28")))
					.andExpect(status().isForbidden());
		}

		assertThat(certification(id)).isEqualTo(before);
	}

	@Test
	@DisplayName("TC-SCRUM-77-05 a member or a supervisor cannot register a member or change a member's details: 403, nothing changes")
	void onlyTheAdminRegistersAndEditsMembers() throws Exception {
		UUID id = MemberRequests.register(mockMvc, objectMapper, MemberRequests.driverWithTas("Synthetic Access Two", null));
		long members = jdbc.queryForObject("SELECT count(*) FROM member", Long.class);

		for (String token : List.of(MockTokens.member(), MockTokens.supervisor())) {
			mockMvc.perform(post("/api/v1/members")
							.header(HttpHeaders.AUTHORIZATION, token)
							.contentType(MediaType.APPLICATION_JSON)
							.content(MemberRequests.driverWithTas("Synthetic Intruder", null)))
					.andExpect(status().isForbidden());
			mockMvc.perform(put("/api/v1/members/" + id)
							.header(HttpHeaders.AUTHORIZATION, token)
							.contentType(MediaType.APPLICATION_JSON)
							.content(MemberRequests.driverWithTas("Synthetic Renamed", null)))
					.andExpect(status().isForbidden());
		}

		assertThat(jdbc.queryForObject("SELECT count(*) FROM member", Long.class)).isEqualTo(members);
		assertThat(jdbc.queryForObject("SELECT full_name FROM member WHERE id = ?", String.class, id))
				.isEqualTo("Synthetic Access Two");
	}

	@Test
	@DisplayName("the contact is shown to the admin and to the member themself, never to the supervisor or another member")
	void contactIsShownOnlyToTheAdminAndTheMember() throws Exception {
		UUID id = MemberRequests.register(mockMvc, objectMapper, MemberRequests.driverWithTas("Synthetic Access Three", CONTACT));

		mockMvc.perform(get("/api/v1/members/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.admin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contact").value(CONTACT));
		mockMvc.perform(get("/api/v1/members/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.bearer(id, "MEMBER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contact").value(CONTACT));

		String asSupervisor = mockMvc.perform(get("/api/v1/members/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String listAsSupervisor = mockMvc.perform(get("/api/v1/members").header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(asSupervisor).doesNotContain(CONTACT).doesNotContain("\"contact\"");
		assertThat(listAsSupervisor).contains("Synthetic Access Three").doesNotContain(CONTACT);

		mockMvc.perform(get("/api/v1/members/" + id).header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/members").header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("the contact is never written to the logs")
	void contactIsNotLogged(CapturedOutput output) throws Exception {
		UUID id = MemberRequests.register(mockMvc, objectMapper, MemberRequests.driverWithTas("Synthetic Access Four", CONTACT));
		mockMvc.perform(put("/api/v1/members/" + id)
						.header(HttpHeaders.AUTHORIZATION, MockTokens.admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content(MemberRequests.driverWithTas("Synthetic Access Four", CONTACT)))
				.andExpect(status().isOk());

		assertThat(output.getAll()).doesNotContain(CONTACT);
	}

	@Test
	@DisplayName("supervisor and admin read the certification types; a member gets 403")
	void certificationTypes() throws Exception {
		mockMvc.perform(get("/api/v1/certification-types").header(HttpHeaders.AUTHORIZATION, MockTokens.supervisor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.code == 'TAS')].name").value("Tripulante de Ambulância de Socorro"))
				.andExpect(jsonPath("$[?(@.code == 'TAT')].name").value("Tripulante de Ambulância de Transporte"));
		mockMvc.perform(get("/api/v1/certification-types").header(HttpHeaders.AUTHORIZATION, MockTokens.member()))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("an unknown member answers 404 to the admin")
	void unknownMember() throws Exception {
		mockMvc.perform(get("/api/v1/members/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, MockTokens.admin()))
				.andExpect(status().isNotFound());
	}

	private Map<String, Object> certification(UUID memberId) {
		return jdbc.queryForMap(
				"SELECT type_code, issued_on::text AS issued_on, expires_on::text AS expires_on FROM member_certification WHERE member_id = ?",
				memberId);
	}
}
