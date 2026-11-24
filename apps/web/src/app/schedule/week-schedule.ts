import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  OnDestroy,
  OnInit,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { AuthService, LAST_PIQUETES_KEY } from '../auth/auth.service';
import {
  addDays,
  addMonths,
  firstOfMonth,
  isoWeek,
  isoWeekNumber,
  longDay,
  mondayOf,
  monthMondays,
  monthTitle,
  shortDay,
  todayInLisbon,
  weekDays,
  weekHeading,
  weekTitle,
} from './iso-week';
import { Piquete, ShiftFormTarget } from './piquete';
import { ShiftForm } from './shift-form';
import { PlacedSegment, ShiftSegment, placeSegments, shiftSegments } from './shift-time';

interface WeekDay {
  date: string;
  short: string;
  long: string;
  past: boolean;
  today: boolean;
  canAdd: boolean;
  /** The day has not passed and the user builds the schedule: its shifts can be changed. */
  canEdit: boolean;
  /** Shifts with a start and an end, placed on the hour grid. */
  shifts: PlacedSegment<Piquete>[];
  /** Shifts with no times yet, listed under the day's name. */
  untimed: Piquete[];
}

interface CalendarWeek {
  monday: string;
  number: number;
  label: string;
  selected: boolean;
  current: boolean;
  days: { date: string; day: number; outside: boolean; today: boolean }[];
}

/** The last week loaded, kept so the roster stays readable offline (RNF06); sign-out removes it. */
interface CachedWeek {
  week: string;
  piquetes: Piquete[];
}

/**
 * The week's schedule, Monday to Sunday, one column per day (SCRUM-65, mockup). Reads GET /api/v1/piquetes?week=.
 * The arrows move a week back or forward and the title opens a calendar to choose a week. Days before today, and so
 * every past week, are read-only here; the "+" in the other columns is a placeholder until adding a shift exists.
 */
