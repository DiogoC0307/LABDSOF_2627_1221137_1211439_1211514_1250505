import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { SwUpdate, UnrecoverableStateEvent, VersionEvent } from '@angular/service-worker';
import { Subject } from 'rxjs';
import { App } from '../app';
import { http, signInAs, testProviders } from '../testing/test-support';
import { RELOAD_PAGE, UPDATE_CHECK_INTERVAL_MS } from './app-update';

/** A simulated SwUpdate: the tests push its events and see what the app asks of it. */
class FakeSwUpdate {
  isEnabled = true;
  readonly versionUpdates = new Subject<VersionEvent>();
  readonly unrecoverable = new Subject<UnrecoverableStateEvent>();
  readonly checkForUpdate = vi.fn(async () => false);
  readonly activateUpdate = vi.fn(async () => true);
}

const versionReady: VersionEvent = {
  type: 'VERSION_READY',
  currentVersion: { hash: 'old' },
  latestVersion: { hash: 'new' },
};

/** The shell tells an open app that a new version was deployed (SCRUM-166). */
describe('AppUpdate', () => {
  let swUpdate: FakeSwUpdate;
  let calls: string[];

  async function start(providers: unknown[]) {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [...testProviders, { provide: RELOAD_PAGE, useValue: () => calls.push('reload') }, ...providers],
    }).compileComponents();
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl('/');
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);
    fixture.detectChanges();
    return fixture;
  }

  function notice(page: HTMLElement): HTMLOutputElement | null {
    return page.querySelector('output.notice');
  }

  beforeEach(() => {
    localStorage.clear();
    signInAs('MEMBER');
    swUpdate = new FakeSwUpdate();
    calls = [];
    swUpdate.activateUpdate.mockImplementation(async () => {
      calls.push('activate');
      return true;
    });
  });

  afterEach(() => {
    http().verify();
    localStorage.clear();
    vi.useRealTimers();
  });

  it('TC-SCRUM-166-01 an open app checks for a new version every minute and, on VERSION_READY, says so in a status message with "Reload"', async () => {
    vi.useFakeTimers();
    swUpdate.checkForUpdate.mockRejectedValueOnce(new Error('offline'));
    const fixture = await start([{ provide: SwUpdate, useValue: swUpdate }]);
    const page = fixture.nativeElement as HTMLElement;
    expect(swUpdate.checkForUpdate).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(UPDATE_CHECK_INTERVAL_MS);
    expect(swUpdate.checkForUpdate).toHaveBeenCalledTimes(1);
    // the first check failed (offline); the next minute checks again
    await vi.advanceTimersByTimeAsync(UPDATE_CHECK_INTERVAL_MS);
    expect(swUpdate.checkForUpdate).toHaveBeenCalledTimes(2);

    // a version found but not downloaded yet is not announced
    swUpdate.versionUpdates.next({ type: 'VERSION_DETECTED', version: { hash: 'new' } });
    fixture.detectChanges();
    expect(notice(page)).toBeNull();

    swUpdate.versionUpdates.next(versionReady);
    fixture.detectChanges();
    const output = notice(page)!;
    expect(output.textContent).toContain('A new version of Escala is available.');
    expect(output.querySelector('button')?.textContent?.trim()).toBe('Reload');
    expect(output.querySelector('button')?.type).toBe('button');
    // <output> has the status role, so a screen reader reads the message without moving the focus
    expect(output.tagName).toBe('OUTPUT');
    expect(calls).toEqual([]);
  });

  it('TC-SCRUM-166-02 "Reload" activates the new version and then reloads the page', async () => {
    const fixture = await start([{ provide: SwUpdate, useValue: swUpdate }]);
    swUpdate.versionUpdates.next(versionReady);
    fixture.detectChanges();

    notice(fixture.nativeElement as HTMLElement)!.querySelector('button')!.click();
    await fixture.whenStable();
    expect(swUpdate.activateUpdate).toHaveBeenCalledTimes(1);
    expect(calls).toEqual(['activate', 'reload']);

    // if the switch fails, the reload still loads the new version
    calls = [];
    swUpdate.activateUpdate.mockRejectedValueOnce(new Error('no version to activate'));
    notice(fixture.nativeElement as HTMLElement)!.querySelector('button')!.click();
    await fixture.whenStable();
    expect(calls).toEqual(['reload']);
  });

  it('TC-SCRUM-166-03 when the cached version cannot be served any more (unrecoverable), the page reloads at once', async () => {
    const fixture = await start([{ provide: SwUpdate, useValue: swUpdate }]);
    swUpdate.unrecoverable.next({ type: 'UNRECOVERABLE_STATE', reason: 'Hash mismatch' });
    fixture.detectChanges();
    expect(calls).toEqual(['reload']);
    expect(swUpdate.activateUpdate).not.toHaveBeenCalled();
    expect(notice(fixture.nativeElement as HTMLElement)).toBeNull();
  });

  it('TC-SCRUM-166-04 with the service worker off (not enabled, or not provided) the app starts, never checks and shows no message', async () => {
    vi.useFakeTimers();
    swUpdate.isEnabled = false;
    const fixture = await start([{ provide: SwUpdate, useValue: swUpdate }]);
    await vi.advanceTimersByTimeAsync(3 * UPDATE_CHECK_INTERVAL_MS);
    swUpdate.versionUpdates.next(versionReady);
    fixture.detectChanges();
    expect(swUpdate.checkForUpdate).not.toHaveBeenCalled();
    expect(notice(fixture.nativeElement as HTMLElement)).toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('nav.menu')).not.toBeNull();
  });

  it('TC-SCRUM-166-04 without SwUpdate at all (as in the other specs) the app starts and shows no message', async () => {
    const fixture = await start([]);
    expect(notice(fixture.nativeElement as HTMLElement)).toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('nav.menu')).not.toBeNull();
  });
});
