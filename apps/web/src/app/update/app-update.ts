import { DestroyRef, Injectable, InjectionToken, inject, signal } from '@angular/core';
import { SwUpdate, VersionEvent, VersionReadyEvent } from '@angular/service-worker';
import { Subscription, filter } from 'rxjs';

/** How often an open app asks the server for a new version (SCRUM-166). */
export const UPDATE_CHECK_INTERVAL_MS = 60_000;

/** Reloads the page. A token, so the tests can replace it: jsdom cannot reload. */
export const RELOAD_PAGE = new InjectionToken<() => void>('RELOAD_PAGE', {
  providedIn: 'root',
  factory: () => () => location.reload(),
});

/**
 * Tells an open app that a new version was deployed (SCRUM-166). Without it, the service worker keeps serving the
 * version the page started with until the user reloads. When the service worker is off (`ng serve`, the tests), there
 * is no `SwUpdate` or it is not enabled, and this does nothing.
 */
@Injectable({ providedIn: 'root' })
export class AppUpdate {
  private readonly swUpdate = inject(SwUpdate, { optional: true });
  private readonly reloadPage = inject(RELOAD_PAGE);
  private readonly subscriptions = new Subscription();
  private timer: ReturnType<typeof setInterval> | undefined;

  /** True once the service worker has downloaded a new version: the app shows "Reload". */
  readonly available = signal(false);

  constructor() {
    inject(DestroyRef).onDestroy(() => this.stop());
  }

  start(): void {
    const swUpdate = this.swUpdate;
    if (!swUpdate?.isEnabled || this.timer !== undefined) {
      return;
    }
    this.subscriptions.add(
      swUpdate.versionUpdates
        .pipe(filter((event: VersionEvent): event is VersionReadyEvent => event.type === 'VERSION_READY'))
        .subscribe(() => this.available.set(true)),
    );
    // The cached version cannot be served any more (e.g. its files were removed from the server): only a reload helps.
    this.subscriptions.add(swUpdate.unrecoverable.subscribe(() => this.reloadPage()));
    this.timer = setInterval(() => {
      // offline or the server is down: try again at the next tick
      swUpdate.checkForUpdate().catch(() => undefined);
    }, UPDATE_CHECK_INTERVAL_MS);
  }

  /** Switches to the new version and reloads, so every file comes from the same version. */
  async reload(): Promise<void> {
    try {
      await this.swUpdate?.activateUpdate();
    } catch {
      // the reload below loads the new version anyway
    } finally {
      this.reloadPage();
    }
  }

  private stop(): void {
    clearInterval(this.timer);
    this.timer = undefined;
    this.subscriptions.unsubscribe();
  }
}
