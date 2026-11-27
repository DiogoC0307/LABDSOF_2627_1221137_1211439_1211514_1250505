import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { SlotRole } from './piquete';
import { changeCount } from './schedule-rules';
import { Crew, DraftShift, WeekDraft, emptyAssigned } from './schedule-model';

/**
 * The supervisor's edits, week by week, are kept on the client until schedule is published.
 *
 * A week's edits stay when the supervisor looks at another week. They belong to whoever made them: someone
 * else who signs in on this device never sees them.
 */
@Injectable({ providedIn: 'root' })
export class ScheduleDraftService {
  private readonly auth = inject(AuthService);
  private readonly owner = computed(() => this.auth.session()?.memberId ?? null);

  // The weeks of whoever made them, keyed by their Monday
  private readonly store = signal<{ owner: string | null; weeks: Record<string, WeekDraft> }>({
    owner: null,
    weeks: {},
  });
  private readonly weeks = computed(() =>
    this.store().owner === this.owner() ? this.store().weeks : {},
  );
  private lastNew = 0;

  // Get the week for a given monday
  week(monday: string): WeekDraft | undefined {
    return this.weeks()[monday];
  }

  // Get shifts for a given monday
  shifts(monday: string): DraftShift[] {
    return this.weeks()[monday]?.shifts ?? [];
  }

  // Get the number of changes for a given monday
  changes(monday: string): number {
    return changeCount(this.weeks()[monday]);
  }

  /**
   * Loads the week for a given monday, with the current published shifts and no edits.
   */
  load(monday: string, published: DraftShift[]): void {
    if (this.weeks()[monday]) {
      // If week already exists in draft, don't overwrite and keep its edits.
      return;
    }

    this.set(monday, { published, shifts: published, deleted: [] });
  }

  /** A new shift on a day, with the crew chosen in the Add shift dialog and nobody on it. */
  add(monday: string, dutyDate: string, start: number, end: number, crew: Crew): DraftShift {
    const shift: DraftShift = {
      id: `new-${++this.lastNew}`,
      dutyDate,
      start,
      end,
      crew: { ...crew },
      assigned: emptyAssigned(),
      change: 'new',
    };

    this.edit(monday, (draft) => ({ ...draft, shifts: [...draft.shifts, shift] }));
    return shift;
  }

  /**
   * Sets the start and end times for a shift.
   */
  setTimes(monday: string, id: string, start: number, end: number): void {
    this.update(monday, id, (shift) => ({ ...shift, start, end }));
  }

  /** How many people a role needs; 0 takes the role off the shift, with the people assigned to it. */
  setNeeded(monday: string, id: string, role: SlotRole, count: number): void {
    this.update(monday, id, (shift) => ({
      ...shift,
      crew: { ...shift.crew, [role]: count },
      assigned: count > 0 ? shift.assigned : { ...shift.assigned, [role]: [] },
    }));
  }

  assign(monday: string, id: string, role: SlotRole, memberId: string): void {
    this.update(monday, id, (shift) => ({
      ...shift,
      assigned: { ...shift.assigned, [role]: [...shift.assigned[role], memberId] },
    }));
  }

  unassign(monday: string, id: string, role: SlotRole, memberId: string): void {
    this.update(monday, id, (shift) => ({
      ...shift,
      assigned: {
        ...shift.assigned,
        [role]: shift.assigned[role].filter((each) => each !== memberId),
      },
    }));
  }

  /** Removes a shift from schedule */
  remove(monday: string, id: string): void {
    this.edit(monday, (draft) => {
      // Removes shift if it's in the schedule draft
      if (!draft.shifts.some((each) => each.id === id)) {
        // If shift isn't found in the draft return unchanged draft
        return draft;
      }

      // Removes shift if it's in the published schedule
      const wasPublished = draft.published.some((each) => each.id === id);
      return {
        ...draft,
        shifts: draft.shifts.filter((each) => each.id !== id),
        deleted: wasPublished ? [...draft.deleted, id] : draft.deleted,
      };
    });
  }

  /** Back to what members see. */
  discard(monday: string): void {
    this.edit(monday, (draft) => ({ ...draft, shifts: draft.published, deleted: [] }));
  }

  /**
   * MOCKUP: marks the week's draft as published on the client only. This is where the week's changes (new, edited
   * and deleted shifts, with their crew and members) will be sent to the API in one call.
   */
  publish(monday: string): void {
    this.edit(monday, (draft) => {
      const published = draft.shifts.map((shift) => ({ ...shift, change: 'published' as const }));
      return { published, shifts: published, deleted: [] };
    });
  }

  private update(monday: string, id: string, change: (shift: DraftShift) => DraftShift): void {
    this.edit(monday, (draft) => ({
      ...draft,
      shifts: draft.shifts.map((shift) => {
        if (shift.id !== id) {
          return shift;
        }
        const changed = change(shift);
        return { ...changed, change: shift.change === 'new' ? 'new' : 'edited' };
      }),
    }));
  }

  private edit(monday: string, change: (draft: WeekDraft) => WeekDraft): void {
    const draft = this.weeks()[monday];
    if (draft) {
      this.set(monday, change(draft));
    }
  }

  private set(monday: string, draft: WeekDraft): void {
    const owner = this.owner();
    this.store.update((store) => ({
      owner,
      weeks: { ...(store.owner === owner ? store.weeks : {}), [monday]: draft },
    }));
  }
}
