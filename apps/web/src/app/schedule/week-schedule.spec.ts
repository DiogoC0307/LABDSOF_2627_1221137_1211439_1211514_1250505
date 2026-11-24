import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WeekSchedule } from './week-schedule';
import { http, signInAs, testProviders, weekRequest } from '../testing/test-support';

/** The week's schedule (SCRUM-65, mockup). The clock is fixed on Wednesday 2026-11-25, in week 2026-W48. */
describe('WeekSchedule', () => {
  function render(role: string) {
    signInAs(role);
    const fixture = TestBed.createComponent(WeekSchedule);
    fixture.detectChanges();
    return fixture;
  }

  function flushWeek(
    fixture: ComponentFixture<WeekSchedule>,
    week: string,
    body: object | string = [],
    options?: { status: number; statusText: string },
  ) {
    const request = weekRequest();
    expect(request.request.params.get('week')).toBe(week);
    request.flush(body, options);
    fixture.detectChanges();
  }

  function element(fixture: ComponentFixture<WeekSchedule>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function click(fixture: ComponentFixture<WeekSchedule>, selector: string) {
    element(fixture).querySelector<HTMLElement>(selector)!.click();
    fixture.detectChanges();
  }

  function title(fixture: ComponentFixture<WeekSchedule>) {
    return element(fixture).querySelector('#week-title')?.textContent;
  }

  function calendarRows(fixture: ComponentFixture<WeekSchedule>) {
    return Array.from(element(fixture).querySelectorAll<HTMLButtonElement>('.calendar-week'));
  }

  beforeEach(async () => {
    localStorage.clear();
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-11-25T10:00:00Z'));
    await TestBed.configureTestingModule({ imports: [WeekSchedule], providers: testProviders }).compileComponents();
  });

  afterEach(() => {
    http().verify();
    vi.useRealTimers();
    localStorage.clear();
  });

  it("asks for this week's piquetes and titles the week with its month, year and number", () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48');

    expect(title(fixture)).toBe('November 2026 - Week 48');
  });

  it('shows the seven days from Monday to Sunday', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48');

    const days = element(fixture).querySelectorAll('.day');
    expect(days.length).toBe(7);
    expect(days[0].getAttribute('aria-label')).toBe('Monday 23 November');
    expect(days[6].getAttribute('aria-label')).toBe('Sunday 29 November');
    expect(days[2].getAttribute('aria-current')).toBe('date');
    expect(element(fixture).querySelectorAll('.shift').length).toBe(0);
  });

  it('gives the supervisor a "+" from today to Sunday, and none on the days that have passed', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48');

    const days = Array.from(element(fixture).querySelectorAll('.day'));
    expect(days.map((day) => day.querySelector('.add-shift') !== null)).toEqual([
      false,
      false,
      true,
      true,
      true,
      true,
      true,
    ]);
    expect(days[0].classList).toContain('day--past');
    expect(days[3].querySelector('.add-shift')?.getAttribute('aria-label')).toBe('Add a shift on Thursday 26 November');
    // Below the day's name, as its own element.
    const name = days[3].querySelector('.day-name')!;
    expect(name.querySelector('.add-shift')).toBeNull();
    expect(name.nextElementSibling?.classList).toContain('add-shift');
  });

  it('gives a member no "+", since only the supervisor adds shifts', () => {
    const fixture = render('MEMBER');
    flushWeek(fixture, '2026-W48');

    expect(element(fixture).querySelectorAll('.add-shift').length).toBe(0);
  });

  it('goes back a week with the arrow, and a past week cannot be changed', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48');

    click(fixture, '[aria-label="Previous week"]');
    flushWeek(fixture, '2026-W47');

    expect(title(fixture)).toBe('November 2026 - Week 47');
    expect(element(fixture).querySelectorAll('.add-shift').length).toBe(0);
    expect(element(fixture).querySelectorAll('.day--past').length).toBe(7);
    expect(element(fixture).querySelector('.week-note')?.textContent).toContain("can't be changed");
  });

  it('goes forward a week with the arrow, where every day is open', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48');

    click(fixture, '[aria-label="Next week"]');
    flushWeek(fixture, '2026-W49');

    expect(title(fixture)).toBe('December 2026 - Week 49');
    expect(element(fixture).querySelectorAll('.add-shift').length).toBe(7);
    expect(element(fixture).querySelector('.week-note')).toBeNull();
  });

  it('drops the answer for a week the user has already left', () => {
    const fixture = render('SUPERVISOR');
    const first = weekRequest();

    click(fixture, '[aria-label="Next week"]');
    expect(first.cancelled).toBe(true);
    flushWeek(fixture, '2026-W49');
  });

  it("opens a calendar under the title on the week's month, with the week chosen", () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48');
    expect(element(fixture).querySelector('.calendar')).toBeNull();

    click(fixture, '.week-toggle');

    expect(element(fixture).querySelector('.week-toggle')?.getAttribute('aria-expanded')).toBe('true');
    expect(element(fixture).querySelector('.calendar-month')?.textContent).toBe('November 2026');
    const rows = calendarRows(fixture);
    expect(rows.length).toBe(6);
    expect(rows[4].getAttribute('aria-label')).toBe('Week 48, November 2026 · Days 23 to 29');
    expect(rows[4].getAttribute('aria-pressed')).toBe('true');
    expect(rows.filter((row) => row.getAttribute('aria-pressed') === 'true').length).toBe(1);
  });

  it('chooses a week from another month in the calendar, and the calendar closes', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48');
    click(fixture, '.week-toggle');

    click(fixture, '[aria-label="Next month"]');
    expect(element(fixture).querySelector('.calendar-month')?.textContent).toBe('December 2026');
    // December 2026 starts on a Tuesday: its rows start on 30 November, then 7 December.
    calendarRows(fixture)[1].click();
    fixture.detectChanges();
    flushWeek(fixture, '2026-W50');

    expect(title(fixture)).toBe('December 2026 - Week 50');
    expect(element(fixture).querySelector('.calendar')).toBeNull();
  });

  it('comes back to this week from the calendar', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48');
    click(fixture, '[aria-label="Previous week"]');
    flushWeek(fixture, '2026-W47');

    click(fixture, '.week-toggle');
    click(fixture, '.calendar-this-week');
    flushWeek(fixture, '2026-W48');

    expect(title(fixture)).toBe('November 2026 - Week 48');
  });

  it('closes the calendar with Escape or a click outside it, without changing the week', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48');

    click(fixture, '.week-toggle');
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(element(fixture).querySelector('.calendar')).toBeNull();

    click(fixture, '.week-toggle');
    click(fixture, '[aria-label="Next month"]');
    expect(element(fixture).querySelector('.calendar')).not.toBeNull();
    document.body.click();
    fixture.detectChanges();
    expect(element(fixture).querySelector('.calendar')).toBeNull();
    expect(title(fixture)).toBe('November 2026 - Week 48');
  });

  it("puts each piquete in its day's column", () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48', [
      {
        id: 'p-thu',
        dutyDate: '2026-11-26',
        slots: [
          { id: 's1', role: 'DRIVER', status: 'OPEN' },
          { id: 's2', role: 'CREW_MEMBER', status: 'OPEN' },
        ],
      },
    ]);

    const days = element(fixture).querySelectorAll('.day');
    expect(days[3].querySelector('.shift')?.textContent).toContain('2 slots');
    expect(days[0].querySelector('.shift')).toBeNull();
  });

  it('shows the hours down the left side, every 2 hours', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48');

    const hours = Array.from(element(fixture).querySelectorAll('.hour')).map((hour) => hour.textContent);
    expect(hours.length).toBe(12);
    expect(hours[0]).toBe('00:00');
    expect(hours[1]).toBe('02:00');
    expect(hours[6]).toBe('12:00');
    expect(hours[11]).toBe('22:00');
  });

  it('places a shift at its hours, with "From … to …" on top', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48', [
      {
        id: 'p-noon',
        dutyDate: '2026-11-26',
        startsAt: '2026-11-26T12:00:00Z',
        endsAt: '2026-11-26T13:30:00Z',
        slots: [{ id: 's1', role: 'DRIVER', status: 'OPEN' }],
      },
    ]);

    const shift = element(fixture).querySelectorAll('.day')[3].querySelector<HTMLElement>('.day-body .shift')!;
    expect(shift.querySelector('.shift-time')?.textContent).toBe('From 12:00 to 13:30');
    expect(shift.textContent).toContain('1 slot');
    expect(shift.getAttribute('aria-label')).toBe('Shift from 12:00 to 13:30, 1 slot');
    expect(parseFloat(shift.style.top)).toBeCloseTo(50);
    expect(parseFloat(shift.style.height)).toBeCloseTo((90 / 1440) * 100);
  });

  it('shows a night shift on both days it covers, split at midnight', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48', [
      {
        id: 'p-night',
        dutyDate: '2026-11-26',
        startsAt: '2026-11-26T20:00:00Z',
        endsAt: '2026-11-27T08:00:00Z',
        slots: [],
      },
    ]);

    const days = element(fixture).querySelectorAll('.day');
    const thursday = days[3].querySelector<HTMLElement>('.day-body .shift')!;
    const friday = days[4].querySelector<HTMLElement>('.day-body .shift')!;
    expect(thursday.querySelector('.shift-time')?.textContent).toBe('From 20:00 to 08:00');
    expect(friday.querySelector('.shift-time')?.textContent).toBe('From 20:00 to 08:00');
    expect(friday.classList).toContain('shift--continued');
    expect(parseFloat(friday.style.top)).toBe(0);
  });

  it('lists a shift with no times yet under its day, as "Time not set"', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48', [{ id: 'p-old', dutyDate: '2026-11-24', slots: [] }]);

    const tuesday = element(fixture).querySelectorAll('.day')[1];
    expect(tuesday.querySelector('.day-head .shift')?.textContent).toContain('Time not set');
    expect(tuesday.querySelector('.day-body .shift')).toBeNull();
  });

  it('opens the add form on the day of the "+" that was clicked', async () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48');
    expect(element(fixture).querySelector('app-shift-form')).toBeNull();

    element(fixture).querySelectorAll<HTMLButtonElement>('.add-shift')[1].click(); // Thursday
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element(fixture).querySelector('#shift-form-title')?.textContent).toBe('Add a shift');
    expect(element(fixture).querySelector<HTMLInputElement>('#shift-day')?.value).toBe('2026-11-26');
  });

  it('reads the week again after a shift is saved, and closes the form', async () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48');
    element(fixture).querySelectorAll<HTMLButtonElement>('.add-shift')[0].click(); // Wednesday
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    element(fixture).querySelector<HTMLButtonElement>('.shift-form button[type="submit"]')!.click();
    http()
      .expectOne({ method: 'POST', url: '/api/v1/piquetes' })
      .flush({ id: 'p-new', dutyDate: '2026-11-25', slots: [] });
    flushWeek(fixture, '2026-W48', [
      {
        id: 'p-new',
        dutyDate: '2026-11-25',
        period: 'DAY',
        startsAt: '2026-11-25T08:00:00Z',
        endsAt: '2026-11-25T20:00:00Z',
        minimumCrew: 1,
        slots: [],
      },
    ]);

    expect(element(fixture).querySelector('app-shift-form')).toBeNull();
    expect(element(fixture).querySelectorAll('.day')[2].querySelector('.shift')?.textContent).toContain(
      'Minimum 1 firefighter',
    );
  });

  it('gives each shift from today on an Edit button that opens it, and none on a day that has passed', async () => {
    const fixture = render('SUPERVISOR');
    const shift = (id: string, day: string) => ({
      id,
      dutyDate: day,
      period: 'DAY',
      startsAt: `${day}T09:00:00Z`,
      endsAt: `${day}T13:00:00Z`,
      minimumCrew: 2,
      slots: [],
    });
    flushWeek(fixture, '2026-W48', [shift('p-mon', '2026-11-23'), shift('p-fri', '2026-11-27')]);

    const days = element(fixture).querySelectorAll('.day');
    expect(days[0].querySelector('.edit-shift')).toBeNull();
    const edit = days[4].querySelector<HTMLButtonElement>('.edit-shift')!;
    expect(edit.getAttribute('aria-label')).toBe('Edit the shift from 09:00 to 13:00 on Friday 27 November');

    edit.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(element(fixture).querySelector('#shift-form-title')?.textContent).toBe('Edit shift');
    expect(element(fixture).querySelector<HTMLInputElement>('#shift-crew')?.value).toBe('2');
  });

  it('gives a member no Edit button', () => {
    const fixture = render('MEMBER');
    flushWeek(fixture, '2026-W48', [{ id: 'p-fri', dutyDate: '2026-11-27', slots: [] }]);

    expect(element(fixture).querySelectorAll('.edit-shift').length).toBe(0);
  });

  it('says so when the week cannot be loaded', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48', 'fail', { status: 500, statusText: 'Server Error' });

    expect(element(fixture).querySelector('[role="alert"]')?.textContent).toContain(
      "Could not load the week's schedule.",
    );
  });

  it('shows the last loaded week when it cannot be loaded again (offline, RNF06)', () => {
    const piquete = { id: 'p-thu', dutyDate: '2026-11-26', slots: [{ id: 's1', role: 'DRIVER', status: 'OPEN' }] };
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48', [piquete]);

    click(fixture, '[aria-label="Next week"]');
    flushWeek(fixture, '2026-W49', 'fail', { status: 0, statusText: 'Unknown Error' });
    expect(element(fixture).querySelector('[role="alert"]')).not.toBeNull();

    click(fixture, '[aria-label="Previous week"]');
    flushWeek(fixture, '2026-W48', 'fail', { status: 0, statusText: 'Unknown Error' });
    expect(element(fixture).querySelector('[role="alert"]')).toBeNull();
    expect(element(fixture).textContent).toContain('Showing the last loaded roster');
    expect(element(fixture).querySelectorAll('.day')[3].querySelector('.shift')).not.toBeNull();
  });
});
