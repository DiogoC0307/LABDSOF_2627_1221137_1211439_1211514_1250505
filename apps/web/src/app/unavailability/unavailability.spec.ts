import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { App } from '../app';
import { http, setValue, signInAs, testProviders } from '../testing/test-support';
import { formatLisbon, lisbonToUtc } from './lisbon-time';

const period = {
  id: '64000000-0000-4000-8000-000000000001',
  startsAt: '2026-12-05T00:00:00Z',
  endsAt: '2026-12-08T00:00:00Z',
  source: 'FORM',
};

describe('Lisbon time', () => {
  it('turns a date and a time in Lisbon into an instant in UTC, in winter and in summer', () => {
    expect(lisbonToUtc('2026-12-05', '00:00')).toBe('2026-12-05T00:00:00.000Z');
    expect(lisbonToUtc('2027-07-01', '08:00')).toBe('2027-07-01T07:00:00.000Z');
    expect(lisbonToUtc('2027-03-28', '03:00')).toBe('2027-03-28T02:00:00.000Z');
  });

  it('shows an instant in Lisbon time', () => {
    expect(formatLisbon('2026-12-05T00:00:00Z')).toBe('2026-12-05 00:00');
    expect(formatLisbon('2027-07-01T07:00:00Z')).toBe('2027-07-01 08:00');
  });
});

/** Asked by the free-text part of the page (SCRUM-89); answered here so that every request is accounted for. */
const SETTINGS = '/api/v1/unavailability-messages/settings';

