import { Slot, SlotRole } from './piquete';

export type ShiftChange = 'new' | 'edited' | 'published';
export type Crew = Record<SlotRole, number>;
export type Assigned = Record<SlotRole, string[]>;

export interface DraftShift {
  id: string;
  dutyDate: string;
  start: number;
  end: number;
  crew: Crew; // How many people each role needs.
  assigned: Assigned; // The members assigned to the shift
  slots: Slot[];
  change: ShiftChange;
}

export interface WeekDraft {
  published: DraftShift[];
  shifts: DraftShift[];
  deleted: string[]; // Published shifts deleted in the draft
}

// A member in the Schedule Page, their identity and available roles.
export interface ScheduleMember {
  id: string;
  name: string;
  roles: SlotRole[];
}

export function emptyCrew(): Crew {
  return { CREW_CHIEF: 0, DRIVER: 0, CREW_MEMBER: 0 };
}

export function emptyAssigned(): Assigned {
  return { CREW_CHIEF: [], DRIVER: [], CREW_MEMBER: [] };
}
