import { TestBed } from '@angular/core/testing';
import { DraftShift, emptyAssigned } from './schedule-model';
import {
  PublicationOutcome,
  SchedulePublisher,
  conflictLines,
  periodOf,
  publicationRequest,
} from './schedule-publication';
import { http, signInAs, testProviders } from '../testing/test-support';

/** Publishing a week: what is sent, how the checks are followed, and how each conflict reads. */
describe('Schedule publication', () => {
  function shift(change: Partial<DraftShift>): DraftShift {
    return {
      id: 'p-1',
      dutyDate: '2026-12-02',
      start: 8 * 60,
      end: 20 * 60,
      crew: { CREW_CHIEF: 1, DRIVER: 1, CREW_MEMBER: 2 },
      assigned: emptyAssigned(),
      change: 'published',
      ...change,
    };
  }

  describe('the request', () => {
    it('sends every shift from today on, with its crew, its members and its times in UTC', () => {
      const request = publicationRequest(
        '2026-11-30',
        [
          shift({ id: 'p-old', dutyDate: '2026-11-30' }),
          shift({
            id: 'p-1',
            assigned: { CREW_CHIEF: ['m-ana'], DRIVER: [], CREW_MEMBER: ['m-rui', 'm-joao'] },
            change: 'edited',
          }),
          shift({ id: 'new-1', dutyDate: '2026-12-03', start: 20 * 60, end: 32 * 60, change: 'new' }),
        ],
        '2026-12-01',
      );

      expect(request.week).toBe('2026-W49');
      expect(request.shifts.map((each) => each.key)).toEqual(['p-1', 'new-1']); // Monday has passed: never sent
      expect(request.shifts[0]).toEqual({
        key: 'p-1',
        id: 'p-1',
        dutyDate: '2026-12-02',
        period: 'DAY',
        startsAt: '2026-12-02T08:00:00Z',
        endsAt: '2026-12-02T20:00:00Z',
        crew: [
          { role: 'CREW_CHIEF', count: 1 },
          { role: 'DRIVER', count: 1 },
          { role: 'CREW_MEMBER', count: 2 },
        ],
        assigned: [
          { role: 'CREW_CHIEF', memberId: 'm-ana' },
          { role: 'CREW_MEMBER', memberId: 'm-rui' },
          { role: 'CREW_MEMBER', memberId: 'm-joao' },
        ],
      });
      // A new shift has no id yet; a night shift ends the next morning
      expect(request.shifts[1]).toMatchObject({
        id: null,
        period: 'NIGHT',
        startsAt: '2026-12-03T20:00:00Z',
        endsAt: '2026-12-04T08:00:00Z',
      });
    });

    it('writes the times on the Lisbon clock, in summer too', () => {
      const request = publicationRequest('2027-06-28', [shift({ id: 'p-s', dutyDate: '2027-06-30' })], '2027-06-28');
      expect(request.shifts[0].startsAt).toBe('2027-06-30T07:00:00Z');
    });

    it('calls a shift from 08:00 to before 20:00 a day shift, and any other a night shift', () => {
      expect(periodOf(shift({ start: 8 * 60, end: 20 * 60 }))).toBe('DAY');
      expect(periodOf(shift({ start: 19 * 60, end: 23 * 60 }))).toBe('DAY');
      expect(periodOf(shift({ start: 0, end: 8 * 60 }))).toBe('NIGHT');
      expect(periodOf(shift({ start: 20 * 60, end: 32 * 60 }))).toBe('NIGHT');
      expect(periodOf(shift({ start: 14 * 60, end: 26 * 60 }))).toBe('NIGHT');
    });
  });

  describe('the conflicts', () => {
    const members = [{ id: 'm-ana', name: 'Ana Costa', roles: ['CREW_CHIEF' as const] }];
    const shifts = [shift({ id: 'p-1' })];

    it('names the shift and the member, with what each broken rule means', () => {
      const lines = conflictLines(
        [
          { type: 'EMPTY_SHIFT', shiftKey: 'p-1', shiftId: 'p-1', dutyDate: '2026-12-02' },
          { type: 'INELIGIBLE', shiftKey: 'p-1', shiftId: 'p-1', dutyDate: '2026-12-02', memberId: 'm-ana', role: 'CREW_CHIEF', rules: ['RN05'] },
          { type: 'INELIGIBLE', shiftKey: 'p-1', shiftId: 'p-1', dutyDate: '2026-12-02', memberId: 'm-x', role: 'DRIVER', rules: ['RN01', 'RN06'] },
          { type: 'NOT_CHECKED', shiftKey: 'p-1', shiftId: 'p-1', dutyDate: '2026-12-02', memberId: 'm-ana', role: 'CREW_CHIEF' },
        ],
        shifts,
        members,
      );

      expect(lines.map((line) => line.shift)).toEqual(Array(4).fill('Wednesday 2 December, 08:00 – 20:00'));
      expect(lines.map((line) => line.problem)).toEqual([
        'Nobody is on this shift. Add someone to it or delete it.',
        'Ana Costa (Crew chief) is on another shift at the same time.',
        "A member (Driver) isn't on the active roster, so they can't be scheduled. They also said they can't come during this shift.",
        "Ana Costa (Crew chief) couldn't be checked. Publish again to check them again.",
      ]);
      expect(lines.every((line) => line.shiftKey === 'p-1')).toBe(true);
    });

    it('falls back to the day when the shift is no longer in the draft', () => {
      const [line] = conflictLines(
        [{ type: 'EMPTY_SHIFT', shiftKey: 'gone', shiftId: 'gone', dutyDate: '2026-12-03' }],
        shifts,
        members,
      );
      expect(line.shift).toBe('Thursday 3 December');
    });
  });

  describe('following the checks', () => {
    let outcome: PublicationOutcome | undefined;

    beforeEach(async () => {
      localStorage.clear();
      signInAs('SUPERVISOR');
      vi.useFakeTimers();
      outcome = undefined;
      await TestBed.configureTestingModule({ providers: testProviders }).compileComponents();
    });

    afterEach(() => {
      http().verify();
      vi.useRealTimers();
      localStorage.clear();
    });

    function start() {
      TestBed.inject(SchedulePublisher)
        .publish({ week: '2026-W49', shifts: [] })
        .subscribe((done) => (outcome = done));
      return http().expectOne('/api/v1/schedule-publications');
    }

    it('asks every second while the members are checked, until the week is published', () => {
      start().flush({ id: 'pub-1', week: '2026-W49', status: 'CHECKING', conflicts: [] });

      vi.advanceTimersByTime(1000);
      http().expectOne('/api/v1/schedule-publications/pub-1').flush({ id: 'pub-1', week: '2026-W49', status: 'CHECKING', conflicts: [] });
      expect(outcome).toBeUndefined();

      vi.advanceTimersByTime(1000);
      http().expectOne('/api/v1/schedule-publications/pub-1').flush({ id: 'pub-1', week: '2026-W49', status: 'PUBLISHED', conflicts: [] });
      expect(outcome).toEqual({ kind: 'published' });

      vi.advanceTimersByTime(5000);
      http().expectNone('/api/v1/schedule-publications/pub-1');
    });

    it('gives the conflicts of a blocked week', () => {
      const conflicts = [{ type: 'EMPTY_SHIFT', shiftKey: 'new-1', shiftId: 'x', dutyDate: '2026-12-01' }];
      start().flush({ id: 'pub-2', week: '2026-W49', status: 'BLOCKED', conflicts });
      expect(outcome).toEqual({ kind: 'blocked', conflicts });
    });

    it('stops asking after a minute still checking, and says so', () => {
      start().flush({ id: 'pub-3', week: '2026-W49', status: 'CHECKING', conflicts: [] });
      for (let second = 1; second <= 59; second++) {
        vi.advanceTimersByTime(1000);
        http().expectOne('/api/v1/schedule-publications/pub-3').flush({ id: 'pub-3', week: '2026-W49', status: 'CHECKING', conflicts: [] });
      }
      vi.advanceTimersByTime(1000);
      // The request still on its way at a minute is cancelled
      expect(http().match('/api/v1/schedule-publications/pub-3').every((request) => request.cancelled)).toBe(true);
      expect(outcome).toEqual({ kind: 'timeout' });
    });
  });
});
