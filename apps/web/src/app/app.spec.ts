import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';
import { authInterceptor } from './auth/auth.interceptor';

const SESSION_KEY = 'escala.session';

const users = [
  { id: '00000000-0000-4000-8000-000000000001', fullName: 'Synthetic Member', role: 'MEMBER' },
  { id: '00000000-0000-4000-8000-000000000002', fullName: 'Synthetic Supervisor', role: 'SUPERVISOR' },
  { id: '00000000-0000-4000-8000-000000000003', fullName: 'Synthetic Admin', role: 'ADMIN' },
];

function signInAs(role: string): void {
  const user = users.find((candidate) => candidate.role === role)!;
  localStorage.setItem(
    SESSION_KEY,
    JSON.stringify({ token: `mock.${role}`, memberId: user.id, role, fullName: user.fullName }),
  );
}

const pending = {
  id: 'p1',
  dutyDate: '2026-11-05',
  slots: [{ id: 's1', role: 'CREW_MEMBER', status: 'PENDING' }],
};

const eligible = {
  ...pending,
  slots: [{ id: 's1', role: 'CREW_MEMBER', status: 'ELIGIBLE' }],
};

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function todaysPiquete(status = 'ELIGIBLE') {
  return {
    id: 'today',
    dutyDate: todayIso(),
    slots: [{ id: 's-today', role: 'CREW_MEMBER', status }],
  };
}

describe('App', () => {
  beforeEach(async () => {
    localStorage.clear();
    signInAs('SUPERVISOR');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    }).compileComponents();
  });

  afterEach(() => {
    vi.useRealTimers();
    TestBed.inject(HttpTestingController).verify();
    localStorage.clear();
  });

  function http(): HttpTestingController {
    return TestBed.inject(HttpTestingController);
  }

  it("should render today's piquete heading", () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain("Today's piquete");
  });

  it("TC-SCRUM-46-01 lists today's date, slot and evaluation status", () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([todaysPiquete('ELIGIBLE')]);
    fixture.detectChanges();
    const card = (fixture.nativeElement as HTMLElement).querySelector('[aria-label="Today\'s piquete"]');
    expect(card?.textContent).toContain(todayIso());
    expect(card?.textContent).toContain('CREW_MEMBER');
    expect(card?.textContent).toContain('Evaluation');
    expect(card?.textContent).toContain('ELIGIBLE');
  });

  it('lists piquetes returned by the API', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([pending]);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('2026-11-05');
    expect(text).toContain('PENDING');
  });

  it('shows an error when the list cannot be loaded', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush('fail', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain(
      'Could not load piquetes.',
    );
  });

  it('shows the last loaded roster when the list cannot be loaded', () => {
    localStorage.setItem('escala.lastPiquetes', JSON.stringify([todaysPiquete('PENDING')]));
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush('fail', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Showing the last loaded roster');
    expect(text).toContain('PENDING');
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')).toBeNull();
  });

  it('creates a piquete and polls until the slot is ELIGIBLE', async () => {
    vi.useFakeTimers();
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('form button')?.click();
    const post = http().expectOne('/api/v1/piquetes');
    expect(post.request.method).toBe('POST');
    expect(post.request.body.dutyDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    post.flush(pending);

    http().expectOne('/api/v1/piquetes').flush([pending]);
    await vi.advanceTimersByTimeAsync(1000);
    http().expectOne('/api/v1/piquetes').flush([eligible]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ELIGIBLE');
  });

  it('shows an error when create fails', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('form button')?.click();
    http().expectOne({ method: 'POST', url: '/api/v1/piquetes' }).flush('fail', {
      status: 500,
      statusText: 'Server Error',
    });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain(
      'Could not create the piquete.',
    );
  });

  it('keeps polling while the slot is still PENDING', async () => {
    vi.useFakeTimers();
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('form button')?.click();
    http().expectOne({ method: 'POST', url: '/api/v1/piquetes' }).flush(pending);
    http().expectOne('/api/v1/piquetes').flush([pending]);

    await vi.advanceTimersByTimeAsync(1000);
    http().expectOne('/api/v1/piquetes').flush([pending]);
    await vi.advanceTimersByTimeAsync(1000);
    http().expectOne('/api/v1/piquetes').flush([eligible]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ELIGIBLE');
  });

  it('shows an error when a poll request fails', async () => {
    vi.useFakeTimers();
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('form button')?.click();
    http().expectOne({ method: 'POST', url: '/api/v1/piquetes' }).flush(pending);
    http().expectOne('/api/v1/piquetes').flush([pending]);

    await vi.advanceTimersByTimeAsync(1000);
    http().expectOne('/api/v1/piquetes').flush('fail', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain(
      'Could not load piquetes.',
    );
  });

  it('stops polling after 16 attempts', async () => {
    vi.useFakeTimers();
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('form button')?.click();
    http().expectOne({ method: 'POST', url: '/api/v1/piquetes' }).flush(pending);
    http().expectOne('/api/v1/piquetes').flush([pending]);

    for (let i = 0; i < 16; i++) {
      await vi.advanceTimersByTimeAsync(1000);
      http().expectOne('/api/v1/piquetes').flush([pending]);
    }
    await vi.advanceTimersByTimeAsync(1000);
    http().expectNone('/api/v1/piquetes');
  });

  it('TC-SCRUM-46-02 offers install when the browser fires beforeinstallprompt', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);

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

  it('TC-SCRUM-63-01 "supervisor" signs in and the schedule editor is shown', () => {
    localStorage.removeItem(SESSION_KEY);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
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
    signIn.flush({ token: 'mock.supervisor-token', memberId: users[1].id, role: 'SUPERVISOR', fullName: 'Synthetic Supervisor' });

    const list = http().expectOne('/api/v1/piquetes');
    expect(list.request.headers.get('Authorization')).toBe('Bearer mock.supervisor-token');
    list.flush([]);
    fixture.detectChanges();
    expect(page.querySelector('form button')?.textContent).toContain('Create piquete');
    expect(page.querySelector('.session')?.textContent).toContain('Synthetic Supervisor');
    expect(page.querySelector('.session')?.textContent).toContain('SUPERVISOR');
  });

  it.each(['MEMBER', 'ADMIN'])('TC-SCRUM-63-01 "%s" signed in sees the schedule but not the schedule editor', (role) => {
    localStorage.clear();
    signInAs(role);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([todaysPiquete('ELIGIBLE')]);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('form')).toBeNull();
    expect(page.textContent).not.toContain('Create piquete');
    expect(page.querySelector('[aria-label="Today\'s piquete"]')?.textContent).toContain('ELIGIBLE');
    expect(page.querySelector('.session')?.textContent).toContain(role);
  });

  it('sends the token with every API call', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const list = http().expectOne('/api/v1/piquetes');
    expect(list.request.headers.get('Authorization')).toBe('Bearer mock.SUPERVISOR');
    list.flush([]);
  });

  it('goes back to the sign-in screen when the API answers 401', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush('', { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();
    http().expectOne('/api/v1/auth/mock-users').flush(users);
    fixture.detectChanges();
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Simulated sign-in');
  });

  it('sign out clears the session and the last loaded roster', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([pending]);
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
});
