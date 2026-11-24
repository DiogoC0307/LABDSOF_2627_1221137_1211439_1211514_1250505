/**
 * The week of the schedule: an ISO 8601 week, Monday to Sunday (RN12 week, SCRUM-65), as the API's
 * GET /api/v1/piquetes?week=2026-W48 expects. Dates are "YYYY-MM-DD" days as the corps sees them in Lisbon;
 * the arithmetic runs in UTC so the device's time zone and clock changes never shift a day.
 */

const DAY_MS = 24 * 60 * 60 * 1000;

const LISBON_DATE = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Europe/Lisbon',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

const MONTH = new Intl.DateTimeFormat('en-GB', { timeZone: 'UTC', month: 'long' });
const WEEKDAY = new Intl.DateTimeFormat('en-GB', { timeZone: 'UTC', weekday: 'short' });
const LONG_DAY = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'UTC',
  weekday: 'long',
  day: 'numeric',
  month: 'long',
});

function toUtc(date: string): Date {
  const [year, month, day] = date.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}

function toIso(date: Date): string {
  return date.toISOString().slice(0, 10);
}

/** Today in Lisbon, whatever the device's time zone ("en-CA" formats as YYYY-MM-DD). */
export function todayInLisbon(now: Date = new Date()): string {
  return LISBON_DATE.format(now);
}

/** The Monday of the week that holds the date. */
export function mondayOf(date: string): string {
  const day = toUtc(date);
  const daysSinceMonday = (day.getUTCDay() + 6) % 7;
  return toIso(new Date(day.getTime() - daysSinceMonday * DAY_MS));
}

/** The date a number of days later (or earlier, when negative). */
export function addDays(date: string, days: number): string {
  return toIso(new Date(toUtc(date).getTime() + days * DAY_MS));
}

/** The seven days of the week that starts on that Monday. */
export function weekDays(monday: string): string[] {
  return Array.from({ length: 7 }, (_, index) => addDays(monday, index));
}

/** The number of the ISO week of that Monday, 1 to 53: the week belongs to the year of its Thursday. */
export function isoWeekNumber(monday: string): number {
  const thursday = toUtc(addDays(monday, 3));
  const dayOfYear = Math.round((thursday.getTime() - Date.UTC(thursday.getUTCFullYear(), 0, 1)) / DAY_MS);
  return Math.floor(dayOfYear / 7) + 1;
}

/** The ISO week of that Monday, e.g. "2026-W48", in the year of its Thursday. */
export function isoWeek(monday: string): string {
  const year = toUtc(addDays(monday, 3)).getUTCFullYear();
  return `${year}-W${String(isoWeekNumber(monday)).padStart(2, '0')}`;
}

/** The first day of the date's month. */
export function firstOfMonth(date: string): string {
  return `${date.slice(0, 7)}-01`;
}

/** The first day of the month a number of months later (or earlier, when negative). */
export function addMonths(first: string, months: number): string {
  const day = toUtc(first);
  return toIso(new Date(Date.UTC(day.getUTCFullYear(), day.getUTCMonth() + months, 1)));
}

/** The Mondays of the weeks that show a day of that month, as rows of a calendar (4 to 6 of them). */
export function monthMondays(first: string): string[] {
  const last = addDays(addMonths(first, 1), -1);
  const mondays: string[] = [];
  for (let monday = mondayOf(first); monday <= last; monday = addDays(monday, 7)) {
    mondays.push(monday);
  }
  return mondays;
}

/** A calendar heading, e.g. "November 2026". */
export function monthTitle(first: string): string {
  const day = toUtc(first);
  return `${MONTH.format(day)} ${day.getUTCFullYear()}`;
}

/**
 * The week in words for screen readers and the calendar rows, e.g. "November 2026 · Days 23 to 29". A week across
 * two months or years names both:
 * "November – December 2026 · Days 30 to 6", "December 2026 – January 2027 · Days 28 to 3".
 */
export function weekTitle(monday: string): string {
  const days = weekDays(monday).map(toUtc);
  const first = days[0];
  const last = days[6];
  const firstMonth = MONTH.format(first);
  const lastMonth = MONTH.format(last);
  let months: string;
  if (first.getUTCFullYear() !== last.getUTCFullYear()) {
    months = `${firstMonth} ${first.getUTCFullYear()} – ${lastMonth} ${last.getUTCFullYear()}`;
  } else if (firstMonth === lastMonth) {
    months = `${firstMonth} ${first.getUTCFullYear()}`;
  } else {
    months = `${firstMonth} – ${lastMonth} ${first.getUTCFullYear()}`;
  }
  return `${months} · Days ${first.getUTCDate()} to ${last.getUTCDate()}`;
}

/**
 * The heading of the week, e.g. "November 2026 - Week 48". A week across two months or years takes the month and
 * year of its Thursday, as its ISO number does: 30 November to 6 December 2026 is "December 2026 - Week 49".
 */
export function weekHeading(monday: string): string {
  return `${monthTitle(firstOfMonth(addDays(monday, 3)))} - Week ${isoWeekNumber(monday)}`;
}

/** A column heading, e.g. "Mon 23". */
export function shortDay(date: string): string {
  const day = toUtc(date);
  return `${WEEKDAY.format(day)} ${day.getUTCDate()}`;
}

/** A day for screen readers, e.g. "Monday 23 November". */
export function longDay(date: string): string {
  return LONG_DAY.format(toUtc(date)).replace(',', '');
}
