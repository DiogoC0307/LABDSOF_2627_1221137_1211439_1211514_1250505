package pt.isep.escala.escalaapi.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What a log line may say about a failure: exception messages often carry the value that failed (an invalid
 * date, a rejected row), so only the class names are written (SCRUM-94).
 */
public final class SafeLogValues {

	private SafeLogValues() {
	}

	/** The class names of the exception and its causes, e.g. {@code A <- B}, without their messages. */
	public static String exceptionClasses(Throwable t) {
		List<String> names = new ArrayList<>();
		for (Throwable current = t; current != null && names.size() < 10; current = current.getCause()) {
			names.add(current.getClass().getName());
			if (current.getCause() == current) {
				break;
			}
		}
		return String.join(" <- ", names);
	}

	/** The value if it is a UUID, otherwise {@code invalid}: an ID field may hold anything a sender put there. */
	public static String uuidOrInvalid(String value) {
		if (value == null || value.isBlank()) {
			return "missing";
		}
		try {
			return UUID.fromString(value).toString();
		} catch (IllegalArgumentException e) {
			return "invalid";
		}
	}
}
