import { TestBed } from '@angular/core/testing';
import { Schedule } from './schedule';
import { http, signInAs, testProviders, weekRequest } from '../testing/test-support';

/** The Schedule page is the week's schedule (SCRUM-65); week-schedule.spec.ts tests the week view itself. */
describe('Schedule', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Schedule],
      providers: testProviders,
    }).compileComponents();
  });

  afterEach(() => {
    http().verify();
    localStorage.clear();
  });

  function render(role: string): HTMLElement {
    signInAs(role);
    const fixture = TestBed.createComponent(Schedule);
    fixture.detectChanges();
    weekRequest().flush([]);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it("shows only the week's schedule, with no today's piquete, list or create form", () => {
    const page = render('SUPERVISOR');
    expect(page.querySelector('.week-grid')).not.toBeNull();
    expect(page.querySelector('form')).toBeNull();
    expect(page.textContent).not.toContain("Today's piquete");
    expect(page.textContent).not.toContain('All piquetes');
  });

  it.each(['MEMBER', 'ADMIN'])(
    'TC-SCRUM-63-01 "%s" signed in sees the week but cannot add shifts',
    (role) => {
      const page = render(role);
      expect(page.querySelectorAll('.day-column').length).toBe(7);
      expect(page.querySelectorAll('.add-slot').length).toBe(0);
      expect(page.textContent).not.toContain('Edit schedule');
      expect(page.textContent).not.toContain('Publish schedule');
    },
  );
});
