package pt.isep.escala.escalaapi.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import pt.isep.escala.escalaapi.member.MemberRepository;

/**
 * Signs the request in when it carries a mocked token of an active member whose role in the database is the role in
 * the token. Otherwise the request stays anonymous, and the rules in {@link SecurityConfig} answer 401 or 403. The
 * token is never logged.
 */
class MockTokenAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final MemberRepository members;

	MockTokenAuthenticationFilter(MemberRepository members) {
		this.members = members;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		signedInMember(request.getHeader(HttpHeaders.AUTHORIZATION)).ifPresent(member -> {
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
					member, null, List.of(new SimpleGrantedAuthority("ROLE_" + member.role().name()))));
			SecurityContextHolder.setContext(context);
		});
		chain.doFilter(request, response);
	}

	private Optional<SignedInMember> signedInMember(String authorization) {
		if (authorization == null || !authorization.startsWith(BEARER)) {
			return Optional.empty();
		}
		return MockToken.decode(authorization.substring(BEARER.length()))
				.flatMap(token -> members.findByIdAndActiveTrue(token.memberId())
						.filter(member -> member.getAccessRole() == token.role())
						.map(member -> new SignedInMember(member.getId(), member.getAccessRole())));
	}
}
