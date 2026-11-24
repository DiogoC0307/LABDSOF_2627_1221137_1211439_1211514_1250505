import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService, LAST_PIQUETES_KEY } from '../auth/auth.service';
import { formatLisbonClock } from '../unavailability/lisbon-time';

interface Slot {
  id: string;
  role: string;
  status: string;
}

interface Piquete {
  id: string;
  dutyDate: string;
  slots: Slot[];
}

interface TodayRisk {
  dico: string;
  rcm: number | null;
  fetchedAt: string | null;
  dataAgeHours: number | null;
  source: string;
  assumedHigher: boolean;
}

const RISK_LABELS: Record<number, string> = {
  1: 'low',
  2: 'moderate',
  3: 'high',
  4: 'very high',
  5: 'maximum',
};

/** Today's piquete and the list; the schedule editor only for the supervisor (SCRUM-46, SCRUM-63). */
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
  protected readonly risk = signal<TodayRisk | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly offline = signal(false);
  /** True while the piquete is being created: the button is disabled, so a second click sends nothing (SCRUM-158). */
  protected readonly creating = signal(false);
  protected dutyDate = new Date().toISOString().slice(0, 10);

  protected readonly today = computed(() => new Date().toISOString().slice(0, 10));
  protected readonly todayPiquete = computed(
    () => this.piquetes().find((piquete) => piquete.dutyDate === this.today()) ?? null,
  );

  /** "Fire risk: 4 (very high) - updated hh:mm - Source: IPMA" (SCRUM-76). */
  protected readonly riskLine = computed(() => {
    const risk = this.risk();
    if (!this.auth.isSupervisor() || risk?.rcm == null || !risk.fetchedAt) {
      return null;
    }
    const label = RISK_LABELS[risk.rcm] ?? String(risk.rcm);
    return `Fire risk: ${risk.rcm} (${label}) - updated ${formatLisbonClock(risk.fetchedAt)} - Source: IPMA`;
  });

  protected readonly riskAge = computed(() => {
    const hours = this.risk()?.dataAgeHours;
    if (hours == null) {
      return null;
    }
    return `${Math.floor(hours)} h`;
  });

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

  private reload(): void {
    this.http.get<Piquete[]>('/api/v1/piquetes').subscribe({
      next: (list) => {
        this.offline.set(false);
        this.piquetes.set(list);
        localStorage.setItem(LAST_PIQUETES_KEY, JSON.stringify(list));
        this.loadRisk();
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

  /** Supervisor only. A failure leaves the roster on screen (ADR 0013: risk does not block the schedule). */
  private loadRisk(): void {
    if (!this.auth.isSupervisor()) {
      return;
    }
    this.http.get<TodayRisk>('/api/v1/risk/today').subscribe({
      next: (risk) => this.risk.set(risk),
      error: () => this.risk.set(null),
    });
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
