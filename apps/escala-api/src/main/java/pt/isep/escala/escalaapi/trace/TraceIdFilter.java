package pt.isep.escala.escalaapi.trace;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TraceIdFilter extends OncePerRequestFilter {

	static final String HEADER = "X-Trace-Id";

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String incoming = request.getHeader(HEADER);
		String traceId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;
		TraceId.set(traceId);
		MDC.put("trace_id", traceId);
		response.setHeader(HEADER, traceId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove("trace_id");
			TraceId.clear();
		}
	}
}
