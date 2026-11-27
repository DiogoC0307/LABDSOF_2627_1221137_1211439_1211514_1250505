import { TestBed } from '@angular/core/testing';
import { AuthService } from '../auth/auth.service';
import { http, signInAs, testProviders } from '../testing/test-support';
import { ScheduleDirectory } from './schedule-directory';
import { DraftShift, emptyAssigned } from './schedule-model';

// Schedule page's members and who can't come, from the API
describe('ScheduleDirectory', () => {
  let directory: ScheduleDirectory;

  // A shift in week 2026-W49 (30 November to 6 December)
  function shift(dutyDate: string, start: number, end: number): DraftShift {
    return {
      id: 's1',
      dutyDate,
      start,
      end,
      crew: { CREW_CHIEF: 1, DRIVER: 0, CREW_MEMBER: 0 },
      assigned: emptyAssigned(),
      change: 'new',
    };
  }

  beforeEach(() => {
    signInAs('SUPERVISOR');
    TestBed.configureTestingModule({ providers: testProviders });
    directory = TestBed.inject(ScheduleDirectory);
  });

  afterEach(() => {
    http().verify();
    localStorage.clear();
  });

  it("never shows the members or who can't come to someone else who signs in on this device", () => {
    directory.load('2026-11-30');
    http()
      .expectOne('/api/v1/members')
      .flush([{ id: 'a', fullName: 'Ana Costa', active: true, rosterStatus: 'ACTIVE_ROSTER', functions: ['CREW_CHIEF'] }]);
    http()
      .expectOne((req) => req.url === '/api/v1/unavailabilities/team')
      .flush([{ memberId: 'a', startsAt: '2026-12-01T00:00:00Z', endsAt: '2026-12-02T00:00:00Z' }]);
    expect(directory.members().length).toBe(1);
    expect(directory.unavailableFor(shift('2026-12-01', 8 * 60, 20 * 60)).size).toBe(1);

    TestBed.inject(AuthService).session.set({
      token: 'mock.MEMBER',
      memberId: '00000000-0000-4000-8000-000000000001',
      role: 'MEMBER',
      fullName: 'Synthetic Member',
    });
    expect(directory.members()).toEqual([]);
    expect(directory.unavailableFor(shift('2026-12-01', 8 * 60, 20 * 60)).size).toBe(0);
    expect(directory.name('a')).toBe('Unknown member');
  });

  it('asks for the members once and for each week once, and keeps active members on the active roster with a role, by name', () => {
    directory.load('2026-11-30');
    directory.load('2026-11-30');

    http()
      .expectOne('/api/v1/members')
      .flush([
        { id: 'b', fullName: 'Rui Almeida', active: true, rosterStatus: 'ACTIVE_ROSTER', functions: ['DRIVER'] },
        { id: 'a', fullName: 'Ana Costa', active: true, rosterStatus: 'ACTIVE_ROSTER', functions: ['CREW_CHIEF', 'CREW_MEMBER'] },
        { id: 'c', fullName: 'Inactive Member', active: false, rosterStatus: 'ACTIVE_ROSTER', functions: ['DRIVER'] },
        { id: 'd', fullName: 'No Roles', active: true, rosterStatus: 'ACTIVE_ROSTER', functions: [] },
        // RN01: off the active roster, even with a function (TC-SCRUM-65-04, -07)
        { id: 'e', fullName: 'Synthetic Admin', active: true, rosterStatus: 'COMMAND_ROSTER', functions: ['CREW_CHIEF'] },
        { id: 'f', fullName: 'Reserve Member', active: true, rosterStatus: 'RESERVE_ROSTER', functions: ['DRIVER'] },
      ]);
    const week = http().expectOne((req) => req.url === '/api/v1/unavailabilities/team');
    expect(week.request.params.get('week')).toBe('2026-W49');
    week.flush([]);

    expect(directory.members()).toEqual([
      { id: 'a', name: 'Ana Costa', roles: ['CREW_CHIEF', 'CREW_MEMBER'] },
      { id: 'b', name: 'Rui Almeida', roles: ['DRIVER'] },
    ]);
    expect(directory.name('a')).toBe('Ana Costa');
    expect(directory.name('zzz')).toBe('Unknown member');

    // Another week asks only for its own periods.
    directory.load('2026-12-07');
    http()
      .expectOne((req) => req.url === '/api/v1/unavailabilities/team')
      .flush([]);
  });

  it("counts who can't come for some or all of the shift's time, also after midnight for a night shift", () => {
    directory.load('2026-11-30');
    http().expectOne('/api/v1/members').flush([]);
    http()
      .expectOne((req) => req.url === '/api/v1/unavailabilities/team')
      .flush([
        { memberId: 'morning', startsAt: '2026-12-01T00:00:00Z', endsAt: '2026-12-01T08:00:00Z' },
        { memberId: 'evening', startsAt: '2026-12-01T19:00:00Z', endsAt: '2026-12-01T23:00:00Z' },
        { memberId: 'early', startsAt: '2026-12-02T05:00:00Z', endsAt: '2026-12-02T07:00:00Z' },
      ]);

    // 08:00 to 20:00: "morning" ends as it starts; "evening" overlaps its last hour.
    expect([...directory.unavailableFor(shift('2026-12-01', 8 * 60, 20 * 60))]).toEqual(['evening']);
    // 20:00 to 08:00 the next day: "evening" and "early", after midnight.
    expect([...directory.unavailableFor(shift('2026-12-01', 20 * 60, 32 * 60))].sort()).toEqual(['early', 'evening']);
  });

  it('says so when the members cannot be loaded, and asks again the next time', () => {
    directory.load('2026-11-30');
    http().expectOne('/api/v1/members').flush(null, { status: 500, statusText: 'Server Error' });
    http()
      .expectOne((req) => req.url === '/api/v1/unavailabilities/team')
      .flush([]);
    expect(directory.error()).toBe('Could not load the members.');

    directory.load('2026-11-30');
    http().expectOne('/api/v1/members').flush([]);
    expect(directory.error()).toBeNull();
  });
});
