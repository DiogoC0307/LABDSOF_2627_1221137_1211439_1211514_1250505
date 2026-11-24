package pt.isep.escala.escalaapi.unavailability;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * A period as the member sees it, in Europe/Lisbon (06, ExtracaoConcluida v2): the days from {@code startDate} to
 * {@code endDate} inclusive, only {@code weekdays} when given (null: every day), and on each day from
 * {@code startTime} to {@code endTime} when given (null: the whole day; an end before the start crosses midnight).
 */
record PeriodPattern(LocalDate startDate, LocalDate endDate, Set<DayOfWeek> weekdays, LocalTime startTime,
		LocalTime endTime) {
}
