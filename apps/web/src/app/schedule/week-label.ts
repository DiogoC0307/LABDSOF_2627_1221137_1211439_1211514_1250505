/**
 * Week selector labels: how far the week is from this week ("Next week") and its days ("23 – 29 November 2026").
 *
 * Weeks run Monday to Sunday and are given by their Monday, "YYYY-MM-DD", as in iso-week.ts. The arithmetic runs in
 * UTC so the device's time zone never shifts a day.
 */

const WEEK_MS = 7 * 24 * 60 * 60 * 1000;

const MONTH = new Intl.DateTimeFormat('en-GB', { timeZone: 'UTC', month: 'long' });

interface Day {
  day: number;
  month: string;
  year: number;
}

// "YYYY-MM-DD" as a date at midnight UTC
function toUtc(date: string): Date {
  const [year, month, day] = date.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}

// The day, month name and year of a date
function parts(date: Date): Day {
  return { day: date.getUTCDate(), month: MONTH.format(date), year: date.getUTCFullYear() };
}

// The first three letters of the month's name ("Nov", "Dec", "Sep")
function short(month: string): string {
  return month.slice(0, 3);
}

/**
 * The week compared with the week that holds today
 *
 * "This week", "Next week", "Last week", "In 3 weeks", "2 weeks ago". Both arguments are Mondays.
 */
export function relativeWeekLabel(monday: string, currentMonday: string): string {
  const weeks = Math.round((toUtc(monday).getTime() - toUtc(currentMonday).getTime()) / WEEK_MS);

  if (weeks === 0) {
    return 'This week';
  }

  if (weeks === 1) {
    return 'Next week';
  }

  if (weeks === -1) {
    return 'Last week';
  }

  return weeks > 1 ? `In ${weeks} weeks` : `${-weeks} weeks ago`;
}

/**
 * The days of the week from Monday to Sunday, with an en dash
 *
 * "23 – 29 November 2026", "30 Nov – 6 Dec 2026", "28 Dec 2026 – 3 Jan 2027".
 */
export function weekRange(monday: string): string {
  const start = toUtc(monday);
  const s = parts(start);
  const e = parts(new Date(start.getTime() + WEEK_MS - 24 * 60 * 60 * 1000));

  // The week is across two years
  if (s.year !== e.year) {
    return `${s.day} ${short(s.month)} ${s.year} – ${e.day} ${short(e.month)} ${e.year}`;
  }

  // The week is across two months
  if (s.month !== e.month) {
    return `${s.day} ${short(s.month)} – ${e.day} ${short(e.month)} ${e.year}`;
  }

  return `${s.day} – ${e.day} ${e.month} ${e.year}`;
}
