import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { longDay } from './iso-week';
import { ROLE_LABELS, SLOT_ROLES, SlotRole } from './piquete';
import { ScheduleDirectory } from './schedule-directory';
import { ScheduleDraftService } from './schedule-draft.service';
import { DraftShift } from './schedule-model';
import {
  Candidate,
  MINUTES_IN_DAY,
  MAXIMUM_PER_ROLE,
  RoleState,
  candidates,
  endAfter,
  parseTime,
  rangeLabel,
  roleState,
  rolesOf,
  timeLabel,
} from './schedule-rules';
import { TimeField, WHOLE_HOURS, endHours, endLabel } from './time-field';

// Number of candidates to show before the "Show all" button appears
const NUM_CANDIDATES = 3;

interface RoleSection {
  role: SlotRole;
  name: string;
  required: number;
  state: RoleState; // Ex: "1 of 1 · Full", "2 of 3 · Needs 1"
  pill: string;
  people: { id: string; name: string }[];
  shown: Candidate[];
  hidden: number;
}

/**
 * A selected shift side panel component with its date, time, roles, assigned people, and candidates
 */
@Component({
  selector: 'app-shift-panel',
  imports: [MatButton, MatIconButton, TimeField],
  templateUrl: './shift-panel.html',
  styleUrl: './shift-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex min-h-0 flex-col' },
})
export class ShiftPanel {
  private readonly draft = inject(ScheduleDraftService);
  private readonly directory = inject(ScheduleDirectory);
  protected readonly directoryError = this.directory.error; // The members or who can't come could not be loaded
  private readonly injector = inject(Injector);

  readonly monday = input.required<string>();
  readonly shift = input.required<DraftShift>();
  readonly dayShifts = input.required<DraftShift[]>();
  readonly editable = input.required<boolean>();
  readonly note = input<string | null>(null); // For shifts that have already happened

  readonly closed = output<void>();
  readonly deleteRequested = output<void>();

  private readonly heading = viewChild<ElementRef<HTMLElement>>('heading');

  protected readonly day = computed(() => longDay(this.shift().dutyDate));
  protected readonly time = computed(() => rangeLabel(this.shift().start, this.shift().end));

  // Start and end time input fields values and dropdown options
  protected readonly startShown = computed(() => timeLabel(this.shift().start));
  protected readonly endShown = computed(() => endLabel(this.shift().end));
  protected readonly startOptions = WHOLE_HOURS;
  protected readonly endOptions = computed(() => endHours(this.shift().start));

  // A new start keeps the end time; when the end is then at or before the start, it is the next day's.
  protected readonly takeStart = (text: string): string | null => {
    const start = parseTime(text);
    if (start === null || start >= MINUTES_IN_DAY) {
      return null;
    }

    const shift = this.shift();
    if (start !== shift.start) {
      this.draft.setTimes(this.monday(), shift.id, start, endAfter(start, shift.end % MINUTES_IN_DAY));
    }
    return timeLabel(start);
  };

  /** An end at or before the start is on the next day: 20:00 to 08:00 is a night shift. */
  protected readonly takeEnd = (text: string): string | null => {
    const time = parseTime(text);
    if (time === null) {
      return null;
    }
    const shift = this.shift();
    const end = endAfter(shift.start, time);
    if (end !== shift.end) {
      this.draft.setTimes(this.monday(), shift.id, shift.start, end);
    }
    return endLabel(end);
  };

  private readonly expanded = signal<ReadonlySet<SlotRole>>(new Set());

  /** The shift needs only one role: its button can't take it off, as a shift keeps at least one role. */
  protected readonly oneRole = computed(() => rolesOf(this.shift().crew).length === 1);

  /** Every role, and whether the shift needs it: the role buttons on top of the panel. */
  protected readonly roleChoices = computed(() => {
    const crew = this.shift().crew;
    return SLOT_ROLES.map((role) => ({
      role,
      name: ROLE_LABELS[role],
      chosen: crew[role] > 0,
      locked: crew[role] > 0 && this.oneRole(),
    }));
  });

  /** Only the roles the shift needs get a section. */
  protected readonly sections = computed<RoleSection[]>(() => {
    const shift = this.shift();
    const unavailable = this.directory.unavailableFor(shift);
    return rolesOf(shift.crew).map((role) => {
      const required = shift.crew[role];
      const people = shift.assigned[role].map((id) => ({ id, name: this.directory.name(id) }));
      const state = roleState(required, people.length);
      const all = candidates(shift, role, this.directory.members(), this.dayShifts(), unavailable);
      const shown = this.expanded().has(role) ? all : all.slice(0, NUM_CANDIDATES);
      return {
        role,
        name: ROLE_LABELS[role],
        required,
        state,
        pill: pill(state, required, people.length),
        people,
        shown,
        hidden: all.length - shown.length,
      };
    });
  });

  // Who can't come for some or all of the shift's time, from "When I can't"
  protected readonly cantCome = computed(() =>
    [...this.directory.unavailableFor(this.shift())].map((id) => this.directory.name(id)).sort(),
  );

  constructor() {
    // A newly selected shift starts with short candidate lists and takes the focus, so the panel is read from its top.
    const shiftId = computed(() => this.shift().id);
    effect(() => {
      shiftId();
      untracked(() => {
        this.expanded.set(new Set());
        afterNextRender(() => this.heading()?.nativeElement.focus(), { injector: this.injector });
      });
    });
  }

  /** Adds a role the shift needs, for 1 person, or takes it off with the people assigned to it (not its only role). */
  protected toggleRole(role: SlotRole): void {
    const crew = this.shift().crew;
    if (crew[role] > 0 && this.oneRole()) {
      return;
    }
    const count = crew[role] > 0 ? 0 : 1;
    this.draft.setNeeded(this.monday(), this.shift().id, role, count);
  }

  /**
   * Digits only, 1 to 20. An empty box or 0 changes nothing, so the role stays while its number is rewritten; the
   * role's button takes it off the shift.
   */
  protected setNeeded(role: SlotRole, field: HTMLInputElement): void {
    const digits = field.value.replace(/\D/g, '').slice(0, 2);
    const count = Math.min(MAXIMUM_PER_ROLE, Number(digits));
    field.value = digits === '' ? '' : count === 0 ? digits : String(count);
    if (count > 0) {
      this.draft.setNeeded(this.monday(), this.shift().id, role, count);
    }
  }

  /** Puts the count back in a box left empty or at 0. */
  protected showNeeded(role: SlotRole, field: HTMLInputElement): void {
    field.value = String(this.shift().crew[role]);
  }

  protected assign(role: SlotRole, memberId: string): void {
    this.draft.assign(this.monday(), this.shift().id, role, memberId);
  }

  protected unassign(role: SlotRole, memberId: string): void {
    this.draft.unassign(this.monday(), this.shift().id, role, memberId);
  }

  protected showAll(role: SlotRole): void {
    this.expanded.update((roles) => new Set([...roles, role]));
  }
}

function pill(state: RoleState, required: number, assigned: number): string {
  return state === 'full'
    ? `${assigned} of ${required} · Full`
    : `${assigned} of ${required} · Needs ${required - assigned}`;
}
