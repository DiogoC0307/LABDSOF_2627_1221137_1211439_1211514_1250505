/**
 * Dates and times as the corps sees them, in Europe/Lisbon, whatever the device's time zone (SCRUM-64, ADR 0012:
 * the API stores instants in UTC).
 */
const ZONE = 'Europe/Lisbon';

const PARTS = new Intl.DateTimeFormat('en-GB', {
  timeZone: ZONE,
  hourCycle: 'h23',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
});

function lisbonParts(instant: number): Record<string, number> {
  const parts: Record<string, number> = {};
  for (const part of PARTS.formatToParts(new Date(instant))) {
    if (part.type !== 'literal') {
      parts[part.type] = Number(part.value);
    }
  }
  return parts;
}

/** How far Lisbon is ahead of UTC at that instant, in milliseconds (0 in winter, 1 hour in summer). */
function lisbonOffset(instant: number): number {
  const p = lisbonParts(instant);
  return Date.UTC(p['year'], p['month'] - 1, p['day'], p['hour'], p['minute'], p['second']) - instant;
}

/** A date ("2026-12-05") and a time ("08:00", or "" for midnight) in Lisbon, as an ISO instant in UTC. */
export function lisbonToUtc(date: string, time: string): string {
  const [year, month, day] = date.split('-').map(Number);
  const [hour, minute] = (time === '' ? '00:00' : time).split(':').map(Number);
  const asIfUtc = Date.UTC(year, month - 1, day, hour, minute);
  const guess = asIfUtc - lisbonOffset(asIfUtc);
  return new Date(asIfUtc - lisbonOffset(guess)).toISOString();
}

/** The day after a date, as "YYYY-MM-DD" (for an exclusive end). */
export function nextDay(date: string): string {
  const [year, month, day] = date.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day + 1)).toISOString().slice(0, 10);
}

/** An instant shown in Lisbon time, as "HH:mm". */
export function formatLisbonClock(iso: string): string {
  const p = lisbonParts(Date.parse(iso));
  const two = (value: number) => String(value).padStart(2, '0');
  return `${two(p['hour'])}:${two(p['minute'])}`;
}

/** An instant shown in Lisbon time, as "YYYY-MM-DD HH:mm". */
export function formatLisbon(iso: string): string {
  const p = lisbonParts(Date.parse(iso));
  const two = (value: number) => String(value).padStart(2, '0');
  return `${p['year']}-${two(p['month'])}-${two(p['day'])} ${two(p['hour'])}:${two(p['minute'])}`;
}
