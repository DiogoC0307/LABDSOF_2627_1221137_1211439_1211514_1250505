import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService, LAST_PIQUETES_KEY } from '../auth/auth.service';

export type PresenceDecision = 'COMING' | 'CANNOT_COME';

interface Slot {
  id: string;
  role: string;
  status: string;
  presenceStatus?: 'UNCONFIRMED' | 'COMING' | 'CANNOT_COME';
}

interface Piquete {
  id: string;
  dutyDate: string;
  slots: Slot[];
}

interface PresenceResponse {
  slotId: string;
  presenceStatus: 'UNCONFIRMED' | 'COMING' | 'CANNOT_COME';
  slotStatus: string;
  slotFreed: boolean;
}

/** Today's piquete, shift confirmation and the list; the schedule editor only for the supervisor (SCRUM-46, SCRUM-63, SCRUM-70). */
@Component({
  selector: 'app-schedule',
  imports: [FormsModule],
  templateUrl: './schedule.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class Schedule implements OnInit {
  private readonly http = inject(HttpClient);
  protected readonly auth = inject(AuthService);

  protected readonly piquetes = signal<Piquete[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly offline = signal(false);
  /** True while the piquete is being created: the button is disabled, so a second click sends nothing (SCRUM-158). */
  protected readonly creating = signal(false);
  protected readonly updatingSlotId = signal<string | null>(null);
  protected dutyDate = new Date().toISOString().slice(0, 10);

  protected readonly today = computed(() => new Date().toISOString().slice(0, 10));
  protected readonly todayPiquete = computed(
    () => this.piquetes().find((piquete) => piquete.dutyDate === this.today()) ?? null,
  );

  ngOnInit(): void {
    this.reload();
  }

  protected create(): void {
    if (this.creating()) {
      return;
    }
    if (this.dutyDate === '') {
      this.error.set('Fill in the date.');
      return;
    }
    this.error.set(null);
    this.creating.set(true);
    this.http
      .post<Piquete>('/api/v1/piquetes', { dutyDate: this.dutyDate })
      .pipe(finalize(() => this.creating.set(false)))
      .subscribe({
        next: (created) => {
          // The form is cleared, as "When I can't" does: a click that comes after the answer finds no date and
          // sends nothing, so quick clicks create one piquete even when the API answers fast (SCRUM-158).
          this.dutyDate = '';
          this.reload();
          this.pollUntilEligible(created.id);
        },
        error: () => this.error.set('Could not create the piquete.'),
      });
  }

  protected confirmPresence(slot: Slot, decision: PresenceDecision): void {
    if (this.updatingSlotId() === slot.id) {
      return;
    }
    this.error.set(null);
    this.updatingSlotId.set(slot.id);
    this.http
      .post<PresenceResponse>(`/api/v1/slots/${slot.id}/presence`, { decision })
      .pipe(finalize(() => this.updatingSlotId.set(null)))
      .subscribe({
        next: (res) => {
          this.piquetes.update((list) =>
            list.map((piquete) => ({
              ...piquete,
              slots: piquete.slots.map((item) =>
                item.id === slot.id
                  ? { ...item, presenceStatus: res.presenceStatus, status: res.slotStatus }
                  : item,
              ),
            })),
          );
        },
        error: () => this.error.set('Could not update shift confirmation.'),
      });
  }

  private reload(): void {
    this.http.get<Piquete[]>('/api/v1/piquetes').subscribe({
      next: (list) => {
        this.offline.set(false);
        this.piquetes.set(list);
        localStorage.setItem(LAST_PIQUETES_KEY, JSON.stringify(list));
      },
      error: () => {
        if (!this.auth.session()) {
          // 401: the interceptor signed out; the shell shows the sign-in screen, with no roster.
          this.piquetes.set([]);
          return;
        }
        const cached = this.readCachedPiquetes();
        if (cached !== null) {
          this.offline.set(true);
          this.piquetes.set(cached);
          this.error.set(null);
          return;
        }
        this.offline.set(false);
        this.error.set('Could not load piquetes.');
      },
    });
  }

  private pollUntilEligible(piqueteId: string, attempt = 0): void {
    if (attempt > 15) {
      return;
    }
    window.setTimeout(() => {
      this.http.get<Piquete[]>('/api/v1/piquetes').subscribe({
        next: (list) => {
          this.piquetes.set(list);
          const piquete = list.find((item) => item.id === piqueteId);
          const eligible = piquete?.slots.some((slot) => slot.status === 'ELIGIBLE');
          if (!eligible) {
            this.pollUntilEligible(piqueteId, attempt + 1);
          }
        },
        error: () => this.error.set('Could not load piquetes.'),
      });
    }, 1000);
  }

  private readCachedPiquetes(): Piquete[] | null {
    const raw = localStorage.getItem(LAST_PIQUETES_KEY);
    if (!raw) {
      return null;
    }
    try {
      const parsed = JSON.parse(raw) as unknown;
      return Array.isArray(parsed) ? (parsed as Piquete[]) : null;
    } catch {
      return null;
    }
  }
}
