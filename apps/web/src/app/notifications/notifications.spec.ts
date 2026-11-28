import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthService } from '../auth/auth.service';
import { mondayOfIsoWeek } from '../schedule/iso-week';
import { http, signInAs, testProviders } from '../testing/test-support';
import { NotificationBell } from './notification-bell';
import { groupNotifications, notificationAge, notificationCopy } from './notification-text';
import { AppNotification, MARK_READ_FAILED, NOTIFICATIONS_EVERY_MS, NotificationService } from './notification.service';

/** The member's notifications: what they say, when they arrived, how they are grouped, and the bell. */
describe('Notifications', () => {
  function notification(change: Partial<AppNotification>): AppNotification {
    return {
      id: 'n-1',
      type: 'WEEKLY_SCHEDULE_PUBLISHED',
      week: '2026-W49',
      createdAt: '2026-11-25T14:32:00Z',
      read: false,
      ...change,
    };
  }

  describe('their title and detail', () => {
    it('says the week is published and how many shifts the member is on, when that is known', () => {
      expect(notificationCopy(notification({ shiftCount: 3 }))).toEqual({
        title: 'Schedule published',
        detail: '30 Nov – 6 Dec 2026 is ready. You are on 3 shifts.',
      });
      expect(notificationCopy(notification({ shiftCount: 1 })).detail).toBe('30 Nov – 6 Dec 2026 is ready. You are on 1 shift.');
      expect(notificationCopy(notification({ shiftCount: null })).detail).toBe('30 Nov – 6 Dec 2026 is ready.');
    });

    it('says what the times of a shift are now and were before', () => {
      expect(
        notificationCopy(
          notification({
            type: 'SHIFT_TIMES_CHANGED',
            startsAt: '2026-12-03T08:00:00Z',
            endsAt: '2026-12-03T20:00:00Z',
            previousStartsAt: '2026-12-03T08:00:00Z',
            previousEndsAt: '2026-12-03T16:00:00Z',
          }),
        ),
      ).toEqual({ title: 'Your shift changed', detail: 'Thu 3 Dec is now 08:00 – 20:00 (was 08:00 – 16:00).' });
    });

    it('says which shift the member was put on, taken off or lost, on the Lisbon clock', () => {
      expect(
        notificationCopy(notification({ type: 'ADDED_TO_SHIFT', startsAt: '2026-12-05T20:00:00Z', endsAt: '2026-12-06T00:00:00Z' })),
      ).toEqual({ title: 'You were assigned to a shift', detail: 'Sat 5 Dec, 20:00 – 24:00' });
      expect(
        notificationCopy(notification({ type: 'REMOVED_FROM_SHIFT', startsAt: '2026-12-01T00:00:00Z', endsAt: '2026-12-01T08:00:00Z' })),
      ).toEqual({ title: 'You were taken off a shift', detail: 'Tue 1 Dec, 00:00 – 08:00' });
      expect(
        notificationCopy(notification({ type: 'SHIFT_CANCELLED', startsAt: '2026-12-02T20:00:00Z', endsAt: '2026-12-03T08:00:00Z' })),
      ).toEqual({ title: 'Your shift was cancelled', detail: 'Wed 2 Dec, 20:00 – 08:00 (next day)' });
      // Summer time: 07:00 UTC is 08:00 in Lisbon
      expect(
        notificationCopy(notification({ type: 'ADDED_TO_SHIFT', startsAt: '2027-06-30T07:00:00Z', endsAt: '2027-06-30T19:00:00Z' })).detail,
      ).toBe('Wed 30 Jun, 08:00 – 20:00');
    });

    it('TC-SCRUM-72-05 asks a member to cover a slot, with its function and times, on the Lisbon clock', () => {
      expect(
        notificationCopy(
          notification({
            type: 'COVER_REQUESTED',
            role: 'DRIVER',
            coverRequestId: 'c-1',
            startsAt: '2026-12-05T20:00:00Z',
            endsAt: '2026-12-06T08:00:00Z',
          }),
        ),
      ).toEqual({ title: 'Can you cover a shift?', detail: 'Driver, Sat 5 Dec, 20:00 – 08:00 (next day)' });
    });

    it('TC-SCRUM-72-06 tells the supervisor a slot opened, and why nobody covers it yet', () => {
      const slot = { role: 'CREW_CHIEF' as const, startsAt: '2026-12-05T20:00:00Z', endsAt: '2026-12-06T08:00:00Z' };
      expect(notificationCopy(notification({ type: 'SLOT_OPENED', reason: 'CANNOT_COME', ...slot }))).toEqual({
        title: 'A slot is open',
        detail: "Crew chief, Sat 5 Dec, 20:00 – 08:00 (next day). The member can't come; others are being asked.",
      });
      expect(notificationCopy(notification({ type: 'SLOT_OPENED', reason: 'NO_ANSWER', ...slot }))).toEqual({
        title: 'A slot is open',
        detail: "Crew chief, Sat 5 Dec, 20:00 – 08:00 (next day). The member didn't confirm; others are being asked.",
      });
      expect(notificationCopy(notification({ type: 'COVER_ESCALATED', reason: 'NO_CANDIDATES', ...slot }))).toEqual({
        title: 'Nobody can cover',
        detail: 'Crew chief, Sat 5 Dec, 20:00 – 08:00 (next day). No member is free and qualified.',
      });
      expect(notificationCopy(notification({ type: 'COVER_ESCALATED', reason: 'NO_ACCEPTANCE', ...slot }))).toEqual({
        title: 'Nobody has covered yet',
        detail: 'Crew chief, Sat 5 Dec, 20:00 – 08:00 (next day). Nobody accepted in time.',
      });
    });

    it('finds the Monday of an ISO week, across years', () => {
      expect(mondayOfIsoWeek('2026-W49')).toBe('2026-11-30');
      expect(mondayOfIsoWeek('2026-W53')).toBe('2026-12-28');
      expect(mondayOfIsoWeek('2027-W01')).toBe('2027-01-04');
    });
  });

  describe('when they arrived', () => {
    const now = new Date('2026-11-25T15:00:00Z'); // A Wednesday

    it('counts minutes and hours today', () => {
      expect(notificationAge('2026-11-25T14:59:30Z', now)).toBe('Just now');
      expect(notificationAge('2026-11-25T15:00:05Z', now)).toBe('Just now'); // a device clock a little behind
      expect(notificationAge('2026-11-25T14:59:00Z', now)).toBe('1 minute ago');
      expect(notificationAge('2026-11-25T14:50:00Z', now)).toBe('10 minutes ago');
      expect(notificationAge('2026-11-25T14:00:00Z', now)).toBe('1 hour ago');
      expect(notificationAge('2026-11-25T13:00:00Z', now)).toBe('2 hours ago');
      expect(notificationAge('2026-11-25T00:10:00Z', now)).toBe('14 hours ago');
    });

    it('says "Yesterday", then the weekday for a week, then the date, with the year when it is another year', () => {
      expect(notificationAge('2026-11-24T20:00:00Z', now)).toBe('Yesterday');
      expect(notificationAge('2026-11-23T10:00:00Z', now)).toBe('Monday');
      expect(notificationAge('2026-11-19T10:00:00Z', now)).toBe('Thursday');
      expect(notificationAge('2026-11-18T10:00:00Z', now)).toBe('Wed 18 Nov');
      expect(notificationAge('2025-11-22T10:00:00Z', now)).toBe('Sat 22 Nov 2025');
    });

    it('goes by the Lisbon calendar, in winter and summer time', () => {
      expect(notificationAge('2026-11-24T23:00:00Z', new Date('2026-11-25T00:30:00Z'))).toBe('Yesterday');
      // 01:30 on 1 July in Lisbon (UTC+1): 23:00 the day before is yesterday, 00:10 the same day is today
      const summer = new Date('2026-07-01T00:30:00Z');
      expect(notificationAge('2026-06-30T22:00:00Z', summer)).toBe('Yesterday');
      expect(notificationAge('2026-06-30T23:10:00Z', summer)).toBe('1 hour ago');
    });
  });

  describe('their groups', () => {
    it('puts the unread under "New" and the read under "Earlier", each newest first', () => {
      const { fresh, earlier } = groupNotifications([
        notification({ id: 'old-unread', createdAt: '2026-11-20T10:00:00Z' }),
        notification({ id: 'read', createdAt: '2026-11-24T10:00:00Z', read: true }),
        notification({ id: 'new-unread', createdAt: '2026-11-25T10:00:00Z' }),
        notification({ id: 'old-read', createdAt: '2026-11-19T10:00:00Z', read: true }),
      ]);
      expect(fresh.map((each) => each.id)).toEqual(['new-unread', 'old-unread']);
      expect(earlier.map((each) => each.id)).toEqual(['read', 'old-read']);
    });

    it('keeps under "New" those read while the list is open', () => {
      const { fresh, earlier } = groupNotifications(
        [notification({ id: 'opened', read: true }), notification({ id: 'other', read: true, createdAt: '2026-11-01T10:00:00Z' })],
        new Set(['opened']),
      );
      expect(fresh.map((each) => each.id)).toEqual(['opened']);
      expect(earlier.map((each) => each.id)).toEqual(['other']);
    });

    it('has empty groups when there are none', () => {
      expect(groupNotifications([])).toEqual({ fresh: [], earlier: [] });
    });
  });

  describe('the bell', () => {
    let fixture: ComponentFixture<NotificationBell>;

    beforeEach(async () => {
      localStorage.clear();
      signInAs('MEMBER');
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-11-25T15:00:00Z'));
      await TestBed.configureTestingModule({ imports: [NotificationBell], providers: testProviders }).compileComponents();
      fixture = TestBed.createComponent(NotificationBell);
      fixture.detectChanges();
      vi.advanceTimersByTime(0); // The first request goes at once
    });

    afterEach(() => {
      http().verify();
      vi.useRealTimers();
      localStorage.clear();
    });

    function bell(): HTMLButtonElement {
      return (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.bell')!;
    }

    function panel(): HTMLElement | null {
      return document.querySelector<HTMLElement>('.panel');
    }

    function rows(): HTMLButtonElement[] {
      return Array.from(document.querySelectorAll<HTMLButtonElement>('.panel button.row'));
    }

    function answer(list: AppNotification[]) {
      http().expectOne('/api/v1/notifications').flush(list);
      fixture.detectChanges();
    }

    function openList() {
      bell().click();
      fixture.detectChanges();
    }

    function button(text: string): HTMLButtonElement {
      return Array.from(document.querySelectorAll<HTMLButtonElement>('.panel button')).find(
        (each) => each.textContent?.trim() === text,
      )!;
    }

    it('asks for the notifications at once and every 30 seconds, and shows how many are unread', () => {
      answer([notification({ id: 'n-1' }), notification({ id: 'n-2', read: true })]);
      expect(bell().getAttribute('aria-label')).toBe('Notifications, 1 unread');
      expect(bell().getAttribute('aria-haspopup')).toBe('dialog');
      expect(bell().querySelector('.badge')?.textContent).toBe('1');

      vi.advanceTimersByTime(NOTIFICATIONS_EVERY_MS - 1);
      http().expectNone('/api/v1/notifications');
      vi.advanceTimersByTime(1);
      answer([notification({ id: 'n-3' }), notification({ id: 'n-1' }), notification({ id: 'n-2', read: true })]);
      expect(bell().getAttribute('aria-label')).toBe('Notifications, 2 unread');
    });

    it('hides the badge when none are unread, and shows "9+" above nine', () => {
      answer([notification({ read: true })]);
      expect(bell().querySelector('.badge')).toBeNull();
      expect(bell().getAttribute('aria-label')).toBe('Notifications');

      vi.advanceTimersByTime(NOTIFICATIONS_EVERY_MS);
      answer(Array.from({ length: 10 }, (_, index) => notification({ id: `n-${index}` })));
      expect(bell().querySelector('.badge')?.textContent).toBe('9+');
    });

    it('opens the list without marking anything read, unread under "New" and read under "Earlier"', () => {
      answer([
        notification({ id: 'n-1', shiftCount: 2 }),
        notification({
          id: 'n-2',
          type: 'SHIFT_CANCELLED',
          startsAt: '2026-11-20T08:00:00Z',
          endsAt: '2026-11-20T20:00:00Z',
          createdAt: '2026-11-20T09:00:00Z',
          read: true,
        }),
      ]);
      openList();
      http().expectNone((req) => req.method === 'POST');

      expect(bell().getAttribute('aria-expanded')).toBe('true');
      expect(panel()?.getAttribute('role')).toBe('dialog');
      expect(Array.from(document.querySelectorAll('.group-label')).map((each) => each.textContent)).toEqual(['New', 'Earlier']);
      const [unread, read] = rows();
      expect(unread.textContent).toContain('Schedule published');
      expect(unread.textContent).toContain('30 Nov – 6 Dec 2026 is ready. You are on 2 shifts.');
      expect(unread.textContent).toContain('28 minutes ago');
      expect(unread.classList).toContain('row--unread');
      expect(unread.querySelector('.dot')?.getAttribute('aria-label')).toBe('Unread');
      expect(read.textContent).toContain('Your shift was cancelled');
      expect(read.textContent).toContain('Fri 20 Nov, 08:00 – 20:00');
      expect(read.textContent).toContain('Friday');
      expect(read.querySelector('.dot')).toBeNull();
      expect(button('Mark all as read')).toBeDefined();
    });

    it('leaves out the "New" label when nothing is unread, and "Mark all as read" too', () => {
      answer([notification({ read: true })]);
      openList();
      expect(Array.from(document.querySelectorAll('.group-label')).map((each) => each.textContent)).toEqual(['Earlier']);
      expect(button('Mark all as read')).toBeUndefined();
    });

    it('marks one read when it is opened, and keeps it under "New" while the list is open', () => {
      answer([notification({ id: 'n-1' }), notification({ id: 'n-2', createdAt: '2026-11-25T14:00:00Z' })]);
      openList();

      rows()[0].click();
      fixture.detectChanges();
      http().expectOne((req) => req.method === 'POST' && req.url === '/api/v1/notifications/n-1/read').flush(null);
      fixture.detectChanges();

      expect(bell().getAttribute('aria-label')).toBe('Notifications, 1 unread');
      expect(rows()[0].classList).not.toContain('row--unread');
      expect(rows()[0].querySelector('.dot')).toBeNull();
      expect(Array.from(document.querySelectorAll('.group-label')).map((each) => each.textContent)).toEqual(['New']);
      expect(panel()).not.toBeNull();

      // Opened again, the read one is under "Earlier"
      bell().click();
      fixture.detectChanges();
      openList();
      expect(Array.from(document.querySelectorAll('.group-label')).map((each) => each.textContent)).toEqual(['New', 'Earlier']);
    });

    it('marks all read at once, and puts them back with a message when the server refuses', () => {
      answer([notification({ id: 'n-1' }), notification({ id: 'n-2' })]);
      openList();

      button('Mark all as read').click();
      fixture.detectChanges();
      expect(bell().querySelector('.badge')).toBeNull();
      expect(button('Mark all as read')).toBeUndefined();
      expect(Array.from(document.querySelectorAll('.group-label')).map((each) => each.textContent)).toEqual(['Earlier']);

      http()
        .expectOne((req) => req.method === 'POST' && req.url === '/api/v1/notifications/read')
        .flush('', { status: 503, statusText: 'Service Unavailable' });
      fixture.detectChanges();
      expect(bell().querySelector('.badge')?.textContent).toBe('2');
      expect(document.querySelector('.mat-mdc-snack-bar-container')?.textContent).toContain(MARK_READ_FAILED);
    });

    it('says when there are none, with no "Mark all as read"', () => {
      answer([]);
      openList();
      expect(panel()?.textContent).toContain("You're all caught up");
      expect(panel()?.textContent).toContain(
        "We'll tell you here when a week's schedule is published or one of your shifts changes.",
      );
      expect(button('Mark all as read')).toBeUndefined();
    });

    it('shows grey rows while loading, and "Try again" when loading failed', () => {
      openList();
      expect(document.querySelectorAll('.panel .row--skeleton')).toHaveLength(3);

      http().expectOne('/api/v1/notifications').flush('', { status: 503, statusText: 'Service Unavailable' });
      fixture.detectChanges();
      expect(panel()?.textContent).toContain("Couldn't load notifications.");

      button('Try again').click();
      vi.advanceTimersByTime(0);
      fixture.detectChanges();
      expect(document.querySelectorAll('.panel .row--skeleton')).toHaveLength(3);
      answer([notification({})]);
      expect(rows()).toHaveLength(1);
    });

    it('closes on Escape and on the bell, and gives focus back to the bell', () => {
      answer([notification({})]);
      openList();
      panel()!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
      fixture.detectChanges();
      expect(panel()).toBeNull();
      expect(bell().getAttribute('aria-expanded')).toBe('false');
      expect(document.activeElement).toBe(bell());

      openList();
      bell().click();
      fixture.detectChanges();
      expect(panel()).toBeNull();
    });

    it('forgets them when the member signs out, and stops asking', () => {
      answer([notification({})]);
      TestBed.inject(AuthService).session.set(null);
      TestBed.tick();
      vi.advanceTimersByTime(0);
      fixture.detectChanges();
      expect(TestBed.inject(NotificationService).list()).toEqual([]);
      expect(bell().querySelector('.badge')).toBeNull();

      vi.advanceTimersByTime(NOTIFICATIONS_EVERY_MS * 2);
      http().expectNone('/api/v1/notifications');
    });

    it('tries again at the next tick when a request fails', () => {
      http().expectOne('/api/v1/notifications').flush('', { status: 503, statusText: 'Service Unavailable' });
      vi.advanceTimersByTime(NOTIFICATIONS_EVERY_MS);
      answer([notification({})]);
      expect(bell().querySelector('.badge')?.textContent).toBe('1');
    });
  });
});
