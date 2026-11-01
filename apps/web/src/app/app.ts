import {
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

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

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
}

const LAST_PIQUETES_KEY = 'escala.lastPiquetes';

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './app.scss',
})
export class App implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private deferredInstall: BeforeInstallPromptEvent | null = null;
  private readonly onBeforeInstall = (event: Event) => this.captureInstallPrompt(event);

  protected readonly piquetes = signal<Piquete[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly offline = signal(false);
  protected readonly canInstall = signal(false);
  protected dutyDate = new Date().toISOString().slice(0, 10);

  protected readonly today = computed(() => new Date().toISOString().slice(0, 10));
  protected readonly todayPiquete = computed(
    () => this.piquetes().find((piquete) => piquete.dutyDate === this.today()) ?? null,
  );

  ngOnInit(): void {
    window.addEventListener('beforeinstallprompt', this.onBeforeInstall);
    this.reload();
  }

  ngOnDestroy(): void {
    window.removeEventListener('beforeinstallprompt', this.onBeforeInstall);
  }

  protected create(): void {
    this.error.set(null);
    this.http.post<Piquete>('/api/v1/piquetes', { dutyDate: this.dutyDate }).subscribe({
      next: (created) => {
        this.reload();
        this.pollUntilEligible(created.id);
      },
      error: () => this.error.set('Could not create the piquete.'),
    });
  }

  protected async install(): Promise<void> {
    if (!this.deferredInstall) {
      return;
    }
    await this.deferredInstall.prompt();
    this.deferredInstall = null;
    this.canInstall.set(false);
  }

  private reload(): void {
    this.http.get<Piquete[]>('/api/v1/piquetes').subscribe({
      next: (list) => {
        this.offline.set(false);
        this.piquetes.set(list);
        localStorage.setItem(LAST_PIQUETES_KEY, JSON.stringify(list));
      },
      error: () => {
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
  private captureInstallPrompt(event: Event): void {
    event.preventDefault();
    this.deferredInstall = event as BeforeInstallPromptEvent;
    this.canInstall.set(true);
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
