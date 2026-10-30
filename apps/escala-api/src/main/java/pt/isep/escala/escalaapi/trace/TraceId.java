package pt.isep.escala.escalaapi.trace;

import java.util.UUID;

public final class TraceId {

	private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

	private TraceId() {
	}

	public static String current() {
		String value = CURRENT.get();
		if (value == null || value.isBlank()) {
			value = UUID.randomUUID().toString();
			CURRENT.set(value);
		}
		return value;
	}

	static void set(String value) {
		CURRENT.set(value);
	}

	static void clear() {
		CURRENT.remove();
	}
}
