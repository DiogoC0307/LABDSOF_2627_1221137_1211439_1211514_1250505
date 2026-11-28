import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { SLOT_ROLES, SlotRole } from './piquete';
import { ScheduleMember } from './schedule-model';

/**
 * The members of the corps, with their names and functions, for the supervisor's Schedule page (GET /api/v1/members).
 *
 * Who may take a role on a shift is never decided here: the eligibility worker checks it (CandidateChecks, SCRUM-171).
 * Supervisor-only: someone else who signs in on this device never sees what was loaded before.
 */

// A member as GET /api/v1/members answers
interface ApiMember {
  id: string;
  fullName: string;
  functions: string[];
}

@Injectable({ providedIn: 'root' })
export class ScheduleDirectory {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly owner = computed(() => this.auth.session()?.memberId ?? null);

  // Whose members are held; nothing is shown to anyone else
  private readonly loadedFor = signal<string | null | undefined>(undefined);
  private readonly current = computed(() => this.loadedFor() === this.owner());

  private readonly loadedMembers = signal<ScheduleMember[]>([]);
  private readonly membersError = signal<string | null>(null);
  private membersLoaded = false;

  readonly members = computed(() => (this.current() ? this.loadedMembers() : [])); // Members with a role, by name
  readonly error = computed(() => (this.current() ? this.membersError() : null)); // Shown in the shift side panel

  // Loads the members, if not loaded yet
  load(): void {
    const owner = this.owner();
    if (this.loadedFor() !== owner) {
      this.forget(owner);
    }
    if (this.membersLoaded) {
      return;
    }

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

  name(id: string): string {
    return this.members().find((member) => member.id === id)?.name ?? 'Unknown member';
  }

  // Drops what was loaded for the person signed in before
  private forget(owner: string | null): void {
    this.loadedFor.set(owner);
    this.loadedMembers.set([]);
    this.membersError.set(null);
    this.membersLoaded = false;
  }
}

// Members with at least one role of a shift, by name; whether they may be scheduled is the worker's (RN01)
function toScheduleMembers(list: ApiMember[]): ScheduleMember[] {
  return list
    .map((member) => ({
      id: member.id,
      name: member.fullName,
      roles: member.functions.filter((role): role is SlotRole => (SLOT_ROLES as readonly string[]).includes(role)),
    }))
    .filter((member) => member.roles.length > 0)
    .sort((a, b) => a.name.localeCompare(b.name));
}
