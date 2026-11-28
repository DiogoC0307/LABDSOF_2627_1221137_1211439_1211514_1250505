import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, last, of, switchMap, takeWhile, timeout, timer } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { addDays } from './iso-week';
import { SLOT_ROLES, SlotRole } from './piquete';
import { DraftShift } from './schedule-model';
import { MINUTES_IN_DAY, timeLabel } from './schedule-rules';
import { lisbonInstant } from './shift-time';

/**
 * Who can take a role on a shift
 */

export type CandidateResult = 'PENDING' | 'ELIGIBLE' | 'INELIGIBLE' | 'NOT_CHECKED';

// A member with the role, as the worker answered: the rules broken when INELIGIBLE (RN01, RN05, RN06)
export interface CandidateAnswer {
  memberId: string;
  result: CandidateResult;
  rules: string[];
}

interface CandidateCheckResponse {
  id: string;
  status: 'CHECKING' | 'DONE';
  candidates: CandidateAnswer[];
}

// A role's check, as the shift panel shows it
export type RoleCheck =
  | { state: 'checking' }
  | { state: 'done'; answers: CandidateAnswer[] }
  | { state: 'failed' };

export interface CandidateCheckRequest {
  startsAt: string;
  endsAt: string;
  role: SlotRole;
  otherShifts: { memberId: string; startsAt: string; endsAt: string }[];
}

// What the worker is asked; the same question is asked once
export interface CandidateQuestion {
  key: string;
  body: CandidateCheckRequest;
}

const POLL_EVERY_MS = 500;
const GIVE_UP_AFTER_MS = 20_000;

// A shift's start and end as instants in UTC
export function shiftInstants(shift: DraftShift): { startsAt: string; endsAt: string } {
  const at = (minutes: number) => {
    const days = Math.floor(minutes / MINUTES_IN_DAY);
    return lisbonInstant(addDays(shift.dutyDate, days), timeLabel(minutes - days * MINUTES_IN_DAY));
  };
  return { startsAt: at(shift.start), endsAt: at(shift.end) };
}

/**
 * The question for a role on the shift: its times, and every member on the week's other shifts with that shift's
 * times.
 */
export function candidateQuestion(shift: DraftShift, role: SlotRole, weekShifts: DraftShift[]): CandidateQuestion {
  const otherShifts = weekShifts
    .filter((other) => other.id !== shift.id)
    .flatMap((other) => {
      const times = shiftInstants(other);
      return SLOT_ROLES.flatMap((each) => other.assigned[each].map((memberId) => ({ memberId, ...times })));
    })
    .sort((a, b) => `${a.memberId}${a.startsAt}`.localeCompare(`${b.memberId}${b.startsAt}`));
  const body: CandidateCheckRequest = { ...shiftInstants(shift), role, otherShifts };
  return { key: JSON.stringify(body), body };
}

@Injectable({ providedIn: 'root' })
export class CandidateChecks {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly owner = computed(() => this.auth.session()?.memberId ?? null);

  // The answers of whoever asked, by question; nobody else who signs in on this device sees them
  private readonly store = signal<{ owner: string | null; checks: Record<string, RoleCheck> }>({
    owner: null,
    checks: {},
  });

  readonly checks = computed(() => (this.store().owner === this.owner() ? this.store().checks : {}));

  /** Asks the worker, unless this question was asked already. */
  ask(question: CandidateQuestion): void {
    if (question.key in this.checks()) {
      return;
    }
    const owner = this.owner();
    this.set(owner, question.key, { state: 'checking' });
    this.follow(question.body).subscribe({
      next: (done) => this.set(owner, question.key, { state: 'done', answers: done.candidates }),
      error: () => this.set(owner, question.key, { state: 'failed' }),
    });
  }

  /** Asks again a question whose check failed. */
  retry(question: CandidateQuestion): void {
    this.store.update((store) => {
      const checks = { ...store.checks };
      delete checks[question.key];
      return { ...store, checks };
    });
    this.ask(question);
  }

  private follow(body: CandidateCheckRequest): Observable<CandidateCheckResponse> {
    return this.http.post<CandidateCheckResponse>('/api/v1/candidate-checks', body).pipe(
      switchMap((started) =>
        started.status === 'DONE'
          ? of(started)
          : timer(POLL_EVERY_MS, POLL_EVERY_MS).pipe(
              switchMap(() => this.http.get<CandidateCheckResponse>(`/api/v1/candidate-checks/${started.id}`)),
              takeWhile((check) => check.status !== 'DONE', true),
              last(),
              timeout({ first: GIVE_UP_AFTER_MS }),
            ),
      ),
    );
  }

  private set(owner: string | null, key: string, check: RoleCheck): void {
    this.store.update((store) => ({
      owner,
      checks: { ...(store.owner === owner ? store.checks : {}), [key]: check },
    }));
  }
}
