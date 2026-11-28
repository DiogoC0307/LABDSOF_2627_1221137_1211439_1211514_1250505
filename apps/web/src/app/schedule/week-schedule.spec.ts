import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { AuthService, LAST_PIQUETES_KEY } from '../auth/auth.service';
import { WeekSchedule } from './week-schedule';
import { SESSION_KEY, http, setValue, signInAs, testProviders, weekRequest } from '../testing/test-support';

/**
 * Schedule page
 *
 * The clock is fixed on Wednesday 2026-11-25, in week 2026-W48 (23 to 29 November): Monday and Tuesday have passed.
 */
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

  function buttonByText(root: ParentNode, text: string): HTMLButtonElement | undefined {
    return Array.from(root.querySelectorAll<HTMLButtonElement>('button')).find(
      (button) => button.textContent?.replace(/\s+/g, ' ').trim() === text,
    );
  }

  function press(fixture: ComponentFixture<WeekSchedule>, text: string) {
    buttonByText(document, text)!.click();
    fixture.detectChanges();
  }

  // The two lines of the week bar: "This week" over "23 – 29 November 2026"
  function title(fixture: ComponentFixture<WeekSchedule>) {
    const relative = element(fixture).querySelector('#week-relative')?.textContent?.trim();
    const range = element(fixture).querySelector('#week-range')?.textContent?.trim();
    return `${relative} / ${range}`;
  }

  function status(fixture: ComponentFixture<WeekSchedule>) {
    return element(fixture).querySelector('output.week-status')?.textContent?.trim();
  }

  function toggle(fixture: ComponentFixture<WeekSchedule>): HTMLButtonElement {
    return element(fixture).querySelector<HTMLButtonElement>('.week-toggle')!;
  }

  // The calendar opens in the CDK overlay container, at the end of the page, not inside the component
  function calendar(): HTMLElement | null {
    return document.querySelector<HTMLElement>('.calendar');
  }

  function clickInCalendar(fixture: ComponentFixture<WeekSchedule>, selector: string) {
    calendar()!.querySelector<HTMLElement>(selector)!.click();
    fixture.detectChanges();
  }

  function calendarRows() {
    return Array.from(calendar()?.querySelectorAll<HTMLButtonElement>('.calendar-week') ?? []);
  }

  function columns(fixture: ComponentFixture<WeekSchedule>) {
    return Array.from(element(fixture).querySelectorAll<HTMLElement>('.day-column'));
  }

  function slotsPerDay(fixture: ComponentFixture<WeekSchedule>) {
    return columns(fixture).map((column) => column.querySelectorAll('.add-slot').length);
  }

  function panel(fixture: ComponentFixture<WeekSchedule>) {
    return element(fixture).querySelector<HTMLElement>('[aria-label="Shift details"]');
  }

  // Shift side panel's role buttons
  function chooseRoles(fixture: ComponentFixture<WeekSchedule>, ...names: string[]) {
    for (const name of names) {
      const choice = Array.from(panel(fixture)!.querySelectorAll<HTMLButtonElement>('.role-choice')).find(
        (button) => button.textContent?.trim() === name,
      )!;
      choice.click();
      fixture.detectChanges();
    }
  }

  function dialog() {
    return document.querySelector<HTMLElement>('mat-dialog-container');
  }

  // Presses a role's button in the Add shift dialog
  function chooseInDialog(fixture: ComponentFixture<WeekSchedule>, name: string) {
    Array.from(dialog()!.querySelectorAll<HTMLButtonElement>('.role-choice'))
      .find((button) => button.textContent?.trim() === name)!
      .click();
    fixture.detectChanges();
  }

  // "+ Add shift" on a day of the week (0 is Monday), then the roles in the dialog, then "Create shift"
  async function addShift(fixture: ComponentFixture<WeekSchedule>, day: number, roles = ['Crew chief']) {
    columns(fixture)[day].querySelector<HTMLButtonElement>('.add-slot')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    for (const role of roles) {
      chooseInDialog(fixture, role);
    }
    press(fixture, 'Create shift');
    await fixture.whenStable();
    fixture.detectChanges();
    flushDirectory(fixture);
  }

  // Members as GET /api/v1/members answers: an inactive one and one off the active roster are never candidates
  const apiMembers = [
    { id: 'm-ana', fullName: 'Ana Costa', functions: ['CREW_CHIEF', 'CREW_MEMBER'] },
    { id: 'm-rui', fullName: 'Rui Almeida', functions: ['CREW_CHIEF', 'DRIVER', 'CREW_MEMBER'] },
    { id: 'm-joao', fullName: 'João Pereira', functions: ['DRIVER', 'CREW_MEMBER'] },
    { id: 'm-marta', fullName: 'Marta Silva', functions: ['DRIVER', 'CREW_MEMBER'] },
    { id: 'm-pedro', fullName: 'Pedro Santos', functions: ['CREW_MEMBER'] },
    { id: 'm-ines', fullName: 'Inês Rodrigues', functions: ['CREW_MEMBER'] },
    { id: 'm-sofia', fullName: 'Sofia Martins', functions: ['CREW_MEMBER'] },
    { id: 'm-carlos', fullName: 'Carlos Gomes', functions: ['CREW_CHIEF'] },
    { id: 'm-beatriz', fullName: 'Beatriz Lopes', functions: ['CREW_MEMBER'] },
    { id: 'm-old', fullName: 'Zé Inativo', active: false, functions: ['CREW_CHIEF'] },
    { id: 'm-admin', fullName: 'Synthetic Admin', rosterStatus: 'COMMAND_ROSTER', functions: ['CREW_CHIEF'] },
  ].map((member) => ({ active: true, rosterStatus: 'ACTIVE_ROSTER', ...member }));

  // Who said they can't come, as the worker gets it in each check: Ana all of Tuesday 1 December, Sofia only until 06:00
  const teamUnavailability = [
    { memberId: 'm-ana', startsAt: '2026-12-01T00:00:00Z', endsAt: '2026-12-02T00:00:00Z' },
    { memberId: 'm-sofia', startsAt: '2026-12-01T00:00:00Z', endsAt: '2026-12-01T06:00:00Z' },
  ];

  function isDirectoryRequest(req: { url: string }): boolean {
    return req.url === '/api/v1/members' || req.url === '/api/v1/candidate-checks';
  }

  function isCandidateCheck(req: { method: string; url: string }): boolean {
    return req.method === 'POST' && req.url === '/api/v1/candidate-checks';
  }

  interface CheckBody {
    startsAt: string;
    endsAt: string;
    role: string;
    otherShifts: { memberId: string; startsAt: string; endsAt: string }[];
  }

  /**
   * The eligibility worker's answer, as escala-api gives it (SCRUM-171), for every member with the role: RN01 off the
   * active roster, RN05 on another shift of the week at the same time, RN06 said they can't come.
   */
  function workerAnswer(body: CheckBody) {
    const start = Date.parse(body.startsAt);
    const end = Date.parse(body.endsAt);
    const during = (from: string, to: string) => Date.parse(from) < end && start < Date.parse(to);
    return {
      id: 'check-' + body.role,
      status: 'DONE',
      candidates: apiMembers
        .filter((member) => member.functions.includes(body.role))
        .map((member) => {
          const rules: string[] = [];
          if (!member.active || member.rosterStatus !== 'ACTIVE_ROSTER') {
            rules.push('RN01');
          }
          if (body.otherShifts.some((other) => other.memberId === member.id && during(other.startsAt, other.endsAt))) {
            rules.push('RN05');
          }
          if (teamUnavailability.some((period) => period.memberId === member.id && during(period.startsAt, period.endsAt))) {
            rules.push('RN06');
          }
          return { memberId: member.id, result: rules.length > 0 ? 'INELIGIBLE' : 'ELIGIBLE', rules };
        }),
    };
  }

  // Answers the members and the worker's candidate checks, asked when a supervisor opens a shift
  function flushDirectory(fixture?: ComponentFixture<WeekSchedule>) {
    http()
      .match('/api/v1/members')
      .forEach((request) => request.flush(apiMembers));
    http()
      .match(isCandidateCheck)
      .forEach((request) => request.flush(workerAnswer(request.request.body as CheckBody)));
    fixture?.detectChanges();
  }

  // A new shift on that day of the week shown, with one role, its panel open, the members loaded but nobody checked
  async function addShiftUnchecked(fixture: ComponentFixture<WeekSchedule>, day: number, role = 'Crew chief') {
    columns(fixture)[day].querySelector<HTMLButtonElement>('.add-slot')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    chooseInDialog(fixture, role);
    press(fixture, 'Create shift');
    await fixture.whenStable();
    fixture.detectChanges();
    http().match('/api/v1/members').forEach((request) => request.flush(apiMembers));
    fixture.detectChanges();
  }

  function publishButton(fixture: ComponentFixture<WeekSchedule>) {
    return buttonByText(element(fixture), 'Publish schedule')!;
  }

  // A shift on 2026-11-26 (Thursday) from 00:00 to 08:00 that needs one crew member
  const thursdayNight = {
    id: 'p-thu',
    dutyDate: '2026-11-26',
    period: 'NIGHT',
    startsAt: '2026-11-26T00:00:00Z',
    endsAt: '2026-11-26T08:00:00Z',
    minimumCrew: 1,
    slots: [{ id: 's1', role: 'CREW_MEMBER', status: 'PENDING' }],
  };

  const mondayDay = {
    id: 'p-mon',
    dutyDate: '2026-11-23',
    period: 'DAY',
    startsAt: '2026-11-23T08:00:00Z',
    endsAt: '2026-11-23T20:00:00Z',
    minimumCrew: 1,
    slots: [{ id: 's2', role: 'CREW_MEMBER', status: 'PENDING' }],
  };

  beforeEach(async () => {
    localStorage.clear();
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-11-25T10:00:00Z'));
    await TestBed.configureTestingModule({
      imports: [WeekSchedule],
      providers: [...testProviders, { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } }],
    }).compileComponents();
  });

  afterEach(() => {
    flushDirectory();
    http().verify();
    vi.useRealTimers();
    localStorage.clear();
  });

  describe('the week selector', () => {
    it("asks for this week's piquetes and names the week by its days, with no week number", () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48');

      expect(title(fixture)).toBe('This week / 23 – 29 November 2026');
      expect(toggle(fixture).getAttribute('aria-label')).toBe('This week, 23 – 29 November 2026. Choose week');
      expect(toggle(fixture).getAttribute('aria-haspopup')).toBe('dialog');
      expect(toggle(fixture).classList).toContain('week-toggle--current');
      expect(element(fixture).querySelector('.week-bar')?.textContent).not.toMatch(/Week \d|Wk/);
    });

    it('goes back and forward a week with the arrows', () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48');

      click(fixture, '[aria-label="Previous week"]');
      flushWeek(fixture, '2026-W47');
      expect(title(fixture)).toBe('Last week / 16 – 22 November 2026');

      click(fixture, '[aria-label="Next week"]');
      flushWeek(fixture, '2026-W48');
      click(fixture, '[aria-label="Next week"]');
      flushWeek(fixture, '2026-W49');
      expect(title(fixture)).toBe('Next week / 30 Nov – 6 Dec 2026');
      expect(calendar()).toBeNull();
    });

    it('drops the answer for a week the user has already left', () => {
      const fixture = render('SUPERVISOR');
      const first = weekRequest();

      click(fixture, '[aria-label="Next week"]');
      expect(first.cancelled).toBe(true);
      flushWeek(fixture, '2026-W49');
    });

    it("opens a calendar only from the date, on the week's month, with the week chosen and focused", async () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48');
      expect(calendar()).toBeNull();

      click(fixture, '.week-toggle');
      await fixture.whenStable();

      expect(toggle(fixture).getAttribute('aria-expanded')).toBe('true');
      expect(calendar()?.querySelector('.calendar-month')?.textContent?.trim()).toBe('November 2026');
      const rows = calendarRows();
      expect(rows.length).toBe(6);
      expect(rows[4].getAttribute('aria-label')).toBe('Week of 23 – 29 November 2026');
      expect(rows[4].getAttribute('aria-pressed')).toBe('true');
      expect(document.activeElement).toBe(rows[4]);
    });

    it('chooses a week from another month in the calendar, and the calendar closes', () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48');
      click(fixture, '.week-toggle');

      clickInCalendar(fixture, '[aria-label="Next month"]');
      calendarRows()[1].click();
      fixture.detectChanges();
      flushWeek(fixture, '2026-W50');

      expect(title(fixture)).toBe('In 2 weeks / 7 – 13 December 2026');
      expect(calendar()).toBeNull();
      expect(document.activeElement).toBe(toggle(fixture));
    });

    it('closes the calendar with Escape or a click outside it, without changing the week', () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48');

      click(fixture, '.week-toggle');
      calendar()!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
      fixture.detectChanges();
      expect(calendar()).toBeNull();

      click(fixture, '.week-toggle');
      document.querySelector<HTMLElement>('.cdk-overlay-backdrop')!.click();
      fixture.detectChanges();
      expect(calendar()).toBeNull();
      expect(title(fixture)).toBe('This week / 23 – 29 November 2026');
    });
  });

  describe('the grid', () => {
    it('shows the seven days, today as the current date, and the hours every 2 hours', () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48');

      const headers = Array.from(element(fixture).querySelectorAll('.day-header')).map((day) => day.textContent);
      expect(headers).toEqual(['Mon 23', 'Tue 24', 'Wed 25', 'Thu 26', 'Fri 27', 'Sat 28', 'Sun 29']);
      expect(element(fixture).querySelector('.day-header--today')?.getAttribute('aria-current')).toBe('date');
      expect(element(fixture).querySelectorAll('.day-header--past').length).toBe(2);
      expect(element(fixture).querySelectorAll('.day-column--past').length).toBe(2);
      const hours = Array.from(element(fixture).querySelectorAll('.hour-label')).map((hour) => hour.textContent);
      expect(hours.length).toBe(12);
      expect(hours[0]).toBe('00:00');
      expect(hours[11]).toBe('22:00');
    });

    it('places a shift as a card at its hours, with how many people it has and needs', () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48', [thursdayNight]);

      const card = columns(fixture)[3].querySelector<HTMLButtonElement>('.shift-card')!;
      expect(card.getAttribute('aria-label')).toBe('Thursday 00:00 to 08:00, 0 of 1 firefighter');
      expect(card.textContent).toContain('00:00 – 08:00');
      expect(card.textContent).toContain('Needs 1 crew member');
      expect(card.classList).toContain('shift-card--short');
      expect(card.style.top).toBe('calc(0% + 2px)');
      expect(card.style.height).toMatch(/^calc\(33\.33\d*% - 4px\)$/);
      expect(card.querySelector('button')).toBeNull();
    });

    it('greys a shift on a day that has passed', () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48', [mondayDay]);

      expect(columns(fixture)[0].querySelector('.shift-card')?.classList).toContain('shift-card--past');
    });
  });

  describe('this week', () => {
    it('is read-only until "Edit schedule", with nothing to publish', () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48', [thursdayNight]);

      expect(status(fixture)).toBe('Published');
      expect(element(fixture).querySelector('.status-dot--green')).not.toBeNull();
      expect(element(fixture).querySelectorAll('.add-slot').length).toBe(0);
      expect(element(fixture).querySelectorAll('.day-add').length).toBe(0);
      expect(publishButton(fixture).disabled).toBe(true);

      press(fixture, 'Edit schedule');
      expect(status(fixture)).toBe('Editing · no changes yet');
      // Slots only today and later; Thursday has a slot after its night shift.
      expect(slotsPerDay(fixture)).toEqual([0, 0, 1, 1, 1, 1, 1]);
      // Monday and Tuesday have passed: no Add shift button on them either.
      expect(
        Array.from(element(fixture).querySelectorAll('.day-add')).map((button) => button.getAttribute('aria-label')),
      ).toEqual([
        'Add shift on Wednesday 25 November',
        'Add shift on Thursday 26 November',
        'Add shift on Friday 27 November',
        'Add shift on Saturday 28 November',
        'Add shift on Sunday 29 November',
      ]);
      expect(buttonByText(element(fixture), 'Stop editing')).toBeDefined();

      press(fixture, 'Stop editing');
      expect(element(fixture).querySelectorAll('.add-slot').length).toBe(0);
      expect(buttonByText(element(fixture), 'Edit schedule')).toBeDefined();
    });

    it('says "No shifts yet" for an empty week', () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48');

      expect(status(fixture)).toBe('No shifts yet');
    });

    it('opens a shift read-only in the panel, saying why', () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48', [thursdayNight, mondayDay]);

      const thursday = columns(fixture)[3].querySelector<HTMLButtonElement>('.shift-card')!;
      thursday.click();
      fixture.detectChanges();
      expect(thursday.getAttribute('aria-pressed')).toBe('true');
      expect(panel(fixture)?.textContent).toContain('This week is published. Press "Edit schedule" to make changes.');
      expect(panel(fixture)?.textContent).toContain('Thursday 26 November');
      expect(buttonByText(panel(fixture)!, 'Delete')).toBeUndefined();
      expect(panel(fixture)?.querySelector('.needed-input')).toBeNull();
      expect(panel(fixture)?.textContent).toContain('1 needed');

      press(fixture, 'Edit schedule');
      columns(fixture)[0].querySelector<HTMLButtonElement>('.shift-card')!.click();
      fixture.detectChanges();
      expect(panel(fixture)?.textContent).toContain("This shift has already happened, so it can't be changed.");
      expect(buttonByText(panel(fixture)!, 'Delete')).toBeUndefined();
    });

    it('closes the panel and gives the focus back to the card', async () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48', [thursdayNight]);
      const card = columns(fixture)[3].querySelector<HTMLButtonElement>('.shift-card')!;
      card.click();
      fixture.detectChanges();

      click(fixture, '[aria-label="Close shift details"]');
      await fixture.whenStable();
      expect(panel(fixture)).toBeNull();
      expect(document.activeElement?.getAttribute('data-shift-id')).toBe('p-thu');
    });
  });

  describe('a later week', () => {
    async function nextWeek() {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48');
      click(fixture, '[aria-label="Next week"]');
      flushWeek(fixture, '2026-W49');
      return fixture;
    }

    it('is always editable, with a slot over every empty day', async () => {
      const fixture = await nextWeek();

      expect(slotsPerDay(fixture)).toEqual([1, 1, 1, 1, 1, 1, 1]);
      const slot = columns(fixture)[0].querySelector('.add-slot')!;
      expect(slot.getAttribute('aria-label')).toBe('Add shift on Monday, 00:00 to 24:00');
      expect(buttonByText(element(fixture), 'Edit schedule')).toBeUndefined();
      expect(element(fixture).querySelectorAll('.day-add').length).toBe(7);
    });

    it('asks for the times and roles of a new shift, then opens it and counts the change until it is discarded', async () => {
      const fixture = await nextWeek();
      columns(fixture)[0].querySelector<HTMLButtonElement>('.add-slot')!.click();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(dialog()?.textContent).toContain('Add shift');
      expect(dialog()?.textContent).toContain('Monday 30 November');
      const box = (name: string) =>
        [...dialog()!.querySelectorAll('mat-form-field')]
          .find((field) => field.querySelector('mat-label')?.textContent?.trim() === name)!
          .querySelector<HTMLInputElement>('input')!;
      expect(box('Start').value).toBe('08:00');
      expect(box('End').value).toBe('20:00');
      // No role is chosen yet: nothing to create.
      expect(dialog()?.querySelector('[aria-label$=" needed"]')).toBeNull();
      expect(dialog()?.textContent).toContain('Choose at least one role.');
      expect(buttonByText(dialog()!, 'Create shift')!.disabled).toBe(true);

      // A new start keeps the end.
      setValue(box('Start'), '9');
      box('Start').dispatchEvent(new Event('blur'));
      fixture.detectChanges();
      expect(box('End').value).toBe('20:00');
      chooseInDialog(fixture, 'Crew chief');
      chooseInDialog(fixture, 'Driver');
      chooseInDialog(fixture, 'Crew member');
      chooseInDialog(fixture, 'Driver');
      const needed = (role: string) => dialog()!.querySelector<HTMLInputElement>(`[aria-label="${role} needed"]`);
      expect(needed('Driver')).toBeNull();
      expect(needed('Crew chief')!.value).toBe('1');
      setValue(needed('Crew member')!, '3a');
      fixture.detectChanges();
      expect(needed('Crew member')!.value).toBe('3');
      // 0 changes nothing: the role's button takes the role off.
      setValue(needed('Crew member')!, '0');
      needed('Crew member')!.dispatchEvent(new Event('blur'));
      fixture.detectChanges();
      expect(needed('Crew member')!.value).toBe('3');

      press(fixture, 'Create shift');
      await fixture.whenStable();
      fixture.detectChanges();
      expect(dialog()).toBeNull();
      const card = columns(fixture)[0].querySelector<HTMLButtonElement>('.shift-card')!;
      expect(card.textContent).toContain('09:00 – 20:00');
      expect(card.textContent).toContain('0 of 4 firefighters');
      expect(card.textContent).toContain('New');
      expect(card.getAttribute('aria-pressed')).toBe('true');
      expect(panel(fixture)?.textContent).toContain('09:00 – 20:00');
      // Before and after the new shift.
      expect(columns(fixture)[0].querySelectorAll('.add-slot').length).toBe(2);
      expect(status(fixture)).toBe('1 unpublished change');
      expect(element(fixture).querySelector('.status-dot--amber')).not.toBeNull();
      expect(publishButton(fixture).disabled).toBe(false);

      press(fixture, 'Discard changes');
      expect(columns(fixture)[0].querySelector('.shift-card')).toBeNull();
      expect(panel(fixture)).toBeNull();
      expect(status(fixture)).toBe('No shifts yet');
    });

    it('assigns people role by role, and the card turns full', async () => {
      const fixture = await nextWeek();
      await addShift(fixture, 0, ['Crew chief', 'Driver', 'Crew member']);

      const needed = (role: string) => panel(fixture)!.querySelector<HTMLInputElement>(`[aria-label="${role} needed"]`);
      expect(needed('Crew chief')!.type).toBe('text');
      expect(needed('Crew chief')!.inputMode).toBe('numeric');
      expect(needed('Crew member')!.value).toBe('1');
      setValue(needed('Crew member')!, '3a');
      fixture.detectChanges();
      expect(needed('Crew member')!.value).toBe('3');
      setValue(needed('Crew member')!, '0'); // 0 changes nothing: the role's button takes it off.
      needed('Crew member')!.dispatchEvent(new Event('blur'));
      fixture.detectChanges();
      expect(needed('Crew member')!.value).toBe('3');
      setValue(needed('Crew member')!, '1');
      fixture.detectChanges();
      chooseRoles(fixture, 'Driver');
      expect(needed('Driver')).toBeNull();
      expect(panel(fixture)?.querySelector('[aria-pressed="false"]')?.textContent?.trim()).toBe('Driver');

      click(fixture, '[aria-label="Assign Ana Costa as Crew chief"]');
      expect(panel(fixture)?.textContent).toContain('1 of 1 · Full');
      
      // Ana is on the shift now: she is no longer a candidate for another role.
      expect(element(fixture).querySelector('[aria-label="Assign Ana Costa as Crew member"]')).toBeNull();

      const card = () => columns(fixture)[0].querySelector<HTMLButtonElement>('.shift-card')!;
      expect(card().classList).toContain('shift-card--short');
      click(fixture, '[aria-label="Assign Beatriz Lopes as Crew member"]');
      expect(card().classList).toContain('shift-card--full');
      expect(card().textContent).toContain('2 of 2 firefighters');
      expect(card().textContent).toContain('Full');

      click(fixture, '[aria-label="Remove Beatriz Lopes as Crew member"]');
      expect(card().classList).toContain('shift-card--short');
    });

    it("adds a shift from the day's own button, also on a day all covered and over its shifts", async () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48');
      click(fixture, '[aria-label="Next week"]');
      const allDay = {
        ...mondayDay,
        id: 'p-all',
        dutyDate: '2026-11-30',
        startsAt: '2026-11-30T00:00:00Z',
        endsAt: '2026-12-01T00:00:00Z',
      };
      flushWeek(fixture, '2026-W49', [allDay]);

      // Monday is all covered: no free time, but its own button.
      expect(slotsPerDay(fixture)).toEqual([0, 1, 1, 1, 1, 1, 1]);
      click(fixture, '[aria-label="Add shift on Monday 30 November"]');
      await fixture.whenStable();
      expect(dialog()?.textContent).toContain('Monday 30 November');
      chooseInDialog(fixture, 'Driver');
      press(fixture, 'Create shift');
      await fixture.whenStable();
      fixture.detectChanges();

      const cards = columns(fixture)[0].querySelectorAll('.shift-card');
      expect(cards.length).toBe(2);
      expect(cards[1].textContent).toContain('08:00 – 20:00');
      expect(status(fixture)).toBe('1 unpublished change');
    });

    it('adds nothing when the dialog is cancelled, or while a time in it is wrong', async () => {
      const fixture = await nextWeek();
      columns(fixture)[2].querySelector<HTMLButtonElement>('.add-slot')!.click();
      fixture.detectChanges();
      await fixture.whenStable();
      chooseInDialog(fixture, 'Driver');
      const start = dialog()!.querySelector<HTMLInputElement>('mat-form-field input')!;
      setValue(start, 'soon');
      start.dispatchEvent(new Event('blur'));
      fixture.detectChanges();
      expect(dialog()?.textContent).toContain('Write a time like 08:30');
      expect(buttonByText(dialog()!, 'Create shift')!.disabled).toBe(true);

      press(fixture, 'Cancel');
      await fixture.whenStable();
      fixture.detectChanges();
      expect(dialog()).toBeNull();
      expect(columns(fixture)[2].querySelector('.shift-card')).toBeNull();
      expect(status(fixture)).toBe('No shifts yet');
    });

    it('takes a written time, not only the whole hours of the list, and says when it is not a time', async () => {
      const fixture = await nextWeek();
      await addShift(fixture, 0);

      const box = (name: string) =>
        [...panel(fixture)!.querySelectorAll('mat-form-field')]
          .find((field) => field.querySelector('mat-label')?.textContent?.trim() === name)!
          .querySelector<HTMLInputElement>('input')!;
      const write = (name: string, text: string) => {
        setValue(box(name), text);
        box(name).dispatchEvent(new Event('blur'));
        fixture.detectChanges();
      };

      expect(box('Start').value).toBe('08:00');
      write('Start', '8:30');
      // The shift keeps its end.
      expect(box('Start').value).toBe('08:30');
      expect(box('End').value).toBe('20:00');
      expect(panel(fixture)?.textContent).toContain('08:30 – 20:00');

      write('End', '615');
      expect(box('End').value).toBe('06:15 (next day)');
      expect(panel(fixture)?.textContent).toContain('08:30 – 06:15');

      // The end keeps its time (06:15): the next day's while the start is later, the same day's once it is earlier.
      write('Start', '7');
      expect(box('End').value).toBe('06:15 (next day)');
      write('Start', '5');
      expect(box('End').value).toBe('06:15');
      write('Start', '8:30');
      expect(box('End').value).toBe('06:15 (next day)');

      write('Start', 'soon');
      expect(panel(fixture)?.textContent).toContain('Write a time like 08:30');
      expect(panel(fixture)?.textContent).toContain('08:30 – 06:15');
    });

    it('only shows the roles the shift needs, and keeps at least one', async () => {
      const fixture = await nextWeek();
      await addShift(fixture, 0);

      const choice = (name: string) =>
        Array.from(panel(fixture)!.querySelectorAll<HTMLButtonElement>('.role-choice')).find(
          (button) => button.textContent?.trim() === name,
        )!;
      expect(panel(fixture)?.querySelector('[aria-label="Crew chief needed"]')).not.toBeNull();
      expect(panel(fixture)?.querySelector('[aria-label="Driver needed"]')).toBeNull();
      // Its only role can't be taken off.
      expect(choice('Crew chief').disabled).toBe(true);
      expect(panel(fixture)?.textContent).toContain('A shift needs at least one role.');

      chooseRoles(fixture, 'Driver');
      expect(choice('Crew chief').disabled).toBe(false);
      chooseRoles(fixture, 'Crew chief');
      expect(panel(fixture)?.querySelector('[aria-label="Crew chief needed"]')).toBeNull();
      expect(choice('Driver').disabled).toBe(true);
      expect(columns(fixture)[0].querySelector('.shift-card')?.textContent).toContain('0 of 1 firefighter');
    });

    it("shows who can't come during the shift and leaves them out of the candidates", async () => {
      const fixture = await nextWeek();
      // Tuesday 1 December, 08:00 to 20:00: Ana can't come all day, Sofia only until 06:00.
      await addShift(fixture, 1);

      const cantCome = panel(fixture)!.querySelector('[aria-labelledby="cant-come-title"]')!;
      expect(cantCome.textContent).toContain("Can't come during this shift");
      expect(cantCome.textContent).toContain('Ana Costa');
      expect(cantCome.textContent).not.toContain('Sofia Martins');
      expect(element(fixture).querySelector('[aria-label="Assign Ana Costa as Crew chief"]')).toBeNull();
      expect(element(fixture).querySelector('[aria-label="Assign Rui Almeida as Crew chief"]')).not.toBeNull();
      // Inactive members and members off the active roster are never candidates (RN01).
      expect(panel(fixture)?.textContent).not.toContain('Zé Inativo');
      expect(panel(fixture)?.textContent).not.toContain('Synthetic Admin');
    });

    it('shows "Checking who can come…" until the eligibility worker answers, then only who it found eligible (SCRUM-171)', async () => {
      const fixture = await nextWeek();
      await addShiftUnchecked(fixture, 1);

      expect(panel(fixture)?.querySelector('output.candidate-check')?.textContent).toContain('Checking who can come…');
      expect(panel(fixture)?.querySelector('[aria-label^="Assign "]')).toBeNull();
      const check = http().expectOne(isCandidateCheck);
      expect(check.request.body).toEqual({
        startsAt: '2026-12-01T08:00:00Z',
        endsAt: '2026-12-01T20:00:00Z',
        role: 'CREW_CHIEF',
        otherShifts: [],
      });

      check.flush(workerAnswer(check.request.body as CheckBody));
      fixture.detectChanges();
      expect(panel(fixture)?.textContent).not.toContain('Checking who can come…');
      expect(element(fixture).querySelector('[aria-label="Assign Rui Almeida as Crew chief"]')).not.toBeNull();
      expect(element(fixture).querySelector('[aria-label="Assign Ana Costa as Crew chief"]')).toBeNull();
    });

    it('asks the worker with who is on the other shifts of the week, and offers nobody it refuses (RN05)', async () => {
      const fixture = await nextWeek();
      await addShift(fixture, 0);
      click(fixture, '[aria-label="Assign Rui Almeida as Crew chief"]');

      await addShiftUnchecked(fixture, 1);
      const check = http().expectOne(isCandidateCheck);
      expect(check.request.body.otherShifts).toEqual([
        { memberId: 'm-rui', startsAt: '2026-11-30T08:00:00Z', endsAt: '2026-11-30T20:00:00Z' },
      ]);
      // The worker refuses Rui for this shift (as it would for one at the same time)
      const answer = workerAnswer(check.request.body as CheckBody);
      check.flush({
        ...answer,
        candidates: answer.candidates.map((candidate) =>
          candidate.memberId === 'm-rui' ? { ...candidate, result: 'INELIGIBLE', rules: ['RN05'] } : candidate,
        ),
      });
      fixture.detectChanges();
      expect(element(fixture).querySelector('[aria-label="Assign Rui Almeida as Crew chief"]')).toBeNull();
      expect(element(fixture).querySelector('[aria-label="Assign Carlos Gomes as Crew chief"]')).not.toBeNull();
      // On another shift is not "can't come"
      expect(panel(fixture)?.querySelector('[aria-labelledby="cant-come-title"]')?.textContent ?? '').not.toContain('Rui Almeida');
    });

    it('says so when the worker could not be asked, and asks again on "Check again"', async () => {
      const fixture = await nextWeek();
      await addShiftUnchecked(fixture, 2);

      http().expectOne(isCandidateCheck).flush(null, { status: 503, statusText: 'Service Unavailable' });
      fixture.detectChanges();
      expect(panel(fixture)?.textContent).toContain("Couldn't check who can come.");
      expect(panel(fixture)?.querySelector('[aria-label^="Assign "]')).toBeNull();

      click(fixture, '[aria-label="Check again who can be Crew chief"]');
      expect(panel(fixture)?.textContent).toContain('Checking who can come…');
      flushDirectory(fixture);
      expect(element(fixture).querySelector('[aria-label="Assign Rui Almeida as Crew chief"]')).not.toBeNull();
    });

    it('says so when the members cannot be loaded', async () => {
      const fixture = await nextWeek();
      columns(fixture)[0].querySelector<HTMLButtonElement>('.add-slot')!.click();
      fixture.detectChanges();
      await fixture.whenStable();
      chooseInDialog(fixture, 'Crew chief');
      press(fixture, 'Create shift');
      await fixture.whenStable();
      fixture.detectChanges();

      http().expectOne('/api/v1/members').flush(null, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();
      expect(panel(fixture)?.textContent).toContain('Could not load the members.');
    });

    it('shows three candidates, then the rest on "Show N more"', async () => {
      const fixture = await nextWeek();
      await addShift(fixture, 2, ['Crew member']);

      const crewMember = () =>
        panel(fixture)!.querySelectorAll('[aria-label^="Assign "][aria-label$=" as Crew member"]');
      expect(crewMember().length).toBe(3);
      const more = panel(fixture)!.querySelector<HTMLButtonElement>('[aria-label$="more who can be Crew member"]')!;
      expect(more.textContent?.trim()).toMatch(/^Show \d+ more$/);
      more.click();
      fixture.detectChanges();
      expect(crewMember().length).toBeGreaterThan(3);
    });

    it('asks before deleting a shift; a new one deleted is no change', async () => {
      const fixture = await nextWeek();
      await addShift(fixture, 0);

      press(fixture, 'Delete');
      await fixture.whenStable();
      const dialog = document.querySelector<HTMLElement>('mat-dialog-container')!;
      expect(dialog.getAttribute('role')).toBe('alertdialog');
      expect(dialog.textContent).toContain('Delete this shift?');
      expect(dialog.textContent).toContain('Monday 30 November, 08:00 – 20:00.');
      expect(dialog.textContent).not.toContain('will be taken off');

      press(fixture, 'Delete shift');
      await fixture.whenStable();
      fixture.detectChanges();
      expect(columns(fixture)[0].querySelector('.shift-card')).toBeNull();
      expect(panel(fixture)).toBeNull();
      expect(status(fixture)).toBe('No shifts yet');
    });

    it('asks before publishing, then shows the week as published', async () => {
      const fixture = await nextWeek();
      await addShift(fixture, 0);
      await addShift(fixture, 1);

      publishButton(fixture).click();
      fixture.detectChanges();
      await fixture.whenStable();
      const dialog = document.querySelector<HTMLElement>('mat-dialog-container')!;
      expect(dialog.textContent).toContain("Publish this week's schedule?");
      expect(dialog.textContent).toContain('30 Nov – 6 Dec 2026 · 2 unpublished changes.');

      press(fixture, 'Publish');
      await fixture.whenStable();
      fixture.detectChanges();
      expect(status(fixture)).toBe('Published');
      expect(publishButton(fixture).disabled).toBe(true);
      expect(element(fixture).querySelector('.tag--change')).toBeNull();
    });

    it('keeps the changes when the supervisor looks at another week and comes back', async () => {
      const fixture = await nextWeek();
      await addShift(fixture, 0);

      click(fixture, '[aria-label="Previous week"]');
      flushWeek(fixture, '2026-W48');
      click(fixture, '[aria-label="Next week"]');
      flushWeek(fixture, '2026-W49', [thursdayNight]);

      expect(status(fixture)).toBe('1 unpublished change');
      expect(columns(fixture)[0].querySelector('.shift-card')?.textContent).toContain('New');
    });
  });

  describe('a past week', () => {
    it("can't be changed", () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48');
      click(fixture, '[aria-label="Previous week"]');
      flushWeek(fixture, '2026-W47');

      expect(status(fixture)).toBe("Past week · can't be changed");
      expect(publishButton(fixture).disabled).toBe(true);
      expect(buttonByText(element(fixture), 'Edit schedule')).toBeUndefined();
      expect(element(fixture).querySelectorAll('.add-slot').length).toBe(0);
    });
  });

  it("never shows a member the supervisor's unpublished changes made on this device", async () => {
    const supervisor = render('SUPERVISOR');
    flushWeek(supervisor, '2026-W48');
    click(supervisor, '[aria-label="Next week"]');
    flushWeek(supervisor, '2026-W49');
    await addShift(supervisor, 1);
    expect(element(supervisor).querySelectorAll('.shift-card').length).toBe(1);
    supervisor.destroy();

    // The supervisor signs out and a member signs in, without the page being reloaded.
    signInAs('MEMBER');
    TestBed.inject(AuthService).session.set(JSON.parse(localStorage.getItem(SESSION_KEY)!));
    const member = TestBed.createComponent(WeekSchedule);
    member.detectChanges();
    flushWeek(member, '2026-W48');
    click(member, '[aria-label="Next week"]');
    flushWeek(member, '2026-W49');

    expect(element(member).querySelectorAll('.shift-card').length).toBe(0);
    expect(element(member).textContent).not.toContain('unpublished');
  });

  it('gives a member neither "Edit schedule" nor "Publish schedule"', () => {
    const fixture = render('MEMBER');
    flushWeek(fixture, '2026-W48', [thursdayNight]);

    expect(buttonByText(element(fixture), 'Edit schedule')).toBeUndefined();
    expect(buttonByText(element(fixture), 'Publish schedule')).toBeUndefined();
    click(fixture, '[aria-label="Next week"]');
    flushWeek(fixture, '2026-W49');
    expect(element(fixture).querySelectorAll('.add-slot').length).toBe(0);

    // A member opens a shift without asking for the members or who can't come (supervisor only).
    click(fixture, '[aria-label="Previous week"]');
    flushWeek(fixture, '2026-W48', [thursdayNight]);
    click(fixture, '[data-shift-id="p-thu"]');
    expect(http().match(isDirectoryRequest).length).toBe(0);
  });

  describe('on a phone', () => {
    beforeEach(() => {
      // Narrower than 64rem: the media query for a desktop does not match.
      vi.stubGlobal('matchMedia', (query: string) => ({
        matches: false,
        media: query,
        addEventListener: () => undefined,
        removeEventListener: () => undefined,
      }));
    });

    afterEach(() => vi.unstubAllGlobals());

    it('lists the days, each with its shifts in order and a night shift only on its first day', () => {
      const fixture = render('SUPERVISOR');
      const tuesdayNight = {
        ...mondayDay,
        id: 'p-tue',
        dutyDate: '2026-11-24',
        startsAt: '2026-11-24T20:00:00Z',
        endsAt: '2026-11-25T08:00:00Z',
      };
      flushWeek(fixture, '2026-W48', [thursdayNight, tuesdayNight]);

      expect(columns(fixture)).toEqual([]);
      const days = Array.from(element(fixture).querySelectorAll<HTMLElement>('.day-list-item'));
      expect(days.map((day) => day.getAttribute('aria-label'))[0]).toBe('Monday 23 November');
      expect(days[1].querySelector('.shift-card')?.textContent).toContain('20:00 – 08:00');
      expect(days[2].querySelector('.shift-card')).toBeNull();
      expect(days[2].textContent).toContain('No shifts');
      expect(days[2].querySelector('h3')?.textContent).toContain('Today');
      expect(days[3].querySelector('.shift-card')?.textContent).toContain('00:00 – 08:00');
    });

    it("shows today's shifts first in this week, and not in another week", () => {
      const fixture = render('SUPERVISOR');
      const wednesday = {
        ...thursdayNight,
        id: 'p-wed',
        dutyDate: '2026-11-25',
        startsAt: '2026-11-25T08:00:00Z',
        endsAt: '2026-11-25T20:00:00Z',
      };
      flushWeek(fixture, '2026-W48', [thursdayNight, wednesday]);

      const today = element(fixture).querySelector<HTMLElement>('[aria-labelledby="today-heading"]')!;
      expect(today.textContent).toContain('Today');
      expect(today.textContent).toContain('Wednesday 25 November');
      expect(
        Array.from(today.querySelectorAll('.shift-card')).map((card) => card.getAttribute('data-shift-id')),
      ).toEqual(['p-wed']);
      // Then the whole week, today in it too.
      expect(today.compareDocumentPosition(element(fixture).querySelector('.day-list')!)).toBe(
        Node.DOCUMENT_POSITION_FOLLOWING,
      );
      expect(element(fixture).querySelectorAll('.day-list .shift-card').length).toBe(2);

      click(fixture, '[aria-label="Next week"]');
      flushWeek(fixture, '2026-W49');
      expect(element(fixture).querySelector('[aria-labelledby="today-heading"]')).toBeNull();
      // A later week can always change: each day has its own Add shift button.
      expect(element(fixture).querySelectorAll('.day-list .day-add-text').length).toBe(7);
    });

    it('opens the shift in a drawer that a tap outside or Escape closes', () => {
      const fixture = render('SUPERVISOR');
      flushWeek(fixture, '2026-W48', [thursdayNight]);

      click(fixture, '[data-shift-id="p-thu"]');
      expect(element(fixture).querySelector('.shift-aside--drawer')).not.toBeNull();
      click(fixture, '.drawer-backdrop');
      expect(panel(fixture)).toBeNull();

      click(fixture, '[data-shift-id="p-thu"]');
      panel(fixture)!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
      fixture.detectChanges();
      expect(panel(fixture)).toBeNull();
    });
  });

  it('says so when the week cannot be loaded', () => {
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48', 'fail', { status: 500, statusText: 'Server Error' });

    expect(element(fixture).querySelector('[role="alert"]')?.textContent).toContain(
      "Could not load the week's schedule.",
    );
  });

  it('shows the last loaded week when it cannot be loaded again (offline, RNF06)', () => {
    // Kept from an earlier visit; the API cannot be reached now.
    localStorage.setItem(LAST_PIQUETES_KEY, JSON.stringify({ week: '2026-W48', piquetes: [thursdayNight] }));
    const fixture = render('SUPERVISOR');
    flushWeek(fixture, '2026-W48', 'fail', { status: 0, statusText: 'Unknown Error' });

    expect(element(fixture).querySelector('[role="alert"]')).toBeNull();
    expect(element(fixture).textContent).toContain('Showing the last loaded roster');
    expect(columns(fixture)[3].querySelector('.shift-card')).not.toBeNull();
  });
});
