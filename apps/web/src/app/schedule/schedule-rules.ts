import { addDays } from './iso-week';
import { Piquete, ROLE_LABELS, SLOT_ROLES, SlotRole } from './piquete';
import {
  Crew,
  DraftShift,
  ScheduleMember,
  WeekDraft,
  emptyAssigned,
  emptyCrew,
} from './schedule-model';
import { lisbonTime } from './shift-time';

// Schedule page's rules

export const MINUTES_IN_DAY = 24 * 60;
export const LONGEST_NEW_SHIFT = 12 * 60; // Maximum lenght of a shift
export const MAXIMUM_PER_ROLE = 50; // Maximum number of people required for a role in a shift

// The minimum free time for a new shift button to appear
export const MIN_FREE_TIME_FOR_NEW_SHIFT_BUTTON = 2 * 60;

export interface Range {
  from: number;
  to: number;
}

// Returns a written time in format "HH:MM" for the given minutes
export function timeLabel(minutes: number): string {
  if (minutes === MINUTES_IN_DAY) {
    return '24:00';
  }

  const clock = ((minutes % MINUTES_IN_DAY) + MINUTES_IN_DAY) % MINUTES_IN_DAY;

  return `${String(Math.floor(clock / 60)).padStart(2, '0')}:${String(clock % 60).padStart(2, '0')}`;
}

/**
 * Parses a written time into minutes from midnight
 *
 * H, HH, H:MM, HH:MM, H.MM, HH.MM, HhMM, HHhMM and HMM all work, and a trailing "(next day)" is ignored.
 */
export function parseTime(text: string): number | null {
  const written = text.trim().replace(/\s*\(next day\)$/i, '');
  // With a separator: H:MM, HH.MM, HhMM; digits only: H, HH, HMM, HHMM, the last two digits being the minutes
  const separated = /^(\d{1,2})[:.h](\d{2})h?$/i.exec(written);
  const digitsOnly = /^(\d{1,4})h?$/i.exec(written);

  let hours: number;
  let minutes: number;
  if (separated) {
    hours = Number(separated[1]);
    minutes = Number(separated[2]);
  } else if (digitsOnly) {
    const digits = digitsOnly[1];
    hours = Number(digits.length > 2 ? digits.slice(0, -2) : digits);
    minutes = digits.length > 2 ? Number(digits.slice(-2)) : 0;
  } else {
    return null;
  }
  if (minutes > 59 || hours > 24 || (hours === 24 && minutes > 0)) {
    return null;
  }

  return hours * 60 + minutes;
}

// A written end time, in minutes from the start of the shift.
export function endAfter(start: number, time: number): number {
  return time <= start ? time + MINUTES_IN_DAY : time;
}

// "HH:MM - HH:MM label"
export function rangeLabel(start: number, end: number): string {
  return `${timeLabel(start)} – ${timeLabel(end)}`;
}

// "HH:MM to HH:MM label"
export function rangeWords(start: number, end: number): string {
  return `${timeLabel(start)} to ${timeLabel(end)}`;
}

// For shifts that go past midnight, returns the shift's segments for each day
export function daySegments(
  shift: DraftShift,
): { date: string; from: number; to: number; continued: boolean }[] {
  const segments = [
    {
      date: shift.dutyDate,
      from: shift.start,
      to: Math.min(shift.end, MINUTES_IN_DAY),
      continued: false,
    },
  ];

  // Pushes a segment for the next day if the shift goes past midnight
  if (shift.end > MINUTES_IN_DAY) {
    segments.push({
      date: addDays(shift.dutyDate, 1),
      from: 0,
      to: shift.end - MINUTES_IN_DAY,
      continued: true,
    });
  }

  return segments;
}

/**
 * The free times of a day, given the times its shifts cover
 */
export function freeGaps(covered: Range[], shortest = MIN_FREE_TIME_FOR_NEW_SHIFT_BUTTON): Range[] {
  const sorted = covered
    .map(({ from, to }) => ({ from: Math.max(0, from), to: Math.min(MINUTES_IN_DAY, to) }))
    .filter(({ from, to }) => to > from)
    .sort((a, b) => a.from - b.from);

  const gaps: Range[] = [];
  let free = 0;

  for (const range of sorted) {
    if (range.from - free >= shortest) {
      gaps.push({ from: free, to: range.from });
    }
    free = Math.max(free, range.to);
  }

  if (MINUTES_IN_DAY - free >= shortest) {
    gaps.push({ from: free, to: MINUTES_IN_DAY });
  }
  return gaps;
}

// New shift range for a free gap
export function newShiftRange(gap: Range): Range {
  if (gap.from === 0 && gap.to === MINUTES_IN_DAY) {
    return { from: 8 * 60, to: 20 * 60 };
  }

  return { from: gap.from, to: Math.min(gap.to, gap.from + LONGEST_NEW_SHIFT) };
}

export function totalOf(crew: Crew): number {
  return SLOT_ROLES.reduce((sum, role) => sum + crew[role], 0);
}

export function assignedCount(shift: DraftShift): number {
  return SLOT_ROLES.reduce((sum, role) => sum + shift.assigned[role].length, 0);
}

// The number of people missing from a shift
export function missingCount(shift: DraftShift): number {
  return SLOT_ROLES.reduce(
    (sum, role) => sum + Math.max(0, shift.crew[role] - shift.assigned[role].length),
    0,
  );
}

