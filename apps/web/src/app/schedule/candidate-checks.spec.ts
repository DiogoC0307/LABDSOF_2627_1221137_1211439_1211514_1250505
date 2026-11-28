import { TestBed } from '@angular/core/testing';
import { AuthService } from '../auth/auth.service';
import { http, signInAs, testProviders } from '../testing/test-support';
import { CandidateChecks, candidateQuestion, shiftInstants } from './candidate-checks';
import { DraftShift, emptyAssigned } from './schedule-model';

// Who can take a role on a shift is asked of the eligibility worker
describe('CandidateChecks', () => {
  function shift(change: Partial<DraftShift>): DraftShift {
    return {
      id: 'p-1',
      dutyDate: '2026-12-02',
      start: 8 * 60,
      end: 20 * 60,
      crew: { CREW_CHIEF: 1, DRIVER: 0, CREW_MEMBER: 0 },
      assigned: emptyAssigned(),
      slots: [],
      change: 'new',
      ...change,
    };
  }

  describe('the question', () => {
    it('gives the times on the Lisbon clock in UTC, a night shift ending the next morning', () => {
      expect(shiftInstants(shift({}))).toEqual({
        startsAt: '2026-12-02T08:00:00Z',
        endsAt: '2026-12-02T20:00:00Z',
      });
      expect(shiftInstants(shift({ start: 20 * 60, end: 32 * 60 }))).toEqual({
        startsAt: '2026-12-02T20:00:00Z',
        endsAt: '2026-12-03T08:00:00Z',
      });
      expect(shiftInstants(shift({ dutyDate: '2027-06-30' })).startsAt).toBe(
        '2027-06-30T07:00:00Z',
      );
    });

    it("sends who is on the week's other shifts, not on this one, and is the same whatever their order", () => {
      const self = shift({ assigned: { ...emptyAssigned(), CREW_CHIEF: ['m-self'] } });
      const monday = shift({
        id: 'p-mon',
        dutyDate: '2026-11-30',
        assigned: { ...emptyAssigned(), DRIVER: ['m-rui'], CREW_MEMBER: ['m-ana'] },
      });
      const question = candidateQuestion(self, 'CREW_CHIEF', [self, monday]);

      expect(question.body).toEqual({
        startsAt: '2026-12-02T08:00:00Z',
        endsAt: '2026-12-02T20:00:00Z',
        role: 'CREW_CHIEF',
        otherShifts: [
          { memberId: 'm-ana', startsAt: '2026-11-30T08:00:00Z', endsAt: '2026-11-30T20:00:00Z' },
          { memberId: 'm-rui', startsAt: '2026-11-30T08:00:00Z', endsAt: '2026-11-30T20:00:00Z' },
        ],
      });
      expect(candidateQuestion(self, 'CREW_CHIEF', [monday, self]).key).toBe(question.key);
      expect(candidateQuestion(shift({}), 'CREW_CHIEF', [monday]).key).toBe(question.key);
      expect(candidateQuestion(self, 'DRIVER', [monday]).key).not.toBe(question.key);
    });
  });

  describe('asking', () => {
    let checks: CandidateChecks;
    const question = candidateQuestion(shift({}), 'CREW_CHIEF', []);

    beforeEach(() => {
      localStorage.clear();
      signInAs('SUPERVISOR');
      vi.useFakeTimers();
      TestBed.configureTestingModule({ providers: testProviders });
      checks = TestBed.inject(CandidateChecks);
    });

    afterEach(() => {
      http().verify();
      vi.useRealTimers();
      localStorage.clear();
    });

    it('asks once, then every half second until every member is checked', () => {
      checks.ask(question);
      checks.ask(question);
      const started = http().expectOne(
        (req) => req.method === 'POST' && req.url === '/api/v1/candidate-checks',
      );
      expect(started.request.body).toEqual(question.body);
      expect(checks.checks()[question.key]).toEqual({ state: 'checking' });

      started.flush({
        id: 'c-1',
        status: 'CHECKING',
        candidates: [{ memberId: 'm-ana', result: 'PENDING', rules: [] }],
      });
      vi.advanceTimersByTime(500);
      http()
        .expectOne('/api/v1/candidate-checks/c-1')
        .flush({
          id: 'c-1',
          status: 'CHECKING',
          candidates: [{ memberId: 'm-ana', result: 'PENDING', rules: [] }],
        });
      expect(checks.checks()[question.key]).toEqual({ state: 'checking' });

      const answers = [{ memberId: 'm-ana', result: 'INELIGIBLE', rules: ['RN06'] }];
      vi.advanceTimersByTime(500);
      http()
        .expectOne('/api/v1/candidate-checks/c-1')
        .flush({ id: 'c-1', status: 'DONE', candidates: answers });
      expect(checks.checks()[question.key]).toEqual({ state: 'done', answers });

      vi.advanceTimersByTime(2000);
      http().expectNone('/api/v1/candidate-checks/c-1');
    });

    it('fails after 20 seconds still checking, or when the API answers an error, and asks again on retry', () => {
      checks.ask(question);
      http()
        .expectOne('/api/v1/candidate-checks')
        .flush({ id: 'c-2', status: 'CHECKING', candidates: [] });
      for (let tick = 0; tick < 40; tick++) {
        vi.advanceTimersByTime(500);
        http()
          .match('/api/v1/candidate-checks/c-2')
          .filter((request) => !request.cancelled)
          .forEach((request) => request.flush({ id: 'c-2', status: 'CHECKING', candidates: [] }));
      }
      expect(checks.checks()[question.key]).toEqual({ state: 'failed' });

      checks.retry(question);
      http()
        .expectOne('/api/v1/candidate-checks')
        .flush(null, { status: 503, statusText: 'Service Unavailable' });
      expect(checks.checks()[question.key]).toEqual({ state: 'failed' });

      checks.retry(question);
      http()
        .expectOne('/api/v1/candidate-checks')
        .flush({ id: 'c-3', status: 'DONE', candidates: [] });
      expect(checks.checks()[question.key]).toEqual({ state: 'done', answers: [] });
    });

    it('never shows the answers to someone else who signs in on this device', () => {
      checks.ask(question);
      http()
        .expectOne('/api/v1/candidate-checks')
        .flush({ id: 'c-4', status: 'DONE', candidates: [] });
      expect(checks.checks()[question.key]).toBeDefined();

      TestBed.inject(AuthService).session.set({
        token: 'mock.MEMBER',
        memberId: '00000000-0000-4000-8000-000000000001',
        role: 'MEMBER',
        fullName: 'Synthetic Member',
      });
      expect(checks.checks()).toEqual({});
    });
  });
});
