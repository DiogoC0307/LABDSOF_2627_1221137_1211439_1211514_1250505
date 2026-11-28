import { TestBed } from '@angular/core/testing';
import { AuthService } from '../auth/auth.service';
import { http, signInAs, testProviders } from '../testing/test-support';
import { ScheduleDirectory } from './schedule-directory';

// Schedule page's members, from the API: their names and functions only (who may be scheduled is the worker's)
describe('ScheduleDirectory', () => {
  let directory: ScheduleDirectory;

  beforeEach(() => {
    signInAs('SUPERVISOR');
    TestBed.configureTestingModule({ providers: testProviders });
    directory = TestBed.inject(ScheduleDirectory);
  });

  afterEach(() => {
    http().verify();
    localStorage.clear();
  });

  it('never shows the members to someone else who signs in on this device', () => {
    directory.load();
    http()
      .expectOne('/api/v1/members')
      .flush([{ id: 'a', fullName: 'Ana Costa', active: true, rosterStatus: 'ACTIVE_ROSTER', functions: ['CREW_CHIEF'] }]);
    expect(directory.members().length).toBe(1);

    TestBed.inject(AuthService).session.set({
      token: 'mock.MEMBER',
      memberId: '00000000-0000-4000-8000-000000000001',
      role: 'MEMBER',
      fullName: 'Synthetic Member',
    });
    expect(directory.members()).toEqual([]);
    expect(directory.name('a')).toBe('Unknown member');
  });

  it('asks for the members once, and keeps every member with a role, by name, whatever their roster', () => {
    directory.load();
    directory.load();

    http()
      .expectOne('/api/v1/members')
      .flush([
        { id: 'b', fullName: 'Rui Almeida', active: true, rosterStatus: 'ACTIVE_ROSTER', functions: ['DRIVER'] },
        { id: 'a', fullName: 'Ana Costa', active: true, rosterStatus: 'ACTIVE_ROSTER', functions: ['CREW_CHIEF', 'CREW_MEMBER'] },
        { id: 'd', fullName: 'No Roles', active: true, rosterStatus: 'ACTIVE_ROSTER', functions: [] },
        // RN01 is the worker's: a member off the active roster is listed, and the worker refuses them
        { id: 'e', fullName: 'Synthetic Admin', active: true, rosterStatus: 'COMMAND_ROSTER', functions: ['CREW_CHIEF'] },
      ]);
    http().expectNone('/api/v1/unavailabilities/team');

    expect(directory.members()).toEqual([
      { id: 'a', name: 'Ana Costa', roles: ['CREW_CHIEF', 'CREW_MEMBER'] },
      { id: 'b', name: 'Rui Almeida', roles: ['DRIVER'] },
      { id: 'e', name: 'Synthetic Admin', roles: ['CREW_CHIEF'] },
    ]);
    expect(directory.name('a')).toBe('Ana Costa');
    expect(directory.name('zzz')).toBe('Unknown member');
  });

  it('says so when the members cannot be loaded, and asks again the next time', () => {
    directory.load();
    http().expectOne('/api/v1/members').flush(null, { status: 500, statusText: 'Server Error' });
    expect(directory.error()).toBe('Could not load the members.');

    directory.load();
    http().expectOne('/api/v1/members').flush([]);
    expect(directory.error()).toBeNull();
  });
});
