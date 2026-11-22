import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { App } from '../app';
import { http, setValue, signInAs, testProviders } from '../testing/test-support';

const MESSAGES = '/api/v1/unavailability-messages';
const id = '75000000-0000-4000-8000-000000000001';

const saturdayMorning = { startDate: '2026-11-28', endDate: '2026-11-28', startTime: '08:00', endTime: '13:00' };

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

  async function start() {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl('/unavailability');
    fixture.detectChanges();
    http().expectOne('/api/v1/unavailabilities').flush([]);
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
    expect(page(fixture).querySelector('.message [role="status"]')?.textContent).toContain('Reading');

    http().expectOne(`${MESSAGES}/${id}`).flush(answer([saturdayMorning]));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(page(fixture).querySelector('.proposal')?.textContent).toContain('Read automatically');
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
    expect(page(fixture).querySelector('.message [role="status"]')?.textContent).toContain('Use the form above');
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
});
