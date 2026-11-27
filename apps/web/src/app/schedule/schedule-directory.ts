import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
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
 */

// A member as GET /api/v1/members answers
interface ApiMember {
  id: string;
  fullName: string;
  active: boolean;
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

  readonly members = signal<ScheduleMember[]>([]); // Active members with at least one role, by name
  readonly error = computed(() => this.membersError() ?? this.periodsError()); // Shown in the shift side panel

  private readonly membersError = signal<string | null>(null);
  private readonly periodsError = signal<string | null>(null);

  private readonly periods = signal<Record<string, TeamUnavailability[]>>({}); // Keyed by the week's Monday
  private membersLoaded = false;
  private readonly loadingWeeks = new Set<string>();

  // Loads the members, if not loaded yet, and the periods of the week of that Monday
  load(monday: string): void {
    if (!this.membersLoaded) {
      this.membersLoaded = true;
      this.http.get<ApiMember[]>('/api/v1/members').subscribe({
        next: (list) => {
          this.membersError.set(null);
          this.members.set(toScheduleMembers(list));
        },
        error: () => {
          // Tried again the next time a shift is opened
          this.membersLoaded = false;
          this.membersError.set('Could not load the members.');
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
          this.loadingWeeks.delete(monday);
          this.periodsError.set(null);
          this.periods.update((weeks) => ({ ...weeks, [monday]: list }));
        },
        error: () => {
          this.loadingWeeks.delete(monday);
          this.periodsError.set("Could not load who can't come.");
        },
      });
  }

  // The members who can't come for some or all of the shift's time
  unavailableFor(shift: DraftShift): ReadonlySet<string> {
    const periods = this.periods()[mondayOf(shift.dutyDate)] ?? [];
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
}

// Active members with at least one role of a shift
function toScheduleMembers(list: ApiMember[]): ScheduleMember[] {
  return list
    .map((member) => ({
      id: member.id,
      name: member.fullName,
      roles: member.functions.filter((role): role is SlotRole => (SLOT_ROLES as readonly string[]).includes(role)),
      active: member.active,
    }))
    .filter((member) => member.active && member.roles.length > 0)
    .map(({ id, name, roles }) => ({ id, name, roles }))
    .sort((a, b) => a.name.localeCompare(b.name));
}
