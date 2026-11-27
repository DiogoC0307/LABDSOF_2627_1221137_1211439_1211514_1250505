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
  untracked,
  viewChild,
} from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { longDay } from './iso-week';
import { ScheduleDraftService } from './schedule-draft.service';
import { DraftShift } from './schedule-model';
import { MINUTES_IN_DAY, endAfter, parseTime, rangeLabel, timeLabel } from './schedule-rules';
import { TimeField, WHOLE_HOURS, endHours, endLabel } from './time-field';

/**
 * A selected shift side panel component with its date and time, which can be changed, and Delete
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
  private readonly injector = inject(Injector);

  readonly monday = input.required<string>();
  readonly shift = input.required<DraftShift>();
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

  // An end at or before the start is on the next day: 20:00 to 08:00 is a night shift.
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

  constructor() {
    // A newly selected shift takes the focus, so the panel is read from its top.
    const shiftId = computed(() => this.shift().id);
    effect(() => {
      shiftId();
      untracked(() => {
        afterNextRender(() => this.heading()?.nativeElement.focus(), { injector: this.injector });
      });
    });
  }
}
