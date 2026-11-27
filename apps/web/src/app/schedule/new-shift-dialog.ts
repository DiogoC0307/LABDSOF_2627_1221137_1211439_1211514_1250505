import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from '@angular/material/dialog';
import { ROLE_LABELS, SLOT_ROLES, SlotRole } from './piquete';
import { Crew, emptyCrew } from './schedule-model';
import {
  MINUTES_IN_DAY,
  endAfter,
  parseTime,
  readNeeded,
  rolesOf,
  timeLabel,
  totalOf,
} from './schedule-rules';
import { TimeField, WHOLE_HOURS, endHours, endLabel } from './time-field';

export interface NewShiftData {
  /** "Monday 30 November". */
  day: string;
  /** The times the dialog starts with, from the free time that was pressed. */
  start: number;
  end: number;
}

/** What "Create shift" gives back: the shift's times and how many people each role needs. */
export interface NewShift {
  start: number;
  end: number;
  crew: Crew;
}

/**
 * Add shift: its times, and which roles it needs and how many people for each. Only the chosen roles get a "Needed"
 * box; the shift needs at least one role before it can be created.
 */
@Component({
  selector: 'app-new-shift-dialog',
  imports: [
    MatButton,
    MatDialogActions,
    MatDialogClose,
    MatDialogContent,
    MatDialogTitle,
    TimeField,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './new-shift-dialog.html',
})
export class NewShiftDialog {
  protected readonly data = inject<NewShiftData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject<MatDialogRef<NewShiftDialog, NewShift>>(MatDialogRef);

  protected readonly start = signal(this.data.start);
  protected readonly end = signal(this.data.end);
  protected readonly crew = signal<Crew>(emptyCrew());
  /** A Start or End box holding text that is not a time: the shift can't be created until it is fixed. */
  private readonly startWrong = signal(false);
  private readonly endWrong = signal(false);

  protected readonly startShown = computed(() => timeLabel(this.start()));
  protected readonly endShown = computed(() => endLabel(this.end()));
  protected readonly startOptions = WHOLE_HOURS;
  protected readonly endOptions = computed(() => endHours(this.start()));

  protected readonly choices = computed(() =>
    SLOT_ROLES.map((role) => ({ role, name: ROLE_LABELS[role], chosen: this.crew()[role] > 0 })),
  );
  protected readonly chosen = computed(() =>
    rolesOf(this.crew()).map((role) => ({
      role,
      name: ROLE_LABELS[role],
      needed: this.crew()[role],
    })),
  );
  protected readonly canCreate = computed(
    () => totalOf(this.crew()) > 0 && !this.startWrong() && !this.endWrong(),
  );

  /** A new start keeps the end time; when the end is then at or before the start, it is the next day's. */
  protected readonly takeStart = (text: string): string | null => {
    const start = parseTime(text);
    this.startWrong.set(start === null || start >= MINUTES_IN_DAY);
    if (start === null || start >= MINUTES_IN_DAY) {
      return null;
    }
    this.end.set(endAfter(start, this.end() % MINUTES_IN_DAY));
    this.start.set(start);
    return timeLabel(start);
  };

  /** An end at or before the start is on the next day: 20:00 to 08:00 is a night shift. */
  protected readonly takeEnd = (text: string): string | null => {
    const time = parseTime(text);
    this.endWrong.set(time === null);
    if (time === null) {
      return null;
    }
    const end = endAfter(this.start(), time);
    this.end.set(end);
    return endLabel(end);
  };

  /** A role the shift needs starts at 1 person; pressed again, the shift no longer needs it. */
  protected toggle(role: SlotRole): void {
    this.crew.update((crew) => ({ ...crew, [role]: crew[role] > 0 ? 0 : 1 }));
  }

  /** Digits only, 1 to 20. An empty box or 0 changes nothing: the role's button takes the role off. */
  protected setNeeded(role: SlotRole, field: HTMLInputElement): void {
    const { shown, count } = readNeeded(field.value);
    field.value = shown;
    if (count > 0) {
      this.crew.update((crew) => ({ ...crew, [role]: count }));
    }
  }

  /** Puts the number back in a box left empty or at 0. */
  protected showNeeded(role: SlotRole, field: HTMLInputElement): void {
    field.value = String(this.crew()[role]);
  }

  protected create(): void {
    if (this.canCreate()) {
      this.dialogRef.close({ start: this.start(), end: this.end(), crew: this.crew() });
    }
  }
}
