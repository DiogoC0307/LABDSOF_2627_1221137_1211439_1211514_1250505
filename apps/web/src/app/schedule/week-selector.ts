import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { CdkTrapFocus } from '@angular/cdk/a11y';
import { CdkConnectedOverlay, CdkOverlayOrigin, ConnectedPosition } from '@angular/cdk/overlay';
import { addDays, addMonths, firstOfMonth, monthMondays, monthTitle, weekDays } from './iso-week';
import { relativeWeekLabel, weekRange } from './week-label';

// A row of the calendar: one week, chosen by pressing anywhere on it
interface CalendarWeek {
  monday: string;
  label: string;
  selected: boolean;
  days: { date: string; day: number; outside: boolean; today: boolean }[];
}

/**
 * Week selector: ‹ "This week / 23 – 29 November 2026" ›
 *
 * The arrows move a week back or forward and the date opens a calendar to choose a week.
 *
 * The week itself stays with the page: it comes in as `monday` and a change goes out as `weekChange`.
 */
@Component({
  selector: 'app-week-selector',
  imports: [CdkConnectedOverlay, CdkOverlayOrigin, CdkTrapFocus],
  templateUrl: './week-selector.html',
  styleUrl: './week-selector.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeekSelector {
  private readonly injector = inject(Injector);

  readonly monday = input.required<string>(); // The Monday of the week shown
  readonly currentMonday = input.required<string>(); // The Monday of the week that holds today
  readonly today = input.required<string>();

  readonly weekChange = output<string>();

  private readonly calendar = viewChild<ElementRef<HTMLElement>>('calendar');
  private readonly pickerToggle = viewChild<ElementRef<HTMLButtonElement>>('pickerToggle');

  // Labels of the bar
  protected readonly isCurrentWeek = computed(() => this.monday() === this.currentMonday());
  // "This week", "Next week", "In 2 weeks" over the dates
  protected readonly relativeLabel = computed(() => relativeWeekLabel(this.monday(), this.currentMonday()));
  protected readonly range = computed(() => weekRange(this.monday())); // "23 – 29 November 2026"
  protected readonly toggleLabel = computed(() => `${this.relativeLabel()}, ${this.range()}. Choose week`);

  // Calendar
  protected readonly pickerOpen = signal(false);
  // The calendar opens 8px under the bar, centred on it
  protected readonly pickerPositions: ConnectedPosition[] = [
    { originX: 'center', originY: 'bottom', overlayX: 'center', overlayY: 'top', offsetY: 8 },
  ];
  protected readonly pickerMonth = signal('');
  protected readonly pickerTitle = computed(() => (this.pickerMonth() ? monthTitle(this.pickerMonth()) : ''));

  // The weeks of the calendar's month, Monday to Sunday
  protected readonly pickerWeeks = computed<CalendarWeek[]>(() => {
    if (!this.pickerMonth()) {
      return [];
    }

    const month = this.pickerMonth().slice(0, 7);

    return monthMondays(this.pickerMonth()).map((monday) => ({
      monday,
      label: `Week of ${weekRange(monday)}`,
      selected: monday === this.monday(),
      days: weekDays(monday).map((date) => ({
        date,
        day: Number(date.slice(8)),
        outside: date.slice(0, 7) !== month,
        today: date === this.today(),
      })),
    }));
  });

  // Week arrows
  protected previousWeek(): void {
    this.weekChange.emit(addDays(this.monday(), -7));
  }

  protected nextWeek(): void {
    this.weekChange.emit(addDays(this.monday(), 7));
  }

  // Opens or closes the calendar
  protected togglePicker(): void {
    if (this.pickerOpen()) {
      this.closePicker();
      return;
    }

    // Open on the month of the week's Thursday: a week across two months opens on the one with most of its days.
    this.pickerMonth.set(firstOfMonth(addDays(this.monday(), 3)));
    this.pickerOpen.set(true);
  }

  // Calendar month arrows
  protected previousMonth(): void {
    this.pickerMonth.update((first) => addMonths(first, -1));
  }

  protected nextMonth(): void {
    this.pickerMonth.update((first) => addMonths(first, 1));
  }

  // A week chosen in the calendar
  protected chooseWeek(monday: string): void {
    this.closePicker();
    this.weekChange.emit(monday);
  }

  // Escape closes the calendar
  protected onPickerKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.closePicker();
    }
  }

  // On opening, focus goes to the chosen week's row, or to the first row when that week is in another month
  protected focusChosenWeek(): void {
    afterNextRender(
      () => {
        const calendar = this.calendar()?.nativeElement;
        const row =
          calendar?.querySelector<HTMLElement>('.calendar-week[aria-pressed="true"]') ??
          calendar?.querySelector<HTMLElement>('.calendar-week');
        row?.focus();
      },
      { injector: this.injector },
    );
  }

  // Closes the calendar (a week chosen, Escape, a click outside it) and gives the focus back to the date
  protected closePicker(): void {
    if (!this.pickerOpen()) {
      return;
    }

    this.pickerOpen.set(false);
    this.pickerToggle()?.nativeElement.focus();
  }
}
