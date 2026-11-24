import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ShiftForm } from './shift-form';
import { ShiftFormTarget } from './piquete';
import { http, setValue, signInAs, testProviders } from '../testing/test-support';

/** The add and edit shift form (SCRUM-65). Lisbon is on UTC in November, so the instants read as the times typed. */
describe('ShiftForm', () => {
  let saved: number;
  let closed: number;

  async function open(target: ShiftFormTarget) {
    const fixture = TestBed.createComponent(ShiftForm);
    fixture.componentRef.setInput('target', target);
    fixture.componentRef.setInput('today', '2026-11-25');
    fixture.componentInstance.saved.subscribe(() => saved++);
    fixture.componentInstance.closed.subscribe(() => closed++);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  function page(fixture: ComponentFixture<ShiftForm>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function input(fixture: ComponentFixture<ShiftForm>, id: string): HTMLInputElement {
    return page(fixture).querySelector<HTMLInputElement>(`#${id}`)!;
  }

  async function submit(fixture: ComponentFixture<ShiftForm>) {
    page(fixture).querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  beforeEach(async () => {
    saved = 0;
    closed = 0;
    localStorage.clear();
    signInAs('SUPERVISOR');
    await TestBed.configureTestingModule({ imports: [ShiftForm], providers: testProviders }).compileComponents();
  });

  afterEach(() => {
    http().verify();
    localStorage.clear();
  });

  it('opens as a modal dialog on the day of the "+", with a day shift from 08:00 to 20:00', async () => {
    const fixture = await open({ kind: 'add', date: '2026-11-26' });

    const dialog = page(fixture).querySelector('dialog')!;
    expect(dialog.hasAttribute('open')).toBe(true);
    expect(page(fixture).querySelector('#shift-form-title')?.textContent).toBe('Add a shift');
    expect(page(fixture).querySelector('.shift-form-day')?.textContent).toBe('Thursday 26 November');
    expect(input(fixture, 'shift-day').value).toBe('2026-11-26');
    expect(input(fixture, 'shift-start').value).toBe('08:00');
    expect(input(fixture, 'shift-end').value).toBe('20:00');
    expect(page(fixture).querySelector<HTMLInputElement>('input[value="DAY"]')!.checked).toBe(true);
  });

  it('adds the shift with POST, its times as instants, then tells the week and closes', async () => {
    const fixture = await open({ kind: 'add', date: '2026-11-26' });
    setValue(input(fixture, 'shift-start'), '12:00');
    setValue(input(fixture, 'shift-end'), '13:30');
    setValue(input(fixture, 'shift-crew'), '3');
    await submit(fixture);

    const request = http().expectOne({ method: 'POST', url: '/api/v1/piquetes' });
    expect(request.request.body).toEqual({
      dutyDate: '2026-11-26',
      period: 'DAY',
      startsAt: '2026-11-26T12:00:00Z',
      endsAt: '2026-11-26T13:30:00Z',
      minimumCrew: 3,
    });
    request.flush({ id: 'p1', dutyDate: '2026-11-26', slots: [] });
    expect(saved).toBe(1);
    expect(closed).toBe(1);
  });

  it('turns the times to 20:00 and 08:00 for a night shift, which ends the next day', async () => {
    const fixture = await open({ kind: 'add', date: '2026-11-26' });
    const night = page(fixture).querySelector<HTMLInputElement>('input[value="NIGHT"]')!;
    night.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(input(fixture, 'shift-start').value).toBe('20:00');
    expect(input(fixture, 'shift-end').value).toBe('08:00');
    expect(page(fixture).querySelector('.field-hint')?.textContent).toContain('Ends the next day');

    await submit(fixture);
    const request = http().expectOne({ method: 'POST', url: '/api/v1/piquetes' });
    expect(request.request.body.period).toBe('NIGHT');
    expect(request.request.body.startsAt).toBe('2026-11-26T20:00:00Z');
    expect(request.request.body.endsAt).toBe('2026-11-27T08:00:00Z');
    request.flush({ id: 'p1', dutyDate: '2026-11-26', slots: [] });
  });

  it('opens a shift to edit with its own values and saves it with PUT', async () => {
    const fixture = await open({
      kind: 'edit',
      piquete: {
        id: 'p-noon',
        dutyDate: '2026-11-27',
        period: 'DAY',
        startsAt: '2026-11-27T09:00:00Z',
        endsAt: '2026-11-27T13:00:00Z',
        minimumCrew: 4,
        slots: [],
      },
    });

    expect(page(fixture).querySelector('#shift-form-title')?.textContent).toBe('Edit shift');
    expect(input(fixture, 'shift-day').value).toBe('2026-11-27');
    expect(input(fixture, 'shift-start').value).toBe('09:00');
    expect(input(fixture, 'shift-end').value).toBe('13:00');
    expect(input(fixture, 'shift-crew').value).toBe('4');

    setValue(input(fixture, 'shift-end'), '14:00');
    await submit(fixture);
    const request = http().expectOne({ method: 'PUT', url: '/api/v1/piquetes/p-noon' });
    expect(request.request.body.endsAt).toBe('2026-11-27T14:00:00Z');
    expect(request.request.body.minimumCrew).toBe(4);
    request.flush({ id: 'p-noon', dutyDate: '2026-11-27', slots: [] });
    expect(saved).toBe(1);
  });

  it('asks for the minimum crew before sending anything', async () => {
    const fixture = await open({ kind: 'add', date: '2026-11-26' });
    setValue(input(fixture, 'shift-crew'), '0');
    await submit(fixture);
    fixture.detectChanges();

    http().expectNone('/api/v1/piquetes');
    expect(page(fixture).querySelector('#shift-crew-error')?.textContent).toContain('at least 1 firefighter');
    expect(input(fixture, 'shift-crew').getAttribute('aria-invalid')).toBe('true');
  });

  it("shows the API's reasons next to their fields", async () => {
    const fixture = await open({ kind: 'add', date: '2026-11-26' });
    await submit(fixture);
    http()
      .expectOne({ method: 'POST', url: '/api/v1/piquetes' })
      .flush(
        { errors: [{ field: 'startsAt', code: 'not_on_duty_date' }] },
        { status: 400, statusText: 'Bad Request' },
      );
    fixture.detectChanges();

    expect(page(fixture).querySelector('#shift-start-error')?.textContent).toContain('start on the chosen day');
    expect(saved).toBe(0);
    expect(closed).toBe(0);
  });

  it('says so when the shift is on a day that has passed (409)', async () => {
    const fixture = await open({
      kind: 'edit',
      piquete: { id: 'p-old', dutyDate: '2026-11-26', slots: [] },
    });
    await submit(fixture);
    http()
      .expectOne({ method: 'PUT', url: '/api/v1/piquetes/p-old' })
      .flush('', { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(page(fixture).querySelector('[role="alert"]')?.textContent).toContain("can't be changed");
  });

  it('closes on Cancel without sending anything', async () => {
    const fixture = await open({ kind: 'add', date: '2026-11-26' });
    Array.from(page(fixture).querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.trim() === 'Cancel')!
      .click();

    http().expectNone('/api/v1/piquetes');
    expect(closed).toBe(1);
  });
});
