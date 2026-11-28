import { HttpClient } from '@angular/common/http';
import { DestroyRef, Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, Subscription, catchError, switchMap, timer } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { SlotRole } from '../schedule/piquete';

export type NotificationType =
  | 'WEEKLY_SCHEDULE_PUBLISHED'
  | 'ADDED_TO_SHIFT'
  | 'REMOVED_FROM_SHIFT'
  | 'SHIFT_TIMES_CHANGED'
  | 'SHIFT_CANCELLED'
  | 'SLOT_OPENED'
  | 'COVER_REQUESTED'
  | 'COVER_ESCALATED';

/** Why nobody covers a slot yet (SCRUM-72): nobody is free and qualified, or nobody accepted in time. */
export type CoverEscalation = 'NO_CANDIDATES' | 'NO_ACCEPTANCE';

// A notification as GET /api/v1/notifications answers it
export interface AppNotification {
  id: string;
  type: NotificationType;
  week: string;
  shiftId?: string | null;
  startsAt?: string | null;
  endsAt?: string | null;
  previousStartsAt?: string | null;
  previousEndsAt?: string | null;
  shiftCount?: number | null;
  coverRequestId?: string | null;
  role?: SlotRole | null;
  reason?: CoverEscalation | null;
  createdAt: string;
  read: boolean;
}

/** Whether the list has been loaded: "loading" until the first answer, "error" when that first request failed. */
export type NotificationsStatus = 'loading' | 'ready' | 'error';

// How often the app verifies for new notifications in milliseconds
export const NOTIFICATIONS_EVERY_MS = 30_000;

export const MARK_READ_FAILED = "Couldn't update notifications. Try again.";

/**
 * The signed-in member's notifications
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  private polling?: Subscription;

  readonly list = signal<AppNotification[]>([]);
  readonly status = signal<NotificationsStatus>('loading');
  readonly unread = computed(() => this.list().filter((each) => !each.read).length);

  private readonly follow = effect(() => {
    const memberId = this.auth.session()?.memberId ?? null;
    untracked(() => this.restart(memberId));
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => this.polling?.unsubscribe());
  }

  /** Marks one notification read at once in the UI; puts it back and says so when the server refuses. */
  markRead(id: string): void {
    if (!this.list().some((each) => each.id === id && !each.read)) {
      return;
    }
    this.setRead(new Set([id]), true);
    this.http.post(`/api/v1/notifications/${id}/read`, null).subscribe({
      error: () => this.rollBack(new Set([id])),
    });
  }

  /** Marks every unread notification read at once in the UI; puts them back and says so when the server refuses. */
  markAllRead(): void {
    const ids = new Set(this.list().filter((each) => !each.read).map((each) => each.id));
    if (ids.size === 0) {
      return;
    }
    this.setRead(ids, true);
    this.http.post('/api/v1/notifications/read', null).subscribe({ error: () => this.rollBack(ids) });
  }

  /** Asks for the list again now, after it failed to load. */
  retry(): void {
    this.restart(this.auth.session()?.memberId ?? null);
  }

  private setRead(ids: ReadonlySet<string>, read: boolean): void {
    this.list.update((list) => list.map((each) => (ids.has(each.id) ? { ...each, read } : each)));
  }

  private rollBack(ids: ReadonlySet<string>): void {
    this.setRead(ids, false);
    this.snackBar.open(MARK_READ_FAILED, undefined, { duration: 6000 });
  }

  private restart(memberId: string | null): void {
    this.polling?.unsubscribe();
    this.list.set([]);
    this.status.set('loading');
    if (memberId === null) {
      return;
    }
    this.polling = timer(0, NOTIFICATIONS_EVERY_MS)
      .pipe(
        switchMap(() =>
          this.http.get<AppNotification[]>('/api/v1/notifications').pipe(
            catchError(() => {
              // A failed check after the list loaded keeps the list; the next check tries again
              if (this.status() !== 'ready') {
                this.status.set('error');
              }
              return EMPTY;
            }),
          ),
        ),
      )
      .subscribe((list) => {
        this.list.set(list);
        this.status.set('ready');
      });
  }
}
