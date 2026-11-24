import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { formatLisbon, lisbonToUtc, nextDay } from './lisbon-time';
import { UnavailabilityMessage } from './message';

interface Period {
  id: string;
  startsAt: string;
  endsAt: string;
  source: string;
}

interface InvalidField {
  field: string;
  code: string;
}

const MESSAGES: Record<string, string> = {
  required: 'Fill in the dates.',
  ends_before_start: 'The end is before the start.',
  in_the_past: 'This period has already ended.',
  too_far_ahead: 'The start is more than 6 months ahead.',
  not_saved: 'The period was not saved. Try again.',
};

/**
 * "When I can't": the member's own periods and the form (SCRUM-64). Dates and times only, never a reason (09).
 * A day without a time runs from 00:00 to 00:00 of the next day, in Lisbon; the API stores UTC.
 */
@Component({
  selector: 'app-unavailability',
  imports: [FormsModule, UnavailabilityMessage],
  templateUrl: './unavailability.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class Unavailability implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly periods = signal<Period[]>([]);
  protected readonly errors = signal<InvalidField[]>([]);
  protected readonly loadError = signal<string | null>(null);

  protected startDate = '';
  protected startTime = '';
  protected endDate = '';
  protected endTime = '';

  protected readonly format = formatLisbon;

  ngOnInit(): void {
    this.reload();
  }

  protected message(error: InvalidField): string {
    return MESSAGES[error.code] ?? 'Not valid.';
  }

  protected save(): void {
    if (this.startDate === '' || this.endDate === '') {
      this.errors.set([{ field: 'startsAt', code: 'required' }]);
      return;
    }
    const startsAt = lisbonToUtc(this.startDate, this.startTime);
    const endsAt = this.endTime === '' ? lisbonToUtc(nextDay(this.endDate), '') : lisbonToUtc(this.endDate, this.endTime);
    if (endsAt <= startsAt) {
      this.errors.set([{ field: 'endsAt', code: 'ends_before_start' }]);
      return;
    }
    this.errors.set([]);
    this.http.post<Period>('/api/v1/unavailabilities', { startsAt, endsAt }).subscribe({
      next: () => {
        this.startDate = '';
        this.startTime = '';
        this.endDate = '';
        this.endTime = '';
        this.reload();
      },
      error: (error: unknown) => this.errors.set(this.apiErrors(error)),
    });
  }

  protected remove(period: Period): void {
    this.http.delete(`/api/v1/unavailabilities/${period.id}`).subscribe({
      next: () => this.reload(),
      error: () => this.loadError.set('Could not remove the period.'),
    });
  }

  protected reload(): void {
    this.http.get<Period[]>('/api/v1/unavailabilities').subscribe({
      next: (list) => {
        this.loadError.set(null);
        this.periods.set(list);
      },
      error: () => this.loadError.set('Could not load your periods.'),
    });
  }

  private apiErrors(error: unknown): InvalidField[] {
    const body = error instanceof HttpErrorResponse ? (error.error as { errors?: InvalidField[] } | null) : null;
    return body?.errors && body.errors.length > 0 ? body.errors : [{ field: 'period', code: 'not_saved' }];
  }
}
