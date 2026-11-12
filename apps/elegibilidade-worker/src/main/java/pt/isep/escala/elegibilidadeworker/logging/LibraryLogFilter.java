package pt.isep.escala.elegibilidadeworker.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.Set;
import org.slf4j.Marker;

/**
 * Keeps request values, SQL values and message bodies out of the logs (SCRUM-94, T-07), whatever level a
 * ConfigMap or an environment variable sets. Registered in logback-spring.xml.
 * <ul>
 * <li>DEBUG and TRACE are written only for the team's own packages ({@code pt.isep.escala}). Libraries print
 * request bodies (Spring MVC), message bodies (Spring AMQP) and SQL bind values (Hibernate) at those levels.</li>
 * <li>The Hibernate loggers that print the rejected row of a failed SQL statement are never written; the
 * failure is logged without values where the exception is handled.</li>
 * </ul>
 */
public class LibraryLogFilter extends TurboFilter {

	static final String OWN_PACKAGES = "pt.isep.escala";

	static final Set<String> NEVER_WRITTEN = Set.of(
			"org.hibernate.orm.jdbc.error",
			"org.hibernate.engine.jdbc.spi.SqlExceptionHelper");

	@Override
	public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
		String name = logger.getName();
		if (name.startsWith(OWN_PACKAGES)) {
			return FilterReply.NEUTRAL;
		}
		if (NEVER_WRITTEN.contains(name)) {
			return FilterReply.DENY;
		}
		if (level != null && !level.isGreaterOrEqual(Level.INFO)) {
			return FilterReply.DENY;
		}
		return FilterReply.NEUTRAL;
	}
}
