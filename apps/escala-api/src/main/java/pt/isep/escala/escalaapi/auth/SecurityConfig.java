package pt.isep.escala.escalaapi.auth;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import pt.isep.escala.escalaapi.member.MemberRepository;

/**
 * Who may call what (SCRUM-63; access-control matrix on 09). Sign-in is mocked (ADR 0006), but the role is checked
 * here, on the server, for every request. Without a valid token a business endpoint answers 401; with a valid token
 * and the wrong role, 403. The health endpoints and the two sign-in endpoints stay open.
 */
@Configuration
class SecurityConfig {

	static final String PIQUETES = "/api/v1/piquetes";
	static final String MEMBERS = "/api/v1/members";
	static final String UNAVAILABILITIES = "/api/v1/unavailabilities";

	private static final String MEMBER = "MEMBER";
	private static final String SUPERVISOR = "SUPERVISOR";
	private static final String ADMIN = "ADMIN";

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http, MemberRepository members) {
		return http
				// CSRF protection stays on, but not for /api/**. CSRF guards credentials the browser sends by itself
				// (cookies, a session). The API is stateless: the PWA sends the token in the Authorization header and
				// there is no cookie or session, so a forged cross-site request carries no credentials and gets 401.
				// A CSRF token on /api/** would only break the PWA's POST requests. Sonar S4502 flags any ignored
				// path, so it is suppressed on the next line for this reason; reviewed in PR #68 (SCRUM-63).
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**")) // NOSONAR stateless API, token in the Authorization header, no cookie or session
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.requestCache(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(new MockTokenAuthenticationFilter(members), AnonymousAuthenticationFilter.class)
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, e) -> response.sendError(HttpStatus.UNAUTHORIZED.value()))
						.accessDeniedHandler((request, response, e) -> response.sendError(HttpStatus.FORBIDDEN.value())))
				.authorizeHttpRequests(requests -> requests
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers("/error").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/auth/mock-users").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/mock-sign-in").permitAll()
						.requestMatchers(HttpMethod.GET, PIQUETES, PIQUETES + "/*").hasAnyRole(MEMBER, SUPERVISOR, ADMIN)
						.requestMatchers(HttpMethod.POST, PIQUETES).hasRole(SUPERVISOR)
						// SCRUM-77: only the admin edits members (09); supervisor and admin read them; a member reads
						// only themself (checked in MemberController); the contact only for the admin and the member.
						.requestMatchers(HttpMethod.GET, MEMBERS, "/api/v1/certification-types").hasAnyRole(SUPERVISOR, ADMIN)
						.requestMatchers(HttpMethod.GET, MEMBERS + "/*").hasAnyRole(MEMBER, SUPERVISOR, ADMIN)
						.requestMatchers(HttpMethod.POST, MEMBERS).hasRole(ADMIN)
						.requestMatchers(HttpMethod.PUT, MEMBERS + "/*", MEMBERS + "/*/certifications").hasRole(ADMIN)
						// SCRUM-64: every role declares its own unavailability (09); only one's own periods are
						// found (checked in UnavailabilityService); a change is DELETE and POST, there is no PUT.
						.requestMatchers(HttpMethod.GET, UNAVAILABILITIES, UNAVAILABILITIES + "/*").hasAnyRole(MEMBER, SUPERVISOR, ADMIN)
						.requestMatchers(HttpMethod.POST, UNAVAILABILITIES).hasAnyRole(MEMBER, SUPERVISOR, ADMIN)
						.requestMatchers(HttpMethod.DELETE, UNAVAILABILITIES + "/*").hasAnyRole(MEMBER, SUPERVISOR, ADMIN)
						.anyRequest().denyAll())
				.build();
	}
}
