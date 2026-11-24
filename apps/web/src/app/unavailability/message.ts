import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, output, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

/** A period in the ExtracaoConcluida v2 format: dates inclusive, optional weekdays and Lisbon times (06). */
interface ProposedPeriod {
  startDate: string;
  endDate: string;
  weekdays?: string[];
  startTime?: string;
  endTime?: string;
}

interface MessageView {
  id: string;
  status: 'PENDING' | 'PROPOSED' | 'CONFIRMED' | 'DISCARDED' | 'EXPIRED';
  referenceDate: string;
  method: string | null;
  periods: ProposedPeriod[];
}

/** A period as the member edits it: empty strings for no time, an empty list for every day. */
interface EditablePeriod {
  startDate: string;
  endDate: string;
  weekdays: string[];
  startTime: string;
  endTime: string;
}

interface InvalidField {
  field: string;
  code: string;
}

type State = 'idle' | 'reading' | 'proposed' | 'nothing' | 'late';

export const WEEKDAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

const MESSAGES = '/api/v1/unavailability-messages';
const POLL_MS = 1000;
/**
 * How long the member waits for the reading, counted here from the moment they send (SCRUM-89 AC3): 10 s, as on page
 * 06 (Responsible AI, the latency target and fallback). After it the form is the way and the message is discarded.
 */
const READ_DEADLINE_MS = 10_000;

const ERRORS: Record<string, string> = {
  required: 'Fill in this field.',
  too_long: 'At most 500 characters.',
  ends_before_start: 'The end is before the start.',
  in_the_past: 'This period has already ended.',
  too_far_ahead: 'More than 6 months ahead.',
  no_days: 'No day in these dates matches the weekdays.',
  invalid: 'Not valid.',
  too_many: 'At most 10 periods.',
};

/**
 * "When I can't", in my own words (SCRUM-75): the member writes, sees what was read in editable fields labelled
 * "Read automatically", and nothing counts until they confirm. The pattern is shown and sent as it is; escala-api
 * expands it only when it is saved. The text stays in the box, in this component's memory only (never in browser
 * storage), until a proposal arrives, so that the member can copy it into the form when nothing usable comes back
 * (SCRUM-89). With AI off (GET settings), the section is not shown at all and only the form is left.
 */