/**
 * What a "needed" box keeps as the supervisor types: digits only, at most MAXIMUM_PER_ROLE. An empty box or 0 is
 * kept as typed and changes nothing (count 0): the role's button takes the role off.
 */
export function readNeeded(value: string): { shown: string; count: number } {
  const digits = value.replace(/\D/g, '').slice(0, 2);
  const count = Math.min(MAXIMUM_PER_ROLE, Number(digits));
  if (count === 0) {
    return { shown: digits, count: 0 };
  }
  return { shown: String(count), count };
}

// Who a short shift still needs, by role: "Needs 1 driver, 2 crew members"
export function missingLabel(shift: DraftShift): string {
  const parts = SLOT_ROLES.map((role) => ({
    role,
    missing: shift.crew[role] - shift.assigned[role].length,
  }))
    .filter((each) => each.missing > 0)
    .map(
      ({ role, missing }) =>
        `${missing} ${ROLE_LABELS[role].toLowerCase()}${missing === 1 ? '' : 's'}`,
    );
  return `Needs ${parts.join(', ')}`;
}

/**
 * Past: on a day before today. Not editable.
 *
 * Full: every role it needs has its people. Editable.
 *
 * Short: someone is missing, or it has no roles yet. Editable.
 */
export type ShiftState = 'past' | 'full' | 'short';

export function shiftState(shift: DraftShift, today: string): ShiftState {
  if (shift.dutyDate < today) {
    return 'past';
  }
  return totalOf(shift.crew) > 0 && missingCount(shift) === 0 ? 'full' : 'short';
}

// The roles a shift needs
export function rolesOf(crew: Crew): SlotRole[] {
  return SLOT_ROLES.filter((role) => crew[role] > 0);
}

export type RoleState = 'full' | 'short';

export function roleState(required: number, assigned: number): RoleState {
  return assigned >= required ? 'full' : 'short';
}

export interface Candidate {
  member: ScheduleMember;
  functions: string; // Ex: "Crew chief · Driver"
  alreadyOn: string | null; // Another shift of theirs that day, which does not overlap this one
}

/**
 * Who can be assigned to the shift in that role: the members with the role the eligibility worker found ELIGIBLE
 * (RN01, RN05, RN06; SCRUM-171), who are not on the shift already.
 */
export function candidates(
  shift: DraftShift,
  role: SlotRole,
  members: ScheduleMember[],
  weekShifts: DraftShift[],
  eligible: ReadonlySet<string>,
): Candidate[] {
  const onShift = new Set(SLOT_ROLES.flatMap((each) => shift.assigned[each]));

  return members
    .filter(
      (member) => member.roles.includes(role) && eligible.has(member.id) && !onShift.has(member.id),
    )
    .map((member) => ({
      member,
      others: weekShifts.filter(
        (each) =>
          each.id !== shift.id &&
          SLOT_ROLES.some((slot) => each.assigned[slot].includes(member.id)),
      ),
    }))
    .map(({ member, others }) => {
      const sameDay = others.find((other) => other.dutyDate === shift.dutyDate);
      return {
        member,
        functions: member.roles.map((each) => ROLE_LABELS[each]).join(' · '),
        alreadyOn: sameDay ? rangeLabel(sameDay.start, sameDay.end) : null,
      };
    });
}

// The number of changes in a draft week, including deleted shifts
export function changeCount(draft: WeekDraft | undefined): number {
  if (!draft) {
    return 0;
  }
  return draft.shifts.filter((shift) => shift.change !== 'published').length + draft.deleted.length;
}

/**
 * A piquete from the API as a published draft shift, or null when it has no times yet.
 */
export function fromPiquete(piquete: Piquete): DraftShift | null {
  if (!piquete.startsAt || !piquete.endsAt) {
    return null;
  }

  const start = lisbonTime(piquete.startsAt);
  const end = lisbonTime(piquete.endsAt);
  const days = Math.round(
    (Date.parse(`${end.date}T00:00:00Z`) - Date.parse(`${piquete.dutyDate}T00:00:00Z`)) /
      86_400_000,
  );

  const startMinutes = start.date === piquete.dutyDate ? start.minutes : 0;
  const endMinutes = Math.min(days * MINUTES_IN_DAY + end.minutes, startMinutes + MINUTES_IN_DAY);
  if (endMinutes <= startMinutes) {
    return null;
  }

  // Count the roles in the piquete's slots, and who holds them once the week is published
  const crew = emptyCrew();
  const assigned = emptyAssigned();
  for (const slot of piquete.slots) {
    if ((SLOT_ROLES as readonly string[]).includes(slot.role)) {
      crew[slot.role as SlotRole]++;
      if (slot.memberId) {
        assigned[slot.role as SlotRole].push(slot.memberId);
      }
    }
  }

  // Add any extra crew members needed
  const extra = (piquete.minimumCrew ?? 0) - totalOf(crew);
  if (extra > 0) {
    crew.CREW_MEMBER += extra;
  }

  return {
    id: piquete.id,
    dutyDate: piquete.dutyDate,
    start: startMinutes,
    end: endMinutes,
    crew,
    assigned,
    slots: piquete.slots,
    change: 'published',
  };
}
