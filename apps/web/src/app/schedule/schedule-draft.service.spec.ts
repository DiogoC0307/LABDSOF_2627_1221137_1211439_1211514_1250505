import { TestBed } from '@angular/core/testing';
import { AuthService } from '../auth/auth.service';
import { signInAs, testProviders } from '../testing/test-support';
import { ScheduleDraftService } from './schedule-draft.service';
import { DraftShift, emptyAssigned } from './schedule-model';

const MONDAY = '2026-11-30';
const CREW = { CREW_CHIEF: 1, DRIVER: 0, CREW_MEMBER: 2 };

function published(id: string, dutyDate = '2026-12-01'): DraftShift {
  return {
    id,
    dutyDate,
    start: 8 * 60,
    end: 20 * 60,
    crew: { CREW_CHIEF: 1, DRIVER: 1, CREW_MEMBER: 3 },
    assigned: emptyAssigned(),
    slots: [],
    change: 'published',
  };
}

// Weekly schedule drafts tests
describe('ScheduleDraftService', () => {
  let drafts: ScheduleDraftService;

  beforeEach(() => {
    signInAs('SUPERVISOR');
    TestBed.configureTestingModule({ providers: testProviders });
    drafts = TestBed.inject(ScheduleDraftService);
    drafts.load(MONDAY, [published('a'), published('b')]);
  });

  afterEach(() => localStorage.clear());

  it("never gives someone else who signs in on this device the supervisor's edits", () => {
    drafts.add(MONDAY, '2026-12-02', 20 * 60, 24 * 60, CREW);
    expect(drafts.changes(MONDAY)).toBe(1);

    TestBed.inject(AuthService).session.set({
      token: 'mock.MEMBER',
      memberId: '00000000-0000-4000-8000-000000000001',
      role: 'MEMBER',
      fullName: 'Synthetic Member',
    });
    expect(drafts.week(MONDAY)).toBeUndefined();

    // The member's own view of the week is only what is published.
    drafts.load(MONDAY, [published('a')]);
    expect(drafts.shifts(MONDAY).map((shift) => shift.id)).toEqual(['a']);
    expect(drafts.changes(MONDAY)).toBe(0);
  });

  it('starts a week with no changes, and keeps its edits when it is loaded again', () => {
    expect(drafts.changes(MONDAY)).toBe(0);
    drafts.setNeeded(MONDAY, 'a', 'DRIVER', 2);
    drafts.load(MONDAY, []);

    expect(drafts.shifts(MONDAY).length).toBe(2);
    expect(drafts.changes(MONDAY)).toBe(1);
  });

  it('counts a new shift once, however much it is changed, with the crew it was created with', () => {
    const added = drafts.add(MONDAY, '2026-12-02', 20 * 60, 24 * 60, CREW);
    expect(added.crew).toEqual({ CREW_CHIEF: 1, DRIVER: 0, CREW_MEMBER: 2 });
    drafts.assign(MONDAY, added.id, 'CREW_CHIEF', 'm-ana');
    drafts.setTimes(MONDAY, added.id, 18 * 60, 24 * 60);

    expect(drafts.changes(MONDAY)).toBe(1);
    expect(drafts.shifts(MONDAY).find((shift) => shift.id === added.id)?.change).toBe('new');
  });

  it('counts a published shift once it is edited, and each edited shift once', () => {
    drafts.assign(MONDAY, 'a', 'CREW_CHIEF', 'm-ana');
    drafts.unassign(MONDAY, 'a', 'CREW_CHIEF', 'm-ana');
    drafts.setNeeded(MONDAY, 'b', 'CREW_MEMBER', 4);

    expect(drafts.shifts(MONDAY).map((shift) => shift.change)).toEqual(['edited', 'edited']);
    expect(drafts.changes(MONDAY)).toBe(2);
  });

  it('takes a role off a shift with the people assigned to it', () => {
    drafts.setNeeded(MONDAY, 'a', 'DRIVER', 2);
    drafts.assign(MONDAY, 'a', 'DRIVER', 'm-rui');
    drafts.setNeeded(MONDAY, 'a', 'DRIVER', 0);

    const shift = drafts.shifts(MONDAY).find((each) => each.id === 'a')!;
    expect(shift.crew.DRIVER).toBe(0);
    expect(shift.assigned.DRIVER).toEqual([]);
  });

  it('counts a deleted published shift, but not a deleted new one', () => {
    const added = drafts.add(MONDAY, '2026-12-02', 8 * 60, 20 * 60, CREW);
    drafts.remove(MONDAY, added.id);
    expect(drafts.changes(MONDAY)).toBe(0);

    drafts.remove(MONDAY, 'a');
    expect(drafts.changes(MONDAY)).toBe(1);
    expect(drafts.week(MONDAY)?.deleted).toEqual(['a']);
  });

  it('discards back to the published week', () => {
    drafts.remove(MONDAY, 'a');
    drafts.add(MONDAY, '2026-12-02', 8 * 60, 20 * 60, CREW);
    drafts.discard(MONDAY);
    expect(drafts.changes(MONDAY)).toBe(0);
    expect(drafts.shifts(MONDAY).map((shift) => shift.id)).toEqual(['a', 'b']);
  });

  it('once published, takes the week as the API has it, with no edits left', () => {
    drafts.add(MONDAY, '2026-12-02', 8 * 60, 20 * 60, CREW);
    drafts.remove(MONDAY, 'b');
    drafts.replace(MONDAY, [published('a'), published('c', '2026-12-02')]);

    expect(drafts.changes(MONDAY)).toBe(0);
    expect(drafts.shifts(MONDAY).map((shift) => shift.id)).toEqual(['a', 'c']);
    expect(drafts.week(MONDAY)?.published.map((shift) => shift.id)).toEqual(['a', 'c']);
  });

  it('forgets a week, so it is loaded again from the API', () => {
    drafts.remove(MONDAY, 'a');
    drafts.forget(MONDAY);
    expect(drafts.week(MONDAY)).toBeUndefined();

    drafts.load(MONDAY, [published('a')]);
    expect(drafts.changes(MONDAY)).toBe(0);
  });

  it('keeps each week apart', () => {
    drafts.load('2026-12-07', [published('c', '2026-12-08')]);
    drafts.remove(MONDAY, 'a');

    expect(drafts.changes(MONDAY)).toBe(1);
    expect(drafts.changes('2026-12-07')).toBe(0);
  });
});
