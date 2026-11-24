import { TestBed } from '@angular/core/testing';
import { Schedule } from './schedule';
import {
  eligible,
  http,
  pending,
  signInAs,
  testProviders,
  todayIso,
  todaysPiquete,
} from '../testing/test-support';

describe('Schedule', () => {
  beforeEach(async () => {
    localStorage.clear();
    signInAs('SUPERVISOR');
    await TestBed.configureTestingModule({
      imports: [Schedule],
      providers: testProviders,
    }).compileComponents();
  });

  afterEach(() => {
    vi.useRealTimers();
    http().verify();
    localStorage.clear();
  });

  it("should render today's piquete heading", () => {
    const fixture = TestBed.createComponent(Schedule);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain("Today's piquete");
  });

  it("TC-SCRUM-46-01 lists today's date, slot and evaluation status", () => {
    const fixture = TestBed.createComponent(Schedule);
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
    const fixture = TestBed.createComponent(Schedule);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([pending]);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('2026-11-05');
    expect(text).toContain('PENDING');
  });

  it('shows an error when the list cannot be loaded', () => {
    const fixture = TestBed.createComponent(Schedule);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush('fail', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain(
      'Could not load piquetes.',
    );
  });

  it('shows the last loaded roster when the list cannot be loaded', () => {
    localStorage.setItem('escala.lastPiquetes', JSON.stringify([todaysPiquete('PENDING')]));
    const fixture = TestBed.createComponent(Schedule);
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
    const fixture = TestBed.createComponent(Schedule);
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
    const fixture = TestBed.createComponent(Schedule);
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

  it('TC-SCRUM-158-03 two clicks on Create piquete before the answer create one piquete; the button is disabled until the answer, then enabled again', () => {
    const fixture = TestBed.createComponent(Schedule);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);
    fixture.detectChanges();
    const create = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('form button[type="submit"]')!;

    create.click();
    create.click();
    fixture.detectChanges();

    const post = http().expectOne({ method: 'POST', url: '/api/v1/piquetes' });
    expect(create.disabled).toBe(true);
    post.flush('fail', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(create.disabled).toBe(false);
  });

  it('keeps polling while the slot is still PENDING', async () => {
    vi.useFakeTimers();
    const fixture = TestBed.createComponent(Schedule);
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
    const fixture = TestBed.createComponent(Schedule);
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
    const fixture = TestBed.createComponent(Schedule);
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

  it.each(['MEMBER', 'ADMIN'])('TC-SCRUM-63-01 "%s" signed in sees the schedule but not the schedule editor', (role) => {
    localStorage.clear();
    signInAs(role);
    const fixture = TestBed.createComponent(Schedule);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([todaysPiquete('ELIGIBLE')]);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('form')).toBeNull();
    expect(page.textContent).not.toContain('Create piquete');
    expect(page.querySelector('[aria-label="Today\'s piquete"]')?.textContent).toContain('ELIGIBLE');
  });
});
