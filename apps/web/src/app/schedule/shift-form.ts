import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  afterNextRender,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { addDays, longDay } from './iso-week';
import { Piquete, ShiftFormTarget, ShiftPeriod, ShiftRequest } from './piquete';
import { lisbonInstant, lisbonTime } from './shift-time';

type Field = 'dutyDate' | 'period' | 'startsAt' | 'endsAt' | 'minimumCrew';

/** The hours a period starts with when the supervisor picks it; they can be changed. */
const PERIOD_HOURS: Record<ShiftPeriod, { start: string; end: string }> = {
  DAY: { start: '08:00', end: '20:00' },
  NIGHT: { start: '20:00', end: '08:00' },
};

/** What the API's codes mean to the supervisor (contracts/openapi.yaml, POST and PUT /api/v1/piquetes). */
const MESSAGES: Record<string, string> = {
  'dutyDate.required': 'Choose a day.',
  'dutyDate.in_the_past': 'Choose today or a later day.',
  'period.required': 'Choose day or night.',
  'startsAt.required': 'Enter the start time.',
  'startsAt.not_on_duty_date': 'The shift must start on the chosen day.',
  'endsAt.required': 'Enter the end time.',
  'endsAt.ends_before_start': 'The shift must end after it starts.',
  'endsAt.too_long': 'A shift lasts at most 24 hours.',
  'minimumCrew.required': 'Enter how many firefighters the shift needs.',
  'minimumCrew.too_small': 'A shift needs at least 1 firefighter.',
  'minimumCrew.too_large': 'A shift needs at most 50 firefighters.',
};

/**
 * Adds a shift on a day, or changes one (SCRUM-65, D1): day, period, start, end and minimum crew. A native modal
 * dialog: the browser keeps focus inside, closes it on Esc and gives focus back to the button that opened it.
 * An end at or before the start ends the next day, as a night shift does.
 */
@Component({
  selector: 'app-shift-form',
  imports: [FormsModule],
  templateUrl: './shift-form.html',
  styleUrl: './shift-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShiftForm implements OnInit {
  private readonly http = inject(HttpClient);

  readonly target = input.required<ShiftFormTarget>();
  /** Today in Lisbon: the earliest day a shift can be on. */
  readonly today = input.required<string>();

  /** The shift was saved; the week reloads. */
  readonly saved = output<Piquete>();
  /** The dialog closed, saved or not. */
  readonly closed = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  protected readonly editing = computed(() => this.target().kind === 'edit');
  protected readonly heading = computed(() => (this.editing() ? 'Edit shift' : 'Add a shift'));

  protected dutyDate = '';
  protected period: ShiftPeriod = 'DAY';
  protected start = PERIOD_HOURS.DAY.start;
  protected end = PERIOD_HOURS.DAY.end;
  protected minimumCrew: number | null = 1;

  protected readonly errors = signal<Partial<Record<Field, string>>>({});
  protected readonly failure = signal<string | null>(null);
  protected readonly saving = signal(false);

  constructor() {
    afterNextRender(() => {
      const dialog = this.dialog().nativeElement;
      // jsdom, in the unit tests, has no showModal(); every browser the PWA supports has it.
      if (typeof dialog.showModal === 'function') {
        dialog.showModal();
      } else {
        dialog.setAttribute('open', '');
      }
    });
  }

  ngOnInit(): void {
    const target = this.target();
    if (target.kind === 'add') {
      this.dutyDate = target.date;
      return;
    }
    const piquete = target.piquete;
    this.dutyDate = piquete.dutyDate;
    this.period = piquete.period ?? 'DAY';
    this.start = piquete.startsAt ? lisbonTime(piquete.startsAt).time : PERIOD_HOURS[this.period].start;
    this.end = piquete.endsAt ? lisbonTime(piquete.endsAt).time : PERIOD_HOURS[this.period].end;
    this.minimumCrew = piquete.minimumCrew ?? 1;
  }

  /** The day in words under the heading, e.g. "Tuesday 24 November". */
  protected dayInWords(): string {
    return /^\d{4}-\d{2}-\d{2}$/.test(this.dutyDate) ? longDay(this.dutyDate) : '';
  }

  /** An end at or before the start is the next morning. */
  protected endsNextDay(): boolean {
    return Boolean(this.start && this.end && this.end <= this.start);
  }

  protected choosePeriod(period: ShiftPeriod): void {
    this.period = period;
    this.start = PERIOD_HOURS[period].start;
    this.end = PERIOD_HOURS[period].end;
  }

  protected save(): void {
    this.failure.set(null);
    const missing = this.missingFields();
    this.errors.set(missing);
    if (Object.keys(missing).length > 0 || this.saving()) {
      return;
    }
    const body: ShiftRequest = {
      dutyDate: this.dutyDate,
      period: this.period,
      startsAt: lisbonInstant(this.dutyDate, this.start),
      endsAt: lisbonInstant(this.endsNextDay() ? addDays(this.dutyDate, 1) : this.dutyDate, this.end),
      minimumCrew: Number(this.minimumCrew),
    };
    const target = this.target();
    const request =
      target.kind === 'edit'
        ? this.http.put<Piquete>(`/api/v1/piquetes/${target.piquete.id}`, body)
        : this.http.post<Piquete>('/api/v1/piquetes', body);
    this.saving.set(true);
    request.subscribe({
      next: (piquete) => {
        this.saving.set(false);
        this.saved.emit(piquete);
        this.close();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.showFailure(error);
      },
    });
  }

  protected close(): void {
    const dialog = this.dialog().nativeElement;
    if (dialog.open && typeof dialog.close === 'function') {
      dialog.close(); // fires "close", which tells the week
    } else {
      this.closed.emit();
    }
  }

  /** The dialog closed: with Cancel, Esc, the back gesture or after saving. */
  protected onClose(): void {
    this.closed.emit();
  }

  private missingFields(): Partial<Record<Field, string>> {
    const errors: Partial<Record<Field, string>> = {};
    if (!this.dutyDate) {
      errors.dutyDate = MESSAGES['dutyDate.required'];
    } else if (this.dutyDate < this.today()) {
      errors.dutyDate = MESSAGES['dutyDate.in_the_past'];
    }
    if (!this.start) {
      errors.startsAt = MESSAGES['startsAt.required'];
    }
    if (!this.end) {
      errors.endsAt = MESSAGES['endsAt.required'];
    }
    const crew = Number(this.minimumCrew);
    if (this.minimumCrew === null || String(this.minimumCrew) === '') {
      errors.minimumCrew = MESSAGES['minimumCrew.required'];
    } else if (!Number.isInteger(crew) || crew < 1) {
      errors.minimumCrew = MESSAGES['minimumCrew.too_small'];
    } else if (crew > 50) {
      errors.minimumCrew = MESSAGES['minimumCrew.too_large'];
    }
    return errors;
  }

  private showFailure(error: HttpErrorResponse): void {
    const fields = (error.error as { errors?: { field: string; code: string }[] } | null)?.errors;
    if (error.status === 400 && Array.isArray(fields) && fields.length > 0) {
      const errors: Partial<Record<Field, string>> = {};
      for (const { field, code } of fields) {
        errors[field as Field] = MESSAGES[`${field}.${code}`] ?? 'Check this field.';
      }
      this.errors.set(errors);
      return;
    }
    if (error.status === 409) {
      this.failure.set("This shift's day has passed, so it can't be changed.");
      return;
    }
    if (error.status === 404) {
      this.failure.set('This shift no longer exists. Close the form to see the week as it is now.');
      return;
    }
    this.failure.set('Could not save the shift. Try again.');
  }
}
