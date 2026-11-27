import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  input,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { MatAutocomplete, MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { ErrorStateMatcher, MatOption } from '@angular/material/core';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MINUTES_IN_DAY, timeLabel } from './schedule-rules';

// Start and End time boxes, used in the shift side panel and in the Add shift dialog

// "00:00" to "23:00": the list of a Start box
export const WHOLE_HOURS: readonly string[] = Array.from({ length: 24 }, (_, hour) => timeLabel(hour * 60));

// An end time in format "HH:MM", with "(next day)" after midnight
export function endLabel(value: number): string {
  return value > MINUTES_IN_DAY ? `${timeLabel(value)} (next day)` : timeLabel(value);
}

// The list of an End box: from the hour after the start to 24 hours after it
export function endHours(start: number): string[] {
  const hours: string[] = [];

  for (let value = Math.floor(start / 60) * 60 + 60; value <= start + MINUTES_IN_DAY; value += 60) {
    hours.push(endLabel(value));
  }

  return hours;
}

/**
 * A Start or End box: pick a whole hour from the list, or write any time ("8:30").
 *
 * What was written is taken on Enter, on picking from the list or on leaving the box. `accept` returns how the time
 * reads, or null when it is not a time, and the box then shows its error.
 */
@Component({
  selector: 'app-time-field',
  imports: [MatAutocomplete, MatAutocompleteTrigger, MatError, MatFormField, MatInput, MatLabel, MatOption],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  styles: `
    // 52px tall: without this, the 44px minimum of every input (styles.scss) makes the box 76px.
    .mat-mdc-form-field {
      --mat-form-field-container-vertical-padding: 14px;
    }

    .mat-mdc-input-element {
      min-height: 0;
    }
  `,
  template: `
    <!-- Time input with its list of hours and its error message -->
    <mat-form-field class="w-full" appearance="outline" subscriptSizing="dynamic">
      <mat-label>{{ label() }}</mat-label>
      <input
        #box
        matInput
        autocomplete="off"
        [value]="text()"
        [disabled]="disabled()"
        [matAutocomplete]="list"
        #trigger="matAutocompleteTrigger"
        [errorStateMatcher]="errors"
        (input)="text.set(box.value)"
        (keydown.enter)="enter(trigger)"
        (blur)="commit()"
      />
      <mat-autocomplete #list="matAutocomplete" class="schedule-theme" (optionSelected)="pick($event)">
        @for (option of shownOptions(); track option) {
          <mat-option [value]="option">{{ option }}</mat-option>
        }
      </mat-autocomplete>
      <mat-error>Write a time like {{ example() }}</mat-error>
    </mat-form-field>
  `,
})
export class TimeField {
  readonly label = input.required<string>();
  readonly value = input.required<string>(); // The time the box shows when nothing is being written in it
  readonly options = input.required<readonly string[]>();
  readonly example = input.required<string>(); // The time in the error message: "Write a time like 08:30"
  readonly disabled = input(false);
  // Takes what was written: the time as the box should show it, or null when it is not one
  readonly accept = input.required<(text: string) => string | null>();

  protected readonly text = signal('');
  protected readonly shownOptions = computed(() => matching(this.options(), this.text(), this.value()));

  // The input has no form control, so it is told when to check its error again.
  private invalid = false;
  protected readonly errors: ErrorStateMatcher = { isErrorState: () => this.invalid };
  private readonly field = viewChild(MatInput);

  constructor() {
    // When the time changes (or another shift is shown), the box shows it again.
    effect(() => {
      const value = this.value();
      untracked(() => {
        this.text.set(value);
        this.setInvalid(false);
      });
    });
  }

  // Enter takes what was written, unless an hour of the list is highlighted: then the list picks it
  protected enter(trigger: MatAutocompleteTrigger): void {
    if (trigger.activeOption) {
      return;
    }

    this.commit();
    trigger.closePanel();
  }

  // Takes the hour picked from the list
  protected pick(event: MatAutocompleteSelectedEvent): void {
    this.text.set(event.option.value);
    this.commit();
  }

  // Takes what was written, or shows the box's error when it is not a time
  protected commit(): void {
    const shown = this.accept()(this.text());
    this.setInvalid(shown === null);

    if (shown !== null) {
      this.text.set(shown);
    }
  }

  // Shows or hides the box's error
  private setInvalid(invalid: boolean): void {
    this.invalid = invalid;
    this.field()?.updateErrorState();
  }
}

/**
 * The hours shown in the list
 *
 * All the hours while the box shows its own time or is empty; otherwise those that start like what is written, with
 * or without the leading zero ("9" finds "09:00", "1" finds "10:00" to "19:00").
 */
function matching(hours: readonly string[], text: string, current: string): readonly string[] {
  const written = text.trim().toLowerCase();

  if (written === '' || text === current) {
    return hours;
  }

  return hours.filter((hour) => hour.startsWith(written) || hour.replace(/^0/, '').startsWith(written));
}
