package pt.isep.escala.escalaapi.validation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.ServletWebRequest;
import tools.jackson.databind.ObjectMapper;

/**
 * Replaces Spring Boot's BasicErrorController: every error that reaches /error (401 and 403 from Spring Security,
 * 404, 405, 415, 500) is answered with Spring Boot's JSON error body (timestamp, status, error, path), whatever the
 * Accept header. With text/html, BasicErrorController answered the Whitelabel page, and without it Tomcat's own
 * HTML page (SCRUM-88). The message and the exception are never included (Boot's defaults).
 */
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
class JsonErrorController implements ErrorController {

	private final ErrorAttributes errorAttributes;
	private final ObjectMapper objectMapper;

	JsonErrorController(ErrorAttributes errorAttributes, ObjectMapper objectMapper) {
		this.errorAttributes = errorAttributes;
		this.objectMapper = objectMapper;
	}

	// An error is forwarded to /error with the method of the original request, so every method is accepted.
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.POST, RequestMethod.PUT,
			RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS})
	void error(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Map<String, Object> body = errorAttributes.getErrorAttributes(new ServletWebRequest(request),
				ErrorAttributeOptions.defaults());
		if (body.get("status") instanceof Integer status) {
			response.setStatus(status);
		}
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
