import { addDays } from './iso-week';

/**
 * Where a shift sits in the week's hour grid. The API keeps the start and end as instants in UTC (ADR 0012); the
 * grid shows them on the Lisbon clock, whatever the device's time zone.
 */

const MINUTES_IN_DAY = 24 * 60;

const LISBON_CLOCK = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Europe/Lisbon',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

export interface LisbonTime {
  /** The Lisbon date, "YYYY-MM-DD". */
  date: string;
  /** Minutes since midnight in Lisbon, 0 to 1439. */
  minutes: number;
  /** "HH:mm" on the Lisbon clock. */
  time: string;
}

/** An instant such as "2026-11-24T12:00:00Z" as Lisbon sees it. */
export function lisbonTime(instant: string): LisbonTime {
  const parts = Object.fromEntries(
    LISBON_CLOCK.formatToParts(new Date(instant)).map((part) => [part.type, part.value]),
  );
  return {
    date: `${parts['year']}-${parts['month']}-${parts['day']}`,
    minutes: Number(parts['hour']) * 60 + Number(parts['minute']),
    time: `${parts['hour']}:${parts['minute']}`,
  };
}

/**
 * The instant, in UTC, of a Lisbon wall-clock time: "2026-11-24" "08:00" is "2026-11-24T08:00:00Z" in winter, and
 * "2027-06-30" "08:00" is "2027-06-30T07:00:00Z" in summer. It corrects the guess twice, so a time next to a clock
 * change still lands on the right side of it.
 */
export function lisbonInstant(date: string, time: string): string {
  const [year, month, day] = date.split('-').map(Number);
  const [hour, minute] = time.split(':').map(Number);
  const wall = Date.UTC(year, month - 1, day, hour, minute);
  let instant = wall;
  for (let attempt = 0; attempt < 2; attempt++) {
    const seen = lisbonTime(new Date(instant).toISOString());
    const [seenYear, seenMonth, seenDay] = seen.date.split('-').map(Number);
    instant += wall - (Date.UTC(seenYear, seenMonth - 1, seenDay) + seen.minutes * 60_000);
  }
  return new Date(instant).toISOString().replace('.000Z', 'Z');
}

/** The part of a shift that falls on one day of the grid, in minutes since that day's midnight. */
export interface ShiftSegment {
  date: string;
  from: number;
  to: number;
  /** "From 20:00 to 08:00": the whole shift, on every part of it. */
  label: string;
  /** This part starts at midnight because the shift began the day before. */
  continued: boolean;
}

/**
 * Splits a shift at midnight, so a night shift from 20:00 to 08:00 shows on both days. A shift that ends at or
 * before it starts has no segments; one longer than a week is cut at a week.
 */
export function shiftSegments(startsAt: string, endsAt: string): ShiftSegment[] {
  if (Date.parse(endsAt) <= Date.parse(startsAt)) {
    return [];
  }
  const start = lisbonTime(startsAt);
  const end = lisbonTime(endsAt);
  const label = `From ${start.time} to ${end.time}`;
  const segments: ShiftSegment[] = [];
  for (let date = start.date, day = 0; date <= end.date && day < 8; date = addDays(date, 1), day++) {
    const from = date === start.date ? start.minutes : 0;
    const to = date === end.date ? end.minutes : MINUTES_IN_DAY;
    if (to > from) {
      segments.push({ date, from, to, label, continued: date !== start.date });
    }
  }
  return segments;
}

/** A segment placed in its day: the share of the day's height above it and its own, and its lane beside others. */
export interface PlacedSegment<T> extends ShiftSegment {
  item: T;
  topPercent: number;
  heightPercent: number;
  leftPercent: number;
  widthPercent: number;
}

/**
 * Places a day's segments in its column. Segments that overlap go side by side in lanes; every segment of the day
 * gets the same width, enough for the most lanes needed at once.
 */
export function placeSegments<T>(segments: (ShiftSegment & { item: T })[]): PlacedSegment<T>[] {
  const sorted = [...segments].sort((a, b) => a.from - b.from || b.to - a.to);
  const laneEnds: number[] = [];
  const lanes = sorted.map((segment) => {
    let lane = laneEnds.findIndex((end) => end <= segment.from);
    if (lane < 0) {
      lane = laneEnds.length;
      laneEnds.push(0);
    }
    laneEnds[lane] = segment.to;
    return lane;
  });
  const width = 100 / Math.max(laneEnds.length, 1);
  return sorted.map((segment, index) => ({
    ...segment,
    topPercent: (segment.from / MINUTES_IN_DAY) * 100,
    heightPercent: ((segment.to - segment.from) / MINUTES_IN_DAY) * 100,
    leftPercent: lanes[index] * width,
    widthPercent: width,
  }));
}
