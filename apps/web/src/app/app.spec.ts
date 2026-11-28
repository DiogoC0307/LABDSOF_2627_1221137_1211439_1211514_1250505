import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { App } from './app';
import {
  SESSION_KEY,
  flushNotificationRequests,
  flushWeekRequests,
  weekRequest,
  http,
  pending,
  signInAs,
  testProviders,
  users,
} from './testing/test-support';

/** The shell: sign-in, the navbar by role, the router with its role guards, sign-out and the install prompt. */
describe('App', () => {
  beforeEach(async () => {
    localStorage.clear();
    signInAs('SUPERVISOR');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: testProviders,
    }).compileComponents();
  });

  afterEach(() => {
    flushWeekRequests();
    flushNotificationRequests();
    http().verify();
    localStorage.clear();
  });

  async function start(url = '/') {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl(url);
    fixture.detectChanges();
    return fixture;
  }

  it('TC-SCRUM-63-01 "supervisor" signs in and can add shifts to the week', async () => {
    localStorage.removeItem(SESSION_KEY);
    const fixture = await start();
    http().expectOne('/api/v1/auth/mock-users').flush(users);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('form')).toBeNull();
    expect(page.textContent).toContain('Simulated sign-in');

    const supervisor = Array.from(page.querySelectorAll<HTMLButtonElement>('.sign-in button')).find((button) =>
      button.textContent?.includes('Synthetic Supervisor'),
    );
    supervisor!.click();
    const signIn = http().expectOne({ method: 'POST', url: '/api/v1/auth/mock-sign-in' });
    expect(signIn.request.body).toEqual({ memberId: users[1].id });
    expect(signIn.request.headers.has('Authorization')).toBe(false);
    signIn.flush({
      token: 'mock.supervisor-token',
      memberId: users[1].id,
      role: 'SUPERVISOR',
      fullName: 'Synthetic Supervisor',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    const list = weekRequest();
    expect(list.request.headers.get('Authorization')).toBe('Bearer mock.supervisor-token');
    list.flush([]);
    fixture.detectChanges();
    expect(page.querySelector('.week-grid')).not.toBeNull();

    // Current week is read-only until the user clicks "Edit schedule"
    Array.from(page.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.trim() === 'Edit schedule')!
      .click();
    fixture.detectChanges();
    expect(page.querySelectorAll('.add-slot').length).toBeGreaterThan(0);
    expect(page.querySelector('app-navbar .session')?.textContent?.trim()).toBe('Synthetic Supervisor');
  });

  it('sends the token with every API call', async () => {
    await start();
    const list = weekRequest();
    expect(list.request.headers.get('Authorization')).toBe('Bearer mock.SUPERVISOR');
    list.flush([]);
  });

  it('goes back to the sign-in screen when the API answers 401', async () => {
    const fixture = await start();
    weekRequest().flush('', { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();
    http().expectOne('/api/v1/auth/mock-users').flush(users);
    fixture.detectChanges();
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Simulated sign-in');
  });

  it('sign out clears the session and the last loaded roster', async () => {
    const fixture = await start();
    weekRequest().flush([pending]);
    fixture.detectChanges();
    expect(localStorage.getItem('escala.lastPiquetes')).not.toBeNull();

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.sign-out')!.click();
    fixture.detectChanges();
    http().expectOne('/api/v1/auth/mock-users').flush(users);
    fixture.detectChanges();
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
    expect(localStorage.getItem('escala.lastPiquetes')).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('2026-11-05');
  });

  it.each(['SUPERVISOR', 'ADMIN'])('the menu of "%s" has Members', async (role) => {
    localStorage.clear();
    signInAs(role);
    const fixture = await start();
    weekRequest().flush([]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('nav[aria-label="Main"] a[href="/members"]')).not.toBeNull();
  });

  it('the menu of a member has no Members, and the guard sends them back to the schedule', async () => {
    localStorage.clear();
    signInAs('MEMBER');
    const fixture = await start('/members');
    weekRequest().flush([]);
    http().expectNone('/api/v1/members');
    fixture.detectChanges();
    expect(TestBed.inject(Router).url).toBe('/');
    expect((fixture.nativeElement as HTMLElement).querySelector('nav[aria-label="Main"] a[href="/members"]')).toBeNull();
  });

  it('TC-SCRUM-46-02 offers install when the browser fires beforeinstallprompt', async () => {
    const fixture = await start();
    weekRequest().flush([]);

    const prompt = Object.assign(new Event('beforeinstallprompt', { cancelable: true }), {
      prompt: async () => undefined,
    });
    window.dispatchEvent(prompt);
    fixture.detectChanges();

    const install = (fixture.nativeElement as HTMLElement).querySelector('.install');
    expect(install?.textContent).toContain('Install Escala');
    (install as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.install')).toBeNull();
  });
});
