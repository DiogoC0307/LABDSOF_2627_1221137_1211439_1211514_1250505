import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { addDays, isoWeek, mondayOf } from './iso-week';
import { SLOT_ROLES, SlotRole } from './piquete';
import { DraftShift, ScheduleMember } from './schedule-model';
import { MINUTES_IN_DAY, timeLabel } from './schedule-rules';
import { lisbonInstant } from './shift-time';

/**
 * The members the Schedule page can assign, and when they can't come, for the supervisor.
 *
 * Members come from GET /api/v1/members
 * The periods they marked in "When I can't" come from GET /api/v1/unavailabilities/team
 *
 * Both are supervisor-only: someone else who signs in on this device never sees what was loaded before.
 */

// A member as GET /api/v1/members answers
interface ApiMember {
  id: string;
  fullName: string;
  active: boolean;
  rosterStatus: string; // Only ACTIVE_ROSTER can be scheduled (RN01)
  functions: string[];
}

// A period in which a member can't come
interface TeamUnavailability {
  memberId: string;
  startsAt: string;
  endsAt: string;
}

@Injectable({ providedIn: 'root' })
export class ScheduleDirectory {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly owner = computed(() => this.auth.session()?.memberId ?? null);

  // Whose members and periods are held; nothing is shown to anyone else
  private readonly loadedFor = signal<string | null | undefined>(undefined);
  private readonly current = computed(() => this.loadedFor() === this.owner());

  private readonly loadedMembers = signal<ScheduleMember[]>([]);
  private readonly membersError = signal<string | null>(null);
  private readonly periodsError = signal<string | null>(null);
  private readonly periods = signal<Record<string, TeamUnavailability[]>>({}); // Keyed by the week's Monday
  private membersLoaded = false;
  private readonly loadingWeeks = new Set<string>();

  readonly members = computed(() => (this.current() ? this.loadedMembers() : [])); // Active roster with a role, by name
  readonly error = computed(() => (this.current() ? (this.membersError() ?? this.periodsError()) : null)); // Shown in the shift side panel

  // Loads the members, if not loaded yet, and the periods of the week of that Monday
  load(monday: string): void {
    const owner = this.owner();
    if (this.loadedFor() !== owner) {
      this.forget(owner);
    }

    if (!this.membersLoaded) {
      this.membersLoaded = true;
      this.http.get<ApiMember[]>('/api/v1/members').subscribe({
        next: (list) => {
          if (this.loadedFor() === owner) {
            this.membersError.set(null);
            this.loadedMembers.set(toScheduleMembers(list));
          }
        },
        error: () => {
          if (this.loadedFor() === owner) {
            // Tried again the next time a shift is opened
            this.membersLoaded = false;
            this.membersError.set('Could not load the members.');
          }
        },
      });
    }

    if (monday in this.periods() || this.loadingWeeks.has(monday)) {
      return;
    }

    this.loadingWeeks.add(monday);
    this.http
      .get<TeamUnavailability[]>('/api/v1/unavailabilities/team', {
        params: new HttpParams().set('week', isoWeek(monday)),
      })
      .subscribe({
        next: (list) => {
          if (this.loadedFor() === owner) {
            this.loadingWeeks.delete(monday);
            this.periodsError.set(null);
            this.periods.update((weeks) => ({ ...weeks, [monday]: list }));
          }
        },
        error: () => {
          if (this.loadedFor() === owner) {
            this.loadingWeeks.delete(monday);
            this.periodsError.set("Could not load who can't come.");
          }
        },
      });
  }

  // The members who can't come for some or all of the shift's time
  unavailableFor(shift: DraftShift): ReadonlySet<string> {
    const periods = this.current() ? (this.periods()[mondayOf(shift.dutyDate)] ?? []) : [];
    const start = Date.parse(lisbonInstant(shift.dutyDate, timeLabel(shift.start)));
    const end = Date.parse(
      lisbonInstant(
        addDays(shift.dutyDate, Math.floor(shift.end / MINUTES_IN_DAY)),
        timeLabel(shift.end % MINUTES_IN_DAY),
      ),
    );

    return new Set(
      periods
        .filter((period) => Date.parse(period.startsAt) < end && Date.parse(period.endsAt) > start)
        .map((period) => period.memberId),
    );
  }

  name(id: string): string {
    return this.members().find((member) => member.id === id)?.name ?? 'Unknown member';
  }

  // Drops what was loaded for the person signed in before
  private forget(owner: string | null): void {
    this.loadedFor.set(owner);
    this.loadedMembers.set([]);
    this.membersError.set(null);
    this.periodsError.set(null);
    this.periods.set({});
    this.membersLoaded = false;
    this.loadingWeeks.clear();
  }
}

// Active members on the active roster (RN01) with at least one role of a shift
function toScheduleMembers(list: ApiMember[]): ScheduleMember[] {
  return list
    .filter((member) => member.active && member.rosterStatus === 'ACTIVE_ROSTER')
    .map((member) => ({
      id: member.id,
      name: member.fullName,
      roles: member.functions.filter((role): role is SlotRole => (SLOT_ROLES as readonly string[]).includes(role)),
    }))
    .filter((member) => member.roles.length > 0)
    .sort((a, b) => a.name.localeCompare(b.name));
}