@Component({
  selector: 'app-week-schedule',
  imports: [ShiftForm],
  templateUrl: './week-schedule.html',
  styleUrl: './week-schedule.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'closeOnOutsideClick($event)',
    '(document:keydown.escape)': 'closeOnEscape()',
  },
})
export class WeekSchedule implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  protected readonly auth = inject(AuthService);
  private request?: Subscription;

  private readonly picker = viewChild<ElementRef<HTMLElement>>('picker');
  private readonly pickerToggle = viewChild<ElementRef<HTMLButtonElement>>('pickerToggle');
  private readonly grid = viewChild<ElementRef<HTMLElement>>('grid');

  private readonly injector = inject(Injector);
  private scrolledToFirstHour = false;

  /** The hour the grid opens on (a labelled one), so the working day shows first; earlier hours are a scroll up. */
  private static readonly FIRST_VISIBLE_HOUR = 6;

  protected readonly today = todayInLisbon();
  protected readonly currentMonday = mondayOf(this.today);

  protected readonly monday = signal(this.currentMonday);
  protected readonly title = computed(() => weekHeading(this.monday()));
  /** The week's days, for screen readers: the title names the week by its number. */
  protected readonly titleDays = computed(() => weekTitle(this.monday()));
  protected readonly piquetes = signal<Piquete[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly offline = signal(false);

  /** Submitting (publishing) a week does not exist yet (RF46), so every week is still open to new shifts. */
  protected readonly submitted = signal(false);

  protected readonly canAddShifts = computed(() => this.auth.isSupervisor() && !this.submitted());

  /** The whole week is before today: nothing in it can change here. */
  protected readonly pastWeek = computed(() => addDays(this.monday(), 6) < this.today);

  /** The hour labels down the left side, every 2 hours: "00:00", "02:00" … "22:00". */
  protected readonly hours = Array.from({ length: 12 }, (_, index) => `${String(index * 2).padStart(2, '0')}:00`);

  protected readonly days = computed<WeekDay[]>(() => {
    const segments: (ShiftSegment & { item: Piquete })[] = this.piquetes().flatMap((piquete) =>
      piquete.startsAt && piquete.endsAt
        ? shiftSegments(piquete.startsAt, piquete.endsAt).map((segment) => ({ ...segment, item: piquete }))
        : [],
    );
    return weekDays(this.monday()).map((date) => ({
      date,
      short: shortDay(date),
      long: longDay(date),
      past: date < this.today,
      today: date === this.today,
      canAdd: this.canAddShifts() && date >= this.today,
      canEdit: this.canAddShifts() && date >= this.today,
      shifts: placeSegments(segments.filter((segment) => segment.date === date)),
      untimed: this.piquetes().filter(
        (piquete) => piquete.dutyDate === date && !(piquete.startsAt && piquete.endsAt),
      ),
    }));
  });

  /** The add or edit form while it is open. */
  protected readonly form = signal<ShiftFormTarget | null>(null);

  protected readonly pickerOpen = signal(false);
  protected readonly pickerMonth = signal(firstOfMonth(this.currentMonday));
  protected readonly pickerTitle = computed(() => monthTitle(this.pickerMonth()));
  protected readonly pickerWeeks = computed<CalendarWeek[]>(() => {
    const month = this.pickerMonth().slice(0, 7);
    return monthMondays(this.pickerMonth()).map((monday) => ({
      monday,
      number: isoWeekNumber(monday),
      label: `Week ${isoWeekNumber(monday)}, ${weekTitle(monday)}`,
      selected: monday === this.monday(),
      current: monday === this.currentMonday,
      days: weekDays(monday).map((date) => ({
        date,
        day: Number(date.slice(8)),
        outside: date.slice(0, 7) !== month,
        today: date === this.today,
      })),
    }));
  });

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.request?.unsubscribe();
  }

  protected addShift(date: string): void {
    this.form.set({ kind: 'add', date });
  }

  protected editShift(piquete: Piquete): void {
    this.form.set({ kind: 'edit', piquete });
  }

  /** A shift was added or changed: the week is read again, so it shows what the API kept. */
  protected onShiftSaved(): void {
    this.load();
  }

  protected onFormClosed(): void {
    this.form.set(null);
  }

  /** "Minimum 4 firefighters" for a shift with times; the slot count for a piquete from before. */
  protected crewLabel(piquete: Piquete): string {
    if (piquete.minimumCrew) {
      return `Minimum ${piquete.minimumCrew} ${piquete.minimumCrew === 1 ? 'firefighter' : 'firefighters'}`;
    }
    return `${piquete.slots.length} ${piquete.slots.length === 1 ? 'slot' : 'slots'}`;
  }

  protected previousWeek(): void {
    this.goTo(addDays(this.monday(), -7));
  }

  protected nextWeek(): void {
    this.goTo(addDays(this.monday(), 7));
  }

  protected togglePicker(): void {
    if (!this.pickerOpen()) {
      // Open on the month of the week's Thursday, the month the ISO week belongs to.
      this.pickerMonth.set(firstOfMonth(addDays(this.monday(), 3)));
    }
    this.pickerOpen.update((open) => !open);
  }

  protected previousMonth(): void {
    this.pickerMonth.update((first) => addMonths(first, -1));
  }

  protected nextMonth(): void {
    this.pickerMonth.update((first) => addMonths(first, 1));
  }

  protected chooseWeek(monday: string): void {
    this.closePicker();
    this.goTo(monday);
  }

  protected closeOnOutsideClick(event: Event): void {
    const picker = this.picker()?.nativeElement;
    if (this.pickerOpen() && picker && !event.composedPath().includes(picker)) {
      this.pickerOpen.set(false);
    }
  }

  protected closeOnEscape(): void {
    if (this.pickerOpen()) {
      this.closePicker();
    }
  }

  private closePicker(): void {
    this.pickerOpen.set(false);
    this.pickerToggle()?.nativeElement.focus();
  }

  /**
   * Once, after the first week has rendered (its heads may have grown), scrolls the grid so 06:00 sits just under the
   * day names. Later weeks keep the hour the user scrolled to.
   */
  private scrollToFirstHourOnce(): void {
    if (this.scrolledToFirstHour) {
      return;
    }
    this.scrolledToFirstHour = true;
    afterNextRender(
      () => {
        const grid = this.grid()?.nativeElement;
        const body = grid?.querySelector<HTMLElement>('.day-body');
        const head = grid?.querySelector<HTMLElement>('.day-head');
        if (grid && body && head) {
          const bodyTop = body.getBoundingClientRect().top - grid.getBoundingClientRect().top + grid.scrollTop;
          const hourTop = bodyTop + (body.offsetHeight * WeekSchedule.FIRST_VISIBLE_HOUR) / 24;
          grid.scrollTop = hourTop - head.offsetHeight;
        }
      },
      { injector: this.injector },
    );
  }

  private goTo(monday: string): void {
    if (monday !== this.monday()) {
      this.monday.set(monday);
      this.piquetes.set([]);
      this.load();
    }
  }

  private load(): void {
    const week = isoWeek(this.monday());
    // A newer week replaces a request still on its way, so a slow answer never fills the wrong week.
    this.request?.unsubscribe();
    this.request = this.http
      .get<Piquete[]>('/api/v1/piquetes', { params: new HttpParams().set('week', week) })
      .subscribe({
        next: (list) => {
          this.error.set(null);
          this.offline.set(false);
          this.piquetes.set(list);
          writeCache({ week, piquetes: list });
          this.scrollToFirstHourOnce();
        },
        error: () => {
          if (!this.auth.session()) {
            // 401: the interceptor signed out; the shell shows the sign-in screen, with no roster.
            this.piquetes.set([]);
            return;
          }
          const cached = readCache();
          if (cached?.week === week) {
            this.error.set(null);
            this.offline.set(true);
            this.piquetes.set(cached.piquetes);
            return;
          }
          this.offline.set(false);
          this.error.set("Could not load the week's schedule.");
        },
      });
  }
}

function writeCache(cached: CachedWeek): void {
  try {
    localStorage.setItem(LAST_PIQUETES_KEY, JSON.stringify(cached));
  } catch {
    // Storage full or blocked: the roster still shows, it just is not kept for offline use.
  }
}

function readCache(): CachedWeek | null {
  try {
    const parsed = JSON.parse(localStorage.getItem(LAST_PIQUETES_KEY) ?? 'null') as Partial<CachedWeek> | null;
    return parsed && typeof parsed.week === 'string' && Array.isArray(parsed.piquetes)
      ? { week: parsed.week, piquetes: parsed.piquetes }
      : null;
  } catch {
    return null;
  }
}
