import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, switchMap, takeWhile, timer, last, timeout, catchError, of } from 'rxjs';
import { addDays, isoWeek, longDay } from './iso-week';
import { CrewNeed, ROLE_LABELS, SLOT_ROLES, ShiftPeriod, SlotRole } from './piquete';
import { ConflictLine } from './schedule-dialogs';
import { DraftShift, ScheduleMember } from './schedule-model';
import { MINUTES_IN_DAY, rangeLabel, timeLabel } from './schedule-rules';
import { lisbonInstant } from './shift-time';

export type PublicationStatus = 'CHECKING' | 'PUBLISHED' | 'BLOCKED' | 'SUPERSEDED';

export type ConflictType = 'EMPTY_SHIFT' | 'INELIGIBLE' | 'NOT_CHECKED';

// Why the week can't be published, as the API answers it
export interface PublicationConflict {
  type: ConflictType;
  shiftKey: string; // The draft shift's id
  shiftId: string;
  dutyDate: string;
  memberId?: string | null;
  role?: SlotRole | null;
  rules?: string[] | null; // RN01, RN05, RN06
}

export interface Publication {
  id: string;
  week: string;
  status: PublicationStatus;
  conflicts: PublicationConflict[];
}

// A shift as POST /api/v1/schedule-publications takes it
export interface PublishedShift {
  key: string;
  id: string | null;
  dutyDate: string;
  period: ShiftPeriod;
  startsAt: string;
  endsAt: string;
  crew: CrewNeed[];
  assigned: { role: SlotRole; memberId: string }[];
}

export interface PublicationRequest {
  week: string;
  shifts: PublishedShift[];
}

// How a publication ended for the page. TIMEOUT when the checks didn't finish in time
export type PublicationOutcome =
  | { kind: 'published' }
  | { kind: 'blocked'; conflicts: PublicationConflict[] }
  | { kind: 'timeout' };

const POLL_EVERY_MS = 1000;
const GIVE_UP_AFTER_MS = 60_000;

// A shift created in this draft has a client id until the API gives it one
export function isNewShift(id: string): boolean {
  return id.startsWith('new-');
}

export function periodOf(shift: DraftShift): ShiftPeriod {
  return shift.start >= 8 * 60 && shift.start < 20 * 60 && shift.end <= MINUTES_IN_DAY ? 'DAY' : 'NIGHT';
}

function instantOf(dutyDate: string, minutes: number): string {
  const days = Math.floor(minutes / MINUTES_IN_DAY);
  return lisbonInstant(addDays(dutyDate, days), timeLabel(minutes - days * MINUTES_IN_DAY));
}

export function publicationRequest(monday: string, shifts: DraftShift[], today: string): PublicationRequest {
  return {
    week: isoWeek(monday),
    shifts: shifts
      .filter((shift) => shift.dutyDate >= today)
      .map((shift) => ({
        key: shift.id,
        id: isNewShift(shift.id) ? null : shift.id,
        dutyDate: shift.dutyDate,
        period: periodOf(shift),
        startsAt: instantOf(shift.dutyDate, shift.start),
        endsAt: instantOf(shift.dutyDate, shift.end),
        crew: SLOT_ROLES.map((role) => ({ role, count: shift.crew[role] })),
        assigned: SLOT_ROLES.flatMap((role) =>
          shift.assigned[role].map((memberId) => ({ role, memberId })),
        ),
      })),
  };
}

@Injectable({ providedIn: 'root' })
export class SchedulePublisher {
  private readonly http = inject(HttpClient);

  /**
   * Sends the week and follows its checks: PUBLISHED, BLOCKED with the conflicts, or TIMEOUT after a minute still
   * CHECKING.
   */
  publish(request: PublicationRequest): Observable<PublicationOutcome> {
    return this.http.post<Publication>('/api/v1/schedule-publications', request).pipe(
      switchMap((started) =>
        started.status === 'CHECKING' ? this.follow(started.id) : of(started),
      ),
      map((done) => outcome(done)),
    );
  }

  private follow(id: string): Observable<Publication> {
    return timer(POLL_EVERY_MS, POLL_EVERY_MS).pipe(
      switchMap(() => this.http.get<Publication>(`/api/v1/schedule-publications/${id}`)),
      takeWhile((publication) => publication.status === 'CHECKING', true),
      last(),
      timeout({ first: GIVE_UP_AFTER_MS }),
      catchError((error: unknown) => {
        if (error instanceof Error && error.name === 'TimeoutError') {
          return of({ id, week: '', status: 'CHECKING' as const, conflicts: [] });
        }
        throw error;
      }),
    );
  }
}

function outcome(publication: Publication): PublicationOutcome {
  switch (publication.status) {
    case 'PUBLISHED':
      return { kind: 'published' };
    case 'BLOCKED':
      return { kind: 'blocked', conflicts: publication.conflicts };
    default:
      return { kind: 'timeout' };
  }
}

const RULE_PROBLEMS: Record<string, string> = {
  RN01: "isn't on the active roster, so they can't be scheduled.",
  RN05: 'is on another shift at the same time.',
  RN06: "said they can't come during this shift.",
};

// Given a list of conflicts, the shifts and the members, returns a list of lines to show in the dialog.
export function conflictLines(
  conflicts: PublicationConflict[],
  shifts: DraftShift[],
  members: ScheduleMember[],
): ConflictLine[] {
  return conflicts.map((conflict) => {
    const shift = shifts.find((each) => each.id === conflict.shiftKey);
    const when = shift
      ? `${longDay(shift.dutyDate)}, ${rangeLabel(shift.start, shift.end)}`
      : longDay(conflict.dutyDate);
    return { shiftKey: conflict.shiftKey, shift: when, problem: problemOf(conflict, members) };
  });
}

function problemOf(conflict: PublicationConflict, members: ScheduleMember[]): string {
  if (conflict.type === 'EMPTY_SHIFT') {
    return 'Nobody is on this shift. Add someone to it or delete it.';
  }
  const name = members.find((member) => member.id === conflict.memberId)?.name ?? 'A member';
  const who = conflict.role ? `${name} (${ROLE_LABELS[conflict.role]})` : name;
  if (conflict.type === 'NOT_CHECKED') {
    return `${who} couldn't be checked. Publish again to check them again.`;
  }
  const rules = conflict.rules ?? [];
  if (rules.length === 0) {
    return `${who} can't be on this shift.`;
  }
  return rules
    .map((rule, index) => {
      const problem = RULE_PROBLEMS[rule] ?? `can't be on this shift (${rule}).`;
      return `${index === 0 ? who : 'They also'} ${problem}`;
    })
    .join(' ');
}
