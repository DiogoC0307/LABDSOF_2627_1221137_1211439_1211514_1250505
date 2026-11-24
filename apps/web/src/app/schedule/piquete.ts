/** A piquete as GET /api/v1/piquetes returns it, and the shift the supervisor sends (contracts/openapi.yaml). */

export interface Slot {
  id: string;
  role: string;
  status: string;
}

export type ShiftPeriod = 'DAY' | 'NIGHT';

/**
 * A shift (SCRUM-65): period, startsAt, endsAt and minimumCrew are null on a piquete created before shifts had times.
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
  minimumCrew: number;
}

/** What the form opens on: a new shift on a day, or a shift to change. */
export type ShiftFormTarget = { kind: 'add'; date: string } | { kind: 'edit'; piquete: Piquete };
