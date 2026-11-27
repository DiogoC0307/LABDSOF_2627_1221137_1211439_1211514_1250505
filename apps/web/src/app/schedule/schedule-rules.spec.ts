import { DraftShift, ScheduleMember, WeekDraft, emptyAssigned } from './schedule-model';
import {
  candidates,
  changeCount,
  endAfter,
  freeGaps,
  fromPiquete,
  missingCount,
  missingLabel,
  newShiftRange,
  overlaps,
  MAXIMUM_PER_ROLE,
  parseTime,
  rangeLabel,
  readNeeded,
  roleState,
  rolesOf,
  shiftState,
} from './schedule-rules';

const Hour = 60;

function shift(overrides: Partial<DraftShift> = {}): DraftShift {
  return {
    id: 's1',
    dutyDate: '2026-11-26',
    start: 8 * Hour,
    end: 20 * Hour,
    crew: { CREW_CHIEF: 1, DRIVER: 1, CREW_MEMBER: 3 },
    assigned: emptyAssigned(),
    change: 'published',
    ...overrides,
  };
}

// The Schedule page's rules
describe('schedule-rules', () => {
  const today = '2026-11-25';

  describe('freeGaps', () => {
    it('gives the whole day when it has no shifts', () => {
      expect(freeGaps([])).toEqual([{ from: 0, to: 24 * Hour }]);
    });

    it('finds the free time before, between and after the shifts', () => {
      expect(
        freeGaps([
          { from: 8 * Hour, to: 12 * Hour },
          { from: 15 * Hour, to: 20 * Hour },
        ]),
      ).toEqual([
        { from: 0, to: 8 * Hour },
        { from: 12 * Hour, to: 15 * Hour },
        { from: 20 * Hour, to: 24 * Hour },
      ]);
    });

    it('leaves out free times shorter than 2 hours', () => {
      expect(
        freeGaps([
          { from: 1 * Hour, to: 8 * Hour },
          { from: 9 * Hour + 30, to: 23 * Hour },
        ]),
      ).toEqual([]);
      expect(freeGaps([{ from: 0, to: 22 * Hour }])).toEqual([{ from: 22 * Hour, to: 24 * Hour }]);
    });

    it('counts overlapping and unsorted shifts as one covered time', () => {
      expect(
        freeGaps([
          { from: 10 * Hour, to: 14 * Hour },
          { from: 0, to: 8 * Hour },
          { from: 12 * Hour, to: 18 * Hour },
        ]),
      ).toEqual([
        { from: 8 * Hour, to: 10 * Hour },
        { from: 18 * Hour, to: 24 * Hour },
      ]);
    });
  });

  describe('readNeeded', () => {
    it('keeps digits only, at most the maximum per role, and changes nothing for an empty box or 0', () => {
      expect(readNeeded('3')).toEqual({ shown: '3', count: 3 });
      expect(readNeeded('a2b')).toEqual({ shown: '2', count: 2 });
      expect(readNeeded('07')).toEqual({ shown: '7', count: 7 });
      expect(readNeeded('99')).toEqual({
        shown: String(MAXIMUM_PER_ROLE),
        count: MAXIMUM_PER_ROLE,
      });
      expect(readNeeded('')).toEqual({ shown: '', count: 0 });
      expect(readNeeded('0')).toEqual({ shown: '0', count: 0 });
    });
  });

  describe('parseTime', () => {
    it('reads the ways people write a time', () => {
      for (const text of ['8:30', '08:30', '8.30', '8h30', '830', '0830', ' 8:30 ']) {
        expect(parseTime(text)).toBe(8 * Hour + 30);
      }
      expect(parseTime('8')).toBe(8 * Hour);
      expect(parseTime('8h')).toBe(8 * Hour);
      expect(parseTime('0')).toBe(0);
      expect(parseTime('24:00')).toBe(24 * Hour);
      expect(parseTime('20H')).toBe(20 * Hour);
      expect(parseTime('2030')).toBe(20 * Hour + 30);
      expect(parseTime('20h30h')).toBe(20 * Hour + 30);
    });

    it('ignores the "(next day)" of an end time', () => {
      expect(parseTime('02:00 (next day)')).toBe(2 * Hour);
    });

    it('refuses what is not a time', () => {
      for (const text of [
        '',
        'abc',
        '8:3',
        '8:60',
        '25',
        '24:30',
        '12345',
        '-1',
        '8::30',
        '8:300',
        'h',
        '860',
      ]) {
        expect(parseTime(text)).toBeNull();
      }
    });
  });

  describe('endAfter', () => {
    it('keeps an end later than the start on the same day', () => {
      expect(endAfter(8 * Hour, 20 * Hour + 30)).toBe(20 * Hour + 30);
    });

    it('moves an end at or before the start to the next day', () => {
      expect(endAfter(20 * Hour, 8 * Hour)).toBe(32 * Hour);
      expect(endAfter(8 * Hour, 8 * Hour)).toBe(32 * Hour);
    });
  });

  describe('newShiftRange', () => {
    it('makes a day shift, 08:00 to 20:00, on an empty day', () => {
      expect(newShiftRange({ from: 0, to: 24 * Hour })).toEqual({ from: 8 * Hour, to: 20 * Hour });
    });

    it('fills a gap of 12 hours or less, and cuts a longer one at 12 hours from its start', () => {
      expect(newShiftRange({ from: 20 * Hour, to: 24 * Hour })).toEqual({
        from: 20 * Hour,
        to: 24 * Hour,
      });
      expect(newShiftRange({ from: 0, to: 20 * Hour })).toEqual({ from: 0, to: 12 * Hour });
    });
  });

  describe('shiftState', () => {
    it('is past for a shift on a day before today, whoever is on it', () => {
      expect(shiftState(shift({ dutyDate: '2026-11-24' }), today)).toBe('past');
    });

    it('is short while a role is missing someone, and full when every role has its people', () => {
      expect(shiftState(shift(), today)).toBe('short');
      const full = shift({
        crew: { CREW_CHIEF: 1, DRIVER: 1, CREW_MEMBER: 1 },
        assigned: { CREW_CHIEF: ['a'], DRIVER: ['b'], CREW_MEMBER: ['c'] },
      });
      expect(shiftState(full, today)).toBe('full');
      expect(shiftState({ ...full, dutyDate: today }, today)).toBe('full');
    });

    it('counts the missing people per role, so a spare driver does not cover a crew chief', () => {
      const spare = shift({
        crew: { CREW_CHIEF: 1, DRIVER: 1, CREW_MEMBER: 0 },
        assigned: { CREW_CHIEF: [], DRIVER: ['b', 'c'], CREW_MEMBER: [] },
      });
      expect(missingCount(spare)).toBe(1);
      expect(shiftState(spare, today)).toBe('short');
      expect(missingLabel(spare)).toBe('Needs 1 crew chief');
    });

    it('says which roles a short shift still needs, and how many of each (TC-SCRUM-65-03)', () => {
      const noDriver = shift({
        crew: { CREW_CHIEF: 1, DRIVER: 1, CREW_MEMBER: 3 },
        assigned: { CREW_CHIEF: ['a'], DRIVER: [], CREW_MEMBER: ['c'] },
      });
      expect(missingLabel(noDriver)).toBe('Needs 1 driver, 2 crew members');
    });

    it('is short for a shift with no roles yet, though nobody is missing', () => {
      expect(shiftState(shift({ crew: { CREW_CHIEF: 0, DRIVER: 0, CREW_MEMBER: 0 } }), today)).toBe(
        'short',
      );
    });

    it('lists the roles a shift needs, in order', () => {
      expect(rolesOf({ CREW_CHIEF: 0, DRIVER: 2, CREW_MEMBER: 1 })).toEqual([
        'DRIVER',
        'CREW_MEMBER',
      ]);
      expect(rolesOf({ CREW_CHIEF: 0, DRIVER: 0, CREW_MEMBER: 0 })).toEqual([]);
    });

    it('names a role full or short', () => {
      expect(roleState(2, 2)).toBe('full');
      expect(roleState(3, 2)).toBe('short');
    });
  });

  describe('candidates', () => {
    const members: ScheduleMember[] = [
      { id: 'ana', name: 'Ana Costa', roles: ['CREW_CHIEF', 'CREW_MEMBER'] },
      { id: 'rui', name: 'Rui Almeida', roles: ['DRIVER'] },
      { id: 'joao', name: 'João Pereira', roles: ['CREW_CHIEF'] },
      { id: 'marta', name: 'Marta Silva', roles: ['CREW_CHIEF'] },
    ];

    it('keeps only members who can fill the role, with all their functions', () => {
      const list = candidates(shift(), 'DRIVER', members, [], new Set());
      expect(list.map((each) => each.member.name)).toEqual(['Rui Almeida']);
      expect(candidates(shift(), 'CREW_MEMBER', members, [], new Set())[0].functions).toBe(
        'Crew chief · Crew member',
      );
    });

    it('leaves out members already on this shift, in any role', () => {
      const onIt = shift({ assigned: { ...emptyAssigned(), CREW_MEMBER: ['ana'] } });
      const names = candidates(onIt, 'CREW_CHIEF', members, [onIt], new Set()).map(
        (each) => each.member.id,
      );
      expect(names).toEqual(['joao', 'marta']);
    });

    it("leaves out members who can't come that day", () => {
      const names = candidates(shift(), 'CREW_CHIEF', members, [], new Set(['joao'])).map(
        (each) => each.member.id,
      );
      expect(names).toEqual(['ana', 'marta']);
    });

    it('keeps members on another shift that day, with its times', () => {
      const morning = shift({
        id: 'm',
        start: 0,
        end: 8 * Hour,
        assigned: { ...emptyAssigned(), CREW_CHIEF: ['marta'] },
      });
      const list = candidates(shift(), 'CREW_CHIEF', members, [morning, shift()], new Set());
      expect(list.find((each) => each.member.id === 'marta')?.alreadyOn).toBe('00:00 – 08:00');
      expect(list.find((each) => each.member.id === 'ana')?.alreadyOn).toBeNull();
    });

    it('leaves out members on another shift at any of its time, also across midnight (TC-SCRUM-65-17, -19)', () => {
      // Friday 20:00 to Saturday 08:00, and Saturday 06:00 to 14:00
      const fridayNight = shift({
        id: 'fri',
        dutyDate: '2026-11-27',
        start: 20 * Hour,
        end: 32 * Hour,
        assigned: { ...emptyAssigned(), CREW_CHIEF: ['marta'] },
      });
      const saturday = shift({
        id: 'sat',
        dutyDate: '2026-11-28',
        start: 6 * Hour,
        end: 14 * Hour,
      });
      const sameTime = shift({
        id: 'same',
        assigned: { ...emptyAssigned(), CREW_CHIEF: ['joao'] },
      });

      const ids = (list: { member: ScheduleMember }[]) => list.map((each) => each.member.id);
      expect(
        ids(candidates(saturday, 'CREW_CHIEF', members, [fridayNight, saturday], new Set())),
      ).toEqual(['ana', 'joao']);
      expect(
        ids(candidates(shift(), 'CREW_CHIEF', members, [sameTime, shift()], new Set())),
      ).toEqual(['ana', 'marta']);
    });

    it('keeps a member whose other shift ends exactly when this one starts (TC-SCRUM-65-18)', () => {
      const fridayNight = shift({
        id: 'fri',
        dutyDate: '2026-11-27',
        start: 20 * Hour,
        end: 32 * Hour,
      });
      const saturday = shift({
        id: 'sat',
        dutyDate: '2026-11-28',
        start: 8 * Hour,
        end: 20 * Hour,
      });
      expect(overlaps(saturday, fridayNight)).toBe(false);
      expect(overlaps(fridayNight, saturday)).toBe(false);
      expect(overlaps({ ...saturday, start: 7 * Hour }, fridayNight)).toBe(true);
      // On different days, with no time in common (TC-SCRUM-65-16)
      expect(overlaps(shift({ dutyDate: '2026-11-25' }), shift())).toBe(false);
    });
  });

  describe('changeCount', () => {
    it('is 0 for a week not loaded or as published', () => {
      expect(changeCount(undefined)).toBe(0);
      const published = [shift()];
      expect(changeCount({ published, shifts: published, deleted: [] })).toBe(0);
    });

    it('counts new and edited shifts, and published shifts deleted', () => {
      const draft: WeekDraft = {
        published: [shift({ id: 'a' }), shift({ id: 'b' }), shift({ id: 'c' })],
        shifts: [
          shift({ id: 'a', change: 'edited' }),
          shift({ id: 'b' }),
          shift({ id: 'new-1', change: 'new' }),
        ],
        deleted: ['c'],
      };
      expect(changeCount(draft)).toBe(3);
    });
  });

  describe('fromPiquete', () => {
    it('reads a night shift as ending the next day, and its crew from its slots', () => {
      const night = fromPiquete({
        id: 'p1',
        dutyDate: '2026-11-26',
        startsAt: '2026-11-26T20:00:00Z',
        endsAt: '2026-11-27T08:00:00Z',
        minimumCrew: 2,
        slots: [
          { id: 'x', role: 'DRIVER', status: 'PENDING' },
          { id: 'y', role: 'CREW_MEMBER', status: 'PENDING' },
        ],
      })!;
      expect(night.start).toBe(20 * Hour);
      expect(night.end).toBe(32 * Hour);
      expect(rangeLabel(night.start, night.end)).toBe('20:00 – 08:00');
      expect(night.crew).toEqual({ CREW_CHIEF: 0, DRIVER: 1, CREW_MEMBER: 1 });
      expect(night.change).toBe('published');
    });

    it('gives a shift from before roles its minimum crew as crew members, and skips one with no times', () => {
      const before = fromPiquete({
        id: 'p2',
        dutyDate: '2026-11-26',
        startsAt: '2026-11-26T08:00:00Z',
        endsAt: '2026-11-26T20:00:00Z',
        minimumCrew: 4,
        slots: [{ id: 'x', role: 'CREW_MEMBER', status: 'PENDING' }],
      })!;
      expect(before.crew.CREW_MEMBER).toBe(4);
      expect(fromPiquete({ id: 'p3', dutyDate: '2026-11-26', slots: [] })).toBeNull();
    });
  });
});