@Component({
  selector: 'app-unavailability-message',
  imports: [FormsModule],
  templateUrl: './message.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class UnavailabilityMessage implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);

  /** Emitted after the member confirms, so the list of periods is loaded again. */
  readonly confirmed = output<void>();

  protected readonly weekdays = WEEKDAYS;
  /** False until escala-api says AI is on: offline, or with AI off, only the form is shown (SCRUM-89 AC2). */
  protected readonly aiEnabled = signal(false);
  protected readonly state = signal<State>('idle');
  protected readonly periods = signal<EditablePeriod[]>([]);
  protected readonly errors = signal<InvalidField[]>([]);
  protected readonly sendError = signal<string | null>(null);
  /** True while the text, or the confirmation, is in flight: its button is disabled (SCRUM-158). */
  protected readonly sending = signal(false);
  protected readonly confirming = signal(false);

  protected text = '';

  private messageId: string | null = null;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private deadline: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.http.get<{ aiEnabled: boolean }>(`${MESSAGES}/settings`).subscribe({
      next: (settings) => this.aiEnabled.set(settings.aiEnabled === true),
      error: () => this.aiEnabled.set(false),
    });
  }

  ngOnDestroy(): void {
    this.stopTimers();
  }

  protected send(): void {
    if (this.sending()) {
      return;
    }
    if (this.text.trim() === '') {
      this.sendError.set(ERRORS['required']);
      return;
    }
    this.sendError.set(null);
    this.errors.set([]);
    this.sending.set(true);
    const sentAt = Date.now();
    this.http
      .post<MessageView>(MESSAGES, { text: this.text })
      .pipe(finalize(() => this.sending.set(false)))
      .subscribe({
        next: (message) => {
          // A text sent while another is being read replaces it: the older one's poll and deadline stop here.
          this.stopTimers();
          this.messageId = message.id;
          this.state.set('reading');
          this.deadline = setTimeout(() => this.tooLate(), Math.max(0, sentAt + READ_DEADLINE_MS - Date.now()));
          this.poll(message.id);
        },
        error: (error: unknown) => {
          if (this.isAiOff(error)) {
            this.aiEnabled.set(false);
            return;
          }
          this.sendError.set(this.sendProblem(error));
        },
      });
  }

  protected hasDay(period: EditablePeriod, day: string): boolean {
    return period.weekdays.includes(day);
  }

  protected toggleDay(period: EditablePeriod, day: string): void {
    period.weekdays = this.hasDay(period, day)
      ? period.weekdays.filter((other) => other !== day)
      : WEEKDAYS.filter((other) => other === day || period.weekdays.includes(other));
  }

  protected message(error: InvalidField): string {
    return ERRORS[error.code] ?? 'Not valid.';
  }

  protected confirm(): void {
    if (this.confirming()) {
      return;
    }
    const local = this.localErrors();
    this.errors.set(local);
    if (local.length > 0 || this.messageId === null) {
      return;
    }
    const periods = this.periods().map(toProposed);
    this.confirming.set(true);
    this.http
      .post(`${MESSAGES}/${this.messageId}/confirm`, { periods })
      .pipe(finalize(() => this.confirming.set(false)))
      .subscribe({
        next: () => {
          this.reset();
          this.confirmed.emit();
        },
        error: (error: unknown) => this.errors.set(this.apiErrors(error)),
      });
  }

  protected discard(): void {
    if (this.messageId === null) {
      return;
    }
    this.http.delete(`${MESSAGES}/${this.messageId}`).subscribe({
      next: () => this.reset(),
      error: () => this.errors.set([{ field: 'message', code: 'invalid' }]),
    });
  }

  /** Asks once a second until there is an answer or the deadline passes; an answer for an older message is ignored. */
  private poll(id: string): void {
    this.http.get<MessageView>(`${MESSAGES}/${id}`).subscribe({
      next: (message) => {
        if (id !== this.messageId) {
          return;
        }
        if (message.status === 'PENDING') {
          this.timer = setTimeout(() => this.poll(id), POLL_MS);
          return;
        }
        this.stopTimers();
        if (message.status === 'PROPOSED' && message.periods.length > 0) {
          this.text = '';
          this.periods.set(message.periods.map(toEditable));
          this.state.set('proposed');
          return;
        }
        // No dates in the text (SCRUM-89 AC1): its copy on the server is deleted now; the box keeps it for the form.
        this.state.set('nothing');
        this.forget(id);
      },
      error: () => {
        if (id === this.messageId) {
          this.timer = setTimeout(() => this.poll(id), POLL_MS);
        }
      },
    });
  }

  /** No answer within READ_DEADLINE_MS of sending (SCRUM-89 AC3): stop asking, then discard, so a late result is unused. */
  private tooLate(): void {
    this.deadline = null;
    const id = this.messageId;
    if (id === null) {
      return;
    }
    this.stopTimers();
    this.state.set('late');
    this.forget(id);
  }

  /** Deletes the message and its free text on the server; from now on its answers are ignored here. */
  private forget(id: string): void {
    this.messageId = null;
    this.http.delete(`${MESSAGES}/${id}`).subscribe({ error: () => undefined });
  }

  private localErrors(): InvalidField[] {
    const errors: InvalidField[] = [];
    this.periods().forEach((period, i) => {
      if (period.startDate === '' || period.endDate === '') {
        errors.push({ field: `periods[${i}].startDate`, code: 'required' });
      } else if (period.endDate < period.startDate) {
        errors.push({ field: `periods[${i}].endDate`, code: 'ends_before_start' });
      }
      if ((period.startTime === '') !== (period.endTime === '')) {
        errors.push({ field: `periods[${i}].endTime`, code: 'required' });
      }
    });
    return errors;
  }

  private reset(): void {
    this.stopTimers();
    this.messageId = null;
    this.text = '';
    this.periods.set([]);
    this.errors.set([]);
    this.state.set('idle');
  }

  private stopTimers(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
    if (this.deadline !== null) {
      clearTimeout(this.deadline);
      this.deadline = null;
    }
  }

  /** 409 ai_off: AI was turned off after the page was opened (SCRUM-89 AC2). */
  private isAiOff(error: unknown): boolean {
    return error instanceof HttpErrorResponse && error.status === 409
      && this.apiErrors(error).some((field) => field.code === 'ai_off');
  }

  private sendProblem(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 429) {
      return 'You sent 10 messages in the last hour. Try again later, or use the form above.';
    }
    const errors = this.apiErrors(error);
    return errors.length > 0 && errors[0].code !== 'not_sent' ? this.message(errors[0]) : 'Not sent. Try again.';
  }

  private apiErrors(error: unknown): InvalidField[] {
    const body = error instanceof HttpErrorResponse ? (error.error as { errors?: InvalidField[] } | null) : null;
    return body?.errors && body.errors.length > 0 ? body.errors : [{ field: 'message', code: 'not_sent' }];
  }
}

function toEditable(period: ProposedPeriod): EditablePeriod {
  return {
    startDate: period.startDate,
    endDate: period.endDate,
    weekdays: period.weekdays ?? [],
    startTime: period.startTime ?? '',
    endTime: period.endTime ?? '',
  };
}

function toProposed(period: EditablePeriod): ProposedPeriod {
  const proposed: ProposedPeriod = { startDate: period.startDate, endDate: period.endDate };
  if (period.weekdays.length > 0) {
    proposed.weekdays = period.weekdays;
  }
  if (period.startTime !== '' && period.endTime !== '') {
    proposed.startTime = period.startTime;
    proposed.endTime = period.endTime;
  }
  return proposed;
}