/** "When I can't": a member declares and removes their own unavailability (SCRUM-64). */
describe('Unavailability', () => {
  beforeEach(async () => {
    localStorage.clear();
    signInAs('MEMBER');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: testProviders,
    }).compileComponents();
  });

  afterEach(() => {
    http().verify();
    localStorage.clear();
  });

  async function start() {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl('/unavailability');
    fixture.detectChanges();
    http().expectOne(SETTINGS).flush({ aiEnabled: true });
    return fixture;
  }

  function page(fixture: ComponentFixture<App>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function field(fixture: ComponentFixture<App>, name: string): HTMLInputElement {
    return page(fixture).querySelector<HTMLInputElement>(`[name="${name}"]`)!;
  }

  async function fill(fixture: ComponentFixture<App>, values: Record<string, string>) {
    await fixture.whenStable();
    Object.entries(values).forEach(([name, value]) => setValue(field(fixture, name), value));
    fixture.detectChanges();
    await fixture.whenStable();
  }

  it('TC-SCRUM-64-01 a member saves whole days from the 5th to the 7th of December and the period appears in "When I can\'t"', async () => {
    const fixture = await start();
    http().expectOne('/api/v1/unavailabilities').flush([]);
    fixture.detectChanges();
    expect(page(fixture).querySelector('h1')?.textContent).toContain("When I can't");
    await fill(fixture, { startDate: '2026-12-05', endDate: '2026-12-07' });

    page(fixture).querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
    const post = http().expectOne({ method: 'POST', url: '/api/v1/unavailabilities' });
    expect(post.request.body).toEqual({ startsAt: '2026-12-05T00:00:00.000Z', endsAt: '2026-12-08T00:00:00.000Z' });
    post.flush(period, { status: 201, statusText: 'Created' });
    http().expectOne('/api/v1/unavailabilities').flush([period]);
    fixture.detectChanges();

    const item = page(fixture).querySelector(`[data-id="${period.id}"]`);
    expect(item?.textContent).toContain('2026-12-05 00:00');
    expect(item?.textContent).toContain('2026-12-08 00:00');
  });

  it('TC-SCRUM-64-01 times are taken as Lisbon time and sent in UTC', async () => {
    const fixture = await start();
    http().expectOne('/api/v1/unavailabilities').flush([]);
    await fill(fixture, { startDate: '2027-07-01', startTime: '08:00', endDate: '2027-07-01', endTime: '20:00' });

    page(fixture).querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
    const post = http().expectOne({ method: 'POST', url: '/api/v1/unavailabilities' });
    expect(post.request.body).toEqual({ startsAt: '2027-07-01T07:00:00.000Z', endsAt: '2027-07-01T19:00:00.000Z' });
    post.flush({ ...period, startsAt: '2027-07-01T07:00:00Z', endsAt: '2027-07-01T19:00:00Z' }, { status: 201, statusText: 'Created' });
    http().expectOne('/api/v1/unavailabilities').flush([]);
  });

  it('TC-SCRUM-64-02 an end date before the start date shows an error and nothing is sent', async () => {
    const fixture = await start();
    http().expectOne('/api/v1/unavailabilities').flush([]);
    await fill(fixture, { startDate: '2026-12-10', endDate: '2026-12-09' });

    page(fixture).querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
    fixture.detectChanges();

    http().expectNone({ method: 'POST', url: '/api/v1/unavailabilities' });
    expect(page(fixture).querySelector('[role="alert"] [data-field="endsAt"]')?.textContent).toContain('before the start');
  });

  it('TC-SCRUM-64-02 the errors the API answers are shown', async () => {
    const fixture = await start();
    http().expectOne('/api/v1/unavailabilities').flush([]);
    await fill(fixture, { startDate: '2026-11-01', endDate: '2026-11-02' });

    page(fixture).querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
    http()
      .expectOne({ method: 'POST', url: '/api/v1/unavailabilities' })
      .flush({ status: 400, errors: [{ field: 'endsAt', code: 'in_the_past' }] }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(page(fixture).querySelector('[role="alert"] [data-field="endsAt"]')?.textContent).toContain('already ended');
  });

  it('TC-SCRUM-158-04 two clicks on Save before the answer send one period; the button is disabled until the answer, then enabled again', async () => {
    const fixture = await start();
    http().expectOne('/api/v1/unavailabilities').flush([]);
    await fill(fixture, { startDate: '2026-12-05', endDate: '2026-12-07' });
    const save = page(fixture).querySelector<HTMLButtonElement>('form button[type="submit"]')!;

    save.click();
    save.click();
    fixture.detectChanges();

    const post = http().expectOne({ method: 'POST', url: '/api/v1/unavailabilities' });
    expect(save.disabled).toBe(true);
    post.flush({ status: 400, errors: [{ field: 'endsAt', code: 'in_the_past' }] }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(save.disabled).toBe(false);
  });

  it('a member removes a period, and the list is loaded again', async () => {
    const fixture = await start();
    http().expectOne('/api/v1/unavailabilities').flush([period]);
    fixture.detectChanges();

    page(fixture).querySelector<HTMLButtonElement>(`[data-id="${period.id}"] button.delete`)!.click();
    http().expectOne({ method: 'DELETE', url: `/api/v1/unavailabilities/${period.id}` }).flush(null, { status: 204, statusText: 'No Content' });
    http().expectOne('/api/v1/unavailabilities').flush([]);
    fixture.detectChanges();

    expect(page(fixture).querySelector(`[data-id="${period.id}"]`)).toBeNull();
  });

  it('every role has "When I can\'t" in the menu', async () => {
    for (const role of ['MEMBER', 'SUPERVISOR', 'ADMIN']) {
      localStorage.clear();
      signInAs(role);
      TestBed.resetTestingModule();
      await TestBed.configureTestingModule({ imports: [App], providers: testProviders }).compileComponents();
      const fixture = TestBed.createComponent(App);
      fixture.detectChanges();
      await TestBed.inject(Router).navigateByUrl('/unavailability');
      fixture.detectChanges();
      http().expectOne('/api/v1/unavailabilities').flush([]);
      http().expectOne(SETTINGS).flush({ aiEnabled: true });
      expect(page(fixture).querySelector('nav[aria-label="Main"] a[href="/unavailability"]')).not.toBeNull();
    }
  });
});
