/** A piquete as GET /api/v1/piquetes returns it, and the shift the supervisor sends (contracts/openapi.yaml). */

export const SLOT_ROLES = ['CREW_CHIEF', 'DRIVER', 'CREW_MEMBER'] as const;
export type SlotRole = (typeof SLOT_ROLES)[number];

export const ROLE_LABELS: Record<SlotRole, string> = {
  CREW_CHIEF: 'Crew chief',
  DRIVER: 'Driver',
  CREW_MEMBER: 'Crew member',
};

export type PresenceStatus = 'UNCONFIRMED' | 'COMING' | 'CANNOT_COME';

export interface Slot {
  id: string;
  role: string;
  status: string;
  memberId?: string | null; // Who holds the slot once its week is published (SCRUM-66)
  presenceStatus?: PresenceStatus | null;
}

// How many people of a role for a shift
export interface CrewNeed {
  role: SlotRole;
  count: number;
}

export type ShiftPeriod = 'DAY' | 'NIGHT';

/**
 * A shift (SCRUM-65): period, startsAt, endsAt and minimumCrew are null on a piquete created before shifts had times.
 * minimumCrew is the number of people its crew needs, one slot each.
 * startsAt and endsAt are instants in UTC (ADR 0012).
 */
export interface Piquete {
  id: string;
  dutyDate: string;
  period?: ShiftPeriod | null;
  startsAt?: string | null;
  endsAt?: string | null;
  minimumCrew?: number | null;
  slots: Slot[];
}

/** The body of POST /api/v1/piquetes and PUT /api/v1/piquetes/{id}. */
export interface ShiftRequest {
  dutyDate: string;
  period: ShiftPeriod;
  startsAt: string;
  endsAt: string;
  crew: CrewNeed[]; // Required roles and quantity of each role for the shift. When count is 0, the role is not required for the shift.
}

/** What the form opens on: a new shift on a day, or a shift to change. */
export type ShiftFormTarget = { kind: 'add'; date: string } | { kind: 'edit'; piquete: Piquete };
