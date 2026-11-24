import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { App } from '../app';
import { http, setValue, signInAs, testProviders } from '../testing/test-support';

const MESSAGES = '/api/v1/unavailability-messages';
const SETTINGS = `${MESSAGES}/settings`;
const id = '75000000-0000-4000-8000-000000000001';

const saturdayMorning = { startDate: '2026-11-28', endDate: '2026-11-28', startTime: '08:00', endTime: '13:00' };

const PENDING_VIEW = { id, status: 'PENDING', referenceDate: '2026-11-22', method: null, periods: [] };

function answer(periods: object[], method = 'MODEL') {
  return { id, status: 'PROPOSED', referenceDate: '2026-11-22', method, periods };
}

/** "When I can't", in my own words: send, see what was read, confirm or discard (SCRUM-75). Synthetic data only. */
describe('Unavailability in my own words', () => {
  beforeEach(async () => {
    localStorage.clear();
    signInAs('MEMBER');
    await TestBed.configureTestingModule({ imports: [App], providers: testProviders }).compileComponents();
  });

  afterEach(() => {
    http().verify();
    localStorage.clear();
  });

  async function start(aiEnabled = true) {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl('/unavailability');
    fixture.detectChanges();
    http().expectOne('/api/v1/unavailabilities').flush([]);
    http().expectOne(SETTINGS).flush({ aiEnabled });
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }

  function page(fixture: ComponentFixture<App>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function input(fixture: ComponentFixture<App>, name: string): HTMLInputElement {
    return page(fixture).querySelector<HTMLInputElement>(`.proposal [name="${name}"]`)!;
  }

  async function send(fixture: ComponentFixture<App>, text: string) {
    const box = page(fixture).querySelector<HTMLTextAreaElement>('textarea[name="text"]')!;
    box.value = text;
    box.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    page(fixture).querySelector<HTMLButtonElement>('.message button.send')!.click();
    fixture.detectChanges();
  }

  async function sendAndAnswer(fixture: ComponentFixture<App>, periods: object[], method = 'MODEL') {
    await send(fixture, 'não posso sábado de manhã');
    http().expectOne({ method: 'POST', url: MESSAGES }).flush({ id, status: 'PENDING', referenceDate: '2026-11-22' },
      { status: 202, statusText: 'Accepted' });
    fixture.detectChanges();
    http().expectOne(`${MESSAGES}/${id}`).flush(answer(periods, method));
    fixture.detectChanges();
    await fixture.whenStable();
  }

  it('TC-SCRUM-75-18 the period read from "não posso sábado de manhã" appears in editable fields labelled "read automatically", and nothing is saved before Confirm', async () => {
    const fixture = await start();
    await send(fixture, 'não posso sábado de manhã');

    const post = http().expectOne({ method: 'POST', url: MESSAGES });
    expect(post.request.body).toEqual({ text: 'não posso sábado de manhã' });
    post.flush({ id, status: 'PENDING', referenceDate: '2026-11-22' }, { status: 202, statusText: 'Accepted' });
    fixture.detectChanges();
    expect(page(fixture).querySelector('.message output')?.textContent).toContain('Reading');

    http().expectOne(`${MESSAGES}/${id}`).flush(answer([saturdayMorning]));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(page(fixture).querySelector('.proposal')?.textContent).toContain('Read automatically');
    expect(page(fixture).querySelector('.proposal fieldset.weekdays > legend')?.textContent).toContain('Only on');
    expect(input(fixture, 'startDate-0').value).toBe('2026-11-28');
    expect(input(fixture, 'endDate-0').value).toBe('2026-11-28');
    expect(input(fixture, 'startTime-0').value).toBe('08:00');
    expect(input(fixture, 'endTime-0').value).toBe('13:00');
    expect(input(fixture, 'startDate-0').disabled).toBe(false);
    http().expectNone({ method: 'POST', url: '/api/v1/unavailabilities' });
    http().expectNone({ method: 'POST', url: `${MESSAGES}/${id}/confirm` });
  });

  it('TC-SCRUM-75-19 the member edits the pattern and confirms it: the pattern is sent as edited, and the list is loaded again', async () => {
    const fixture = await start();
    await sendAndAnswer(fixture, [saturdayMorning]);

    setValue(input(fixture, 'endTime-0'), '12:00');
    input(fixture, 'weekday-0-SAT').click();
    fixture.detectChanges();
    await fixture.whenStable();
    page(fixture).querySelector<HTMLButtonElement>('.proposal button.confirm')!.click();

    const confirm = http().expectOne({ method: 'POST', url: `${MESSAGES}/${id}/confirm` });
    expect(confirm.request.body).toEqual({
      periods: [{ startDate: '2026-11-28', endDate: '2026-11-28', weekdays: ['SAT'], startTime: '08:00', endTime: '12:00' }],
    });
    confirm.flush([{ id: 'u1', startsAt: '2026-11-28T08:00:00Z', endsAt: '2026-11-28T12:00:00Z', source: 'FREE_TEXT' }]);
    http().expectOne('/api/v1/unavailabilities')
      .flush([{ id: 'u1', startsAt: '2026-11-28T08:00:00Z', endsAt: '2026-11-28T12:00:00Z', source: 'FREE_TEXT' }]);
    fixture.detectChanges();

    expect(page(fixture).querySelector('.proposal')).toBeNull();
    expect(page(fixture).querySelector('[data-id="u1"]')?.textContent).toContain('2026-11-28 08:00');
  });

  it('TC-SCRUM-75-19 Discard deletes the message and clears the proposal', async () => {
    const fixture = await start();
    await sendAndAnswer(fixture, [saturdayMorning]);

    page(fixture).querySelector<HTMLButtonElement>('.proposal button.discard')!.click();
    http().expectOne({ method: 'DELETE', url: `${MESSAGES}/${id}` }).flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();

    expect(page(fixture).querySelector('.proposal')).toBeNull();
    http().expectNone({ method: 'POST', url: `${MESSAGES}/${id}/confirm` });
  });

  it('TC-SCRUM-75-20 nothing read automatically: the form is suggested and the message is deleted', async () => {
    const fixture = await start();
    await sendAndAnswer(fixture, [], 'FALLBACK');

    http().expectOne({ method: 'DELETE', url: `${MESSAGES}/${id}` }).flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();
    expect(page(fixture).querySelector('.proposal')).toBeNull();
    expect(page(fixture).querySelector('.message output')?.textContent).toContain('Use the form above');
  });

  it('TC-SCRUM-75-20 too many messages (429) and a text that is too long (400) are explained', async () => {
    const fixture = await start();
    await send(fixture, 'não posso amanhã');
    http().expectOne({ method: 'POST', url: MESSAGES })
      .flush({ status: 429 }, { status: 429, statusText: 'Too Many Requests', headers: { 'Retry-After': '600' } });
    fixture.detectChanges();
    expect(page(fixture).querySelector('.message [role="alert"]')?.textContent).toContain('10 messages');

    await send(fixture, 'não posso amanhã');
    http().expectOne({ method: 'POST', url: MESSAGES })
      .flush({ status: 400, errors: [{ field: 'text', code: 'too_long' }] }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    expect(page(fixture).querySelector('.message [role="alert"]')?.textContent).toContain('500 characters');
  });

  it('TC-SCRUM-75-20 an end date before the start date in the proposal shows an error and nothing is sent', async () => {
    const fixture = await start();
    await sendAndAnswer(fixture, [saturdayMorning]);

    setValue(input(fixture, 'endDate-0'), '2026-11-27');
    fixture.detectChanges();
    await fixture.whenStable();
    page(fixture).querySelector<HTMLButtonElement>('.proposal button.confirm')!.click();
    fixture.detectChanges();

    http().expectNone({ method: 'POST', url: `${MESSAGES}/${id}/confirm` });
    expect(page(fixture).querySelector('.proposal [role="alert"]')?.textContent).toContain('before the start');
  });

  it('TC-SCRUM-158-05 two clicks on Send before the answer send the text once; the button is disabled until the answer, then enabled again', async () => {
    const fixture = await start();
    const box = page(fixture).querySelector<HTMLTextAreaElement>('textarea[name="text"]')!;
    box.value = 'não posso amanhã';
    box.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    const sendButton = page(fixture).querySelector<HTMLButtonElement>('.message button.send')!;

    sendButton.click();
    sendButton.click();
    fixture.detectChanges();

    const post = http().expectOne({ method: 'POST', url: MESSAGES });
    expect(sendButton.disabled).toBe(true);
    post.flush({ status: 429 }, { status: 429, statusText: 'Too Many Requests' });
    fixture.detectChanges();

    expect(sendButton.disabled).toBe(false);
  });

  it('TC-SCRUM-158-06 two clicks on Confirm before the answer confirm once; the button is disabled until the answer, then enabled again', async () => {
    const fixture = await start();
    await sendAndAnswer(fixture, [saturdayMorning]);
    const confirm = page(fixture).querySelector<HTMLButtonElement>('.proposal button.confirm')!;

    confirm.click();
    confirm.click();
    fixture.detectChanges();

    const post = http().expectOne({ method: 'POST', url: `${MESSAGES}/${id}/confirm` });
    expect(confirm.disabled).toBe(true);
    post.flush({ status: 400, errors: [{ field: 'periods[0].endDate', code: 'in_the_past' }] }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(confirm.disabled).toBe(false);
  });
  it('TC-SCRUM-89-01 "bom dia a todos" has no dates: Escala says so, suggests the form, deletes the message and saves nothing', async () => {
    const fixture = await start();
    await send(fixture, 'bom dia a todos');
    http().expectOne({ method: 'POST', url: MESSAGES }).flush({ id, status: 'PENDING', referenceDate: '2026-11-22' },
      { status: 202, statusText: 'Accepted' });
    fixture.detectChanges();
    http().expectOne(`${MESSAGES}/${id}`).flush(answer([], 'MODEL'));
    fixture.detectChanges();

    http().expectOne({ method: 'DELETE', url: `${MESSAGES}/${id}` }).flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();
    expect(page(fixture).querySelector('.message output')?.textContent?.trim())
      .toBe('No dates found in your message. Use the form above.');
    expect(page(fixture).querySelector('.proposal')).toBeNull();
    http().expectNone({ method: 'POST', url: `${MESSAGES}/${id}/confirm` });
    http().expectNone({ method: 'POST', url: '/api/v1/unavailabilities' });
  });

  it('TC-SCRUM-89-02 with AI off only the form is shown, and the form works', async () => {
    const fixture = await start(false);

    expect(page(fixture).querySelector('.message')).toBeNull();
    expect(page(fixture).querySelector('textarea')).toBeNull();
    const form = page(fixture).querySelector<HTMLFormElement>('form[aria-label="Unavailability"]');
    expect(form).not.toBeNull();

    setValue(page(fixture).querySelector<HTMLInputElement>('[name="startDate"]')!, '2026-12-05');
    setValue(page(fixture).querySelector<HTMLInputElement>('[name="endDate"]')!, '2026-12-05');
    fixture.detectChanges();
    await fixture.whenStable();
    form!.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
    expect(http().expectOne({ method: 'POST', url: '/api/v1/unavailabilities' }).request.body.startsAt).toBeDefined();
  });

  it('TC-SCRUM-89-02 if AI is turned off after the page opened, the 409 ai_off hides the free text and leaves the form', async () => {
    const fixture = await start();
    await send(fixture, 'não posso amanhã');
    http().expectOne({ method: 'POST', url: MESSAGES })
      .flush({ status: 409, errors: [{ field: 'text', code: 'ai_off' }] }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(page(fixture).querySelector('.message')).toBeNull();
    expect(page(fixture).querySelector('form[aria-label="Unavailability"]')).not.toBeNull();
  });

  it('TC-SCRUM-89-03 no answer within 10 s of sending: polling stops, the message is deleted, the text stays in the box, and a late answer is ignored', async () => {
    const fixture = await start();
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'Date'] });
    try {
      await send(fixture, 'não posso sábado de manhã');
      http().expectOne({ method: 'POST', url: MESSAGES }).flush({ id, status: 'PENDING', referenceDate: '2026-11-22' },
        { status: 202, statusText: 'Accepted' });
      fixture.detectChanges();

      // Still waiting at 9 s: one poll a second, all PENDING.
      http().expectOne(`${MESSAGES}/${id}`).flush(PENDING_VIEW);
      for (let second = 1; second < 10; second++) {
        await vi.advanceTimersByTimeAsync(1000);
        http().expectOne(`${MESSAGES}/${id}`).flush(PENDING_VIEW);
      }
      fixture.detectChanges();
      expect(page(fixture).querySelector('.message output')?.textContent).toContain('Reading');

      // 10 s after sending: no more polls, and the message is discarded so that a late result is not used.
      await vi.advanceTimersByTimeAsync(1000);
      http().expectNone({ method: 'GET', url: `${MESSAGES}/${id}` });
      http().expectOne({ method: 'DELETE', url: `${MESSAGES}/${id}` }).flush(null, { status: 204, statusText: 'No Content' });
      fixture.detectChanges();

      expect(page(fixture).querySelector('.message output')?.textContent?.trim())
        .toBe('This is taking too long. Your text is still here; use the form above.');
      expect(page(fixture).querySelector<HTMLTextAreaElement>('textarea[name="text"]')!.value).toBe('não posso sábado de manhã');
      expect(page(fixture).textContent).not.toContain('Still reading');

      await vi.advanceTimersByTimeAsync(30_000);
      http().expectNone({ method: 'GET', url: `${MESSAGES}/${id}` });
      fixture.detectChanges();
      expect(page(fixture).querySelector('.proposal')).toBeNull();
    } finally {
      vi.useRealTimers();
    }
  });

  it('TC-SCRUM-89-03 a second text sent while the first is read deletes the first before it is sent, and gets its own 10 s, counted from its own sending', async () => {
    const fixture = await start();
    const second = '75000000-0000-4000-8000-000000000002';
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'Date'] });
    try {
      await send(fixture, 'não posso sábado de manhã');
      http().expectOne({ method: 'POST', url: MESSAGES }).flush({ id, status: 'PENDING', referenceDate: '2026-11-22' },
        { status: 202, statusText: 'Accepted' });
      http().expectOne(`${MESSAGES}/${id}`).flush(PENDING_VIEW);
      await vi.advanceTimersByTimeAsync(5000);
      http().match(`${MESSAGES}/${id}`).forEach((poll) => poll.flush(PENDING_VIEW));

      await send(fixture, 'não posso domingo');
      // The first text is deleted on the server first, so it does not stay PENDING for 24 h (page 09).
      const deleteFirst = http().expectOne({ method: 'DELETE', url: `${MESSAGES}/${id}` });
      http().expectNone({ method: 'POST', url: MESSAGES });
      deleteFirst.flush(null, { status: 204, statusText: 'No Content' });
      const post = http().expectOne({ method: 'POST', url: MESSAGES });
      expect(post.request.body).toEqual({ text: 'não posso domingo' });
      post.flush({ id: second, status: 'PENDING', referenceDate: '2026-11-22' }, { status: 202, statusText: 'Accepted' });
      for (let elapsed = 0; elapsed < 9000; elapsed += 1000) {
        http().expectOne(`${MESSAGES}/${second}`).flush({ ...PENDING_VIEW, id: second });
        await vi.advanceTimersByTimeAsync(1000);
      }
      // 14 s after the first text, 9 s after the second: no deadline has passed for the second one.
      http().expectNone({ method: 'DELETE', url: `${MESSAGES}/${second}` });
      http().expectNone({ method: 'GET', url: `${MESSAGES}/${id}` });
      fixture.detectChanges();
      expect(page(fixture).querySelector('.message output')?.textContent).toContain('Reading');

      http().expectOne(`${MESSAGES}/${second}`).flush({ ...PENDING_VIEW, id: second });
      await vi.advanceTimersByTimeAsync(1000);
      http().expectOne({ method: 'DELETE', url: `${MESSAGES}/${second}` }).flush(null, { status: 204, statusText: 'No Content' });
    } finally {
      vi.useRealTimers();
    }
  });

  it('TC-SCRUM-89-03 if deleting the first text fails, the second is sent anyway', async () => {
    const fixture = await start();
    const second = '75000000-0000-4000-8000-000000000002';
    await send(fixture, 'não posso sábado de manhã');
    http().expectOne({ method: 'POST', url: MESSAGES }).flush({ id, status: 'PENDING', referenceDate: '2026-11-22' },
      { status: 202, statusText: 'Accepted' });
    const poll = http().expectOne({ method: 'GET', url: `${MESSAGES}/${id}` });

    await send(fixture, 'não posso domingo');
    http().expectOne({ method: 'DELETE', url: `${MESSAGES}/${id}` })
      .flush(null, { status: 503, statusText: 'Service Unavailable' });
    const post = http().expectOne({ method: 'POST', url: MESSAGES });
    expect(post.request.body).toEqual({ text: 'não posso domingo' });
    post.flush({ id: second, status: 'PENDING', referenceDate: '2026-11-22' }, { status: 202, statusText: 'Accepted' });

    // An answer for the first text, still in flight, is not used.
    poll.flush(answer([saturdayMorning]));
    http().expectOne({ method: 'GET', url: `${MESSAGES}/${second}` }).flush({ ...PENDING_VIEW, id: second });
    fixture.detectChanges();
    expect(page(fixture).querySelector('.proposal')).toBeNull();
    expect(page(fixture).querySelector('.message output')?.textContent).toContain('Reading');
    fixture.destroy();
  });

  it('TC-SCRUM-89-03 a poll still in flight at 10 s is ignored when it comes back with a proposal', async () => {
    const fixture = await start();
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'Date'] });
    try {
      await send(fixture, 'não posso sábado de manhã');
      http().expectOne({ method: 'POST', url: MESSAGES }).flush({ id, status: 'PENDING', referenceDate: '2026-11-22' },
        { status: 202, statusText: 'Accepted' });
      http().expectOne(`${MESSAGES}/${id}`).flush(PENDING_VIEW);
      for (let second = 1; second < 10; second++) {
        await vi.advanceTimersByTimeAsync(1000);
        if (second < 9) {
          http().expectOne(`${MESSAGES}/${id}`).flush(PENDING_VIEW);
        }
      }
      const inFlight = http().expectOne(`${MESSAGES}/${id}`);

      await vi.advanceTimersByTimeAsync(1000);
      http().expectOne({ method: 'DELETE', url: `${MESSAGES}/${id}` }).flush(null, { status: 204, statusText: 'No Content' });
      inFlight.flush(answer([saturdayMorning]));
      fixture.detectChanges();

      expect(page(fixture).querySelector('.proposal')).toBeNull();
      expect(page(fixture).querySelector('.message output')?.textContent).toContain('taking too long');
    } finally {
      vi.useRealTimers();
    }
  });
});
