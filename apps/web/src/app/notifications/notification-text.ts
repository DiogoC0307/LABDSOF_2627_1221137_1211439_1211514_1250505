import { addDays, mondayOfIsoWeek, todayInLisbon } from '../schedule/iso-week';
import { ROLE_LABELS } from '../schedule/piquete';
import { lisbonTime } from '../schedule/shift-time';
import { weekRange } from '../schedule/week-label';
import { AppNotification, NotificationType } from './notification.service';

/** What a notification says: a short title and one line of detail. */
export interface NotificationCopy {
  title: string;
  detail: string;
}

/** How a notification looks in the list: its icon and the colour family of the icon circle. */
export interface NotificationLook {
  icon: string;
  kind: 'published' | 'changed' | 'assigned' | 'removed' | 'cancelled';
}

const SHORT_DAY = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'UTC',
  weekday: 'short',
  day: 'numeric',
  month: 'short',
});
const SHORT_DAY_WITH_YEAR = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'UTC',
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
});
const WEEKDAY = new Intl.DateTimeFormat('en-GB', { timeZone: 'UTC', weekday: 'long' });

const MINUTE_MS = 60 * 1000;
const HOUR_MS = 60 * MINUTE_MS;

const LOOKS: Record<NotificationType, NotificationLook> = {
  WEEKLY_SCHEDULE_PUBLISHED: { icon: 'event_available', kind: 'published' },
  SHIFT_TIMES_CHANGED: { icon: 'schedule', kind: 'changed' },
  ADDED_TO_SHIFT: { icon: 'person_add', kind: 'assigned' },
  REMOVED_FROM_SHIFT: { icon: 'person_remove', kind: 'removed' },
  SHIFT_CANCELLED: { icon: 'event_busy', kind: 'cancelled' },
  SLOT_OPENED: { icon: 'person_off', kind: 'removed' },
  COVER_REQUESTED: { icon: 'front_hand', kind: 'assigned' },
  COVER_ESCALATED: { icon: 'warning', kind: 'cancelled' },
};

/**
 * A notification's title and detail
 *
 * "Schedule published" / "30 Nov – 6 Dec 2026 is ready. You are on 3 shifts."
 * "Your shift changed" / "Thu 3 Dec is now 08:00 – 20:00 (was 08:00 – 16:00)."
 * "You were assigned to a shift" / "Sat 5 Dec, 20:00 – 24:00"
 * "Can you cover a shift?" / "Driver, Sat 5 Dec, 20:00 – 08:00 (next day)"
 */
export function notificationCopy(notification: AppNotification): NotificationCopy {
  switch (notification.type) {
    case 'WEEKLY_SCHEDULE_PUBLISHED': {
      const range = weekRange(mondayOfIsoWeek(notification.week));
      const count = notification.shiftCount;
      if (count == null) {
        return { title: 'Schedule published', detail: `${range} is ready.` };
      }
      const shifts = count === 1 ? 'shift' : 'shifts';
      return { title: 'Schedule published', detail: `${range} is ready. You are on ${count} ${shifts}.` };
    }
    case 'SHIFT_TIMES_CHANGED':
      return {
        title: 'Your shift changed',
        detail: `${dayOf(notification.startsAt)} is now ${times(notification.startsAt, notification.endsAt)} (was ${times(
          notification.previousStartsAt,
          notification.previousEndsAt,
        )}).`,
      };
    case 'ADDED_TO_SHIFT':
      return { title: 'You were assigned to a shift', detail: shiftWords(notification) };
    case 'REMOVED_FROM_SHIFT':
      return { title: 'You were taken off a shift', detail: shiftWords(notification) };
    case 'SHIFT_CANCELLED':
      return { title: 'Your shift was cancelled', detail: shiftWords(notification) };
    case 'COVER_REQUESTED':
      return { title: 'Can you cover a shift?', detail: slotWords(notification) };
    case 'SLOT_OPENED':
      return {
        title: 'A slot is open',
        detail: `${slotWords(notification)}. ${notification.reason === 'NO_ANSWER' ? "The member didn't confirm" : "The member can't come"}; others are being asked.`,
      };
    case 'COVER_ESCALATED':
      return notification.reason === 'NO_ACCEPTANCE'
        ? { title: 'Nobody has covered yet', detail: `${slotWords(notification)}. Nobody accepted in time.` }
        : { title: 'Nobody can cover', detail: `${slotWords(notification)}. No member is free and qualified.` };
  }
}

/** The icon and colours of a notification's type. */
export function notificationLook(type: NotificationType): NotificationLook {
  return LOOKS[type];
}

/**
 * How long ago a notification arrived, on the Lisbon calendar
 *
 * "Just now", "10 minutes ago", "2 hours ago" (today), "Yesterday", "Monday" (within 7 days), "Sun 22 Nov", and
 * "Mon 22 Dec 2025" for another year.
 */
export function notificationAge(createdAt: string, now: Date): string {
  const ago = now.getTime() - new Date(createdAt).getTime();
  if (ago < MINUTE_MS) {
    return 'Just now';
  }
  if (ago < HOUR_MS) {
    const minutes = Math.floor(ago / MINUTE_MS);
    return `${minutes} ${minutes === 1 ? 'minute' : 'minutes'} ago`;
  }

  const today = todayInLisbon(now);
  const day = lisbonTime(createdAt).date;
  if (day === today) {
    const hours = Math.floor(ago / HOUR_MS);
    return `${hours} ${hours === 1 ? 'hour' : 'hours'} ago`;
  }
  if (day === addDays(today, -1)) {
    return 'Yesterday';
  }
  if (day >= addDays(today, -6)) {
    return WEEKDAY.format(utcDate(day));
  }
  const format = day.slice(0, 4) === today.slice(0, 4) ? SHORT_DAY : SHORT_DAY_WITH_YEAR;
  return format.format(utcDate(day)).replace(',', '');
}

/**
 * Splits the notifications into "New" (unread) and "Earlier" (read), each newest first
 *
 * `keptNew` holds those that stay under "New" although they were read since, so a row the member just opened does
 * not jump to the other group under their pointer.
 */
export function groupNotifications<T extends Pick<AppNotification, 'id' | 'read' | 'createdAt'>>(
  list: readonly T[],
  keptNew: ReadonlySet<string> = new Set(),
): { fresh: T[]; earlier: T[] } {
  const newestFirst = [...list].sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt));
  const isNew = (each: T) => !each.read || keptNew.has(each.id);
  return { fresh: newestFirst.filter(isNew), earlier: newestFirst.filter((each) => !isNew(each)) };
}

// "Sat 5 Dec, 20:00 – 24:00"
function shiftWords(notification: AppNotification): string {
  return `${dayOf(notification.startsAt)}, ${times(notification.startsAt, notification.endsAt)}`;
}

// "Driver, Sat 5 Dec, 20:00 – 08:00 (next day)": the open slot's function and its shift
function slotWords(notification: AppNotification): string {
  const role = notification.role ? ROLE_LABELS[notification.role] : 'A slot';
  return `${role}, ${shiftWords(notification)}`;
}

// "Thu 3 Dec", the day the shift starts on the Lisbon clock
function dayOf(instant?: string | null): string {
  return instant ? SHORT_DAY.format(utcDate(lisbonTime(instant).date)).replace(',', '') : 'A day of the week';
}

// "08:00 – 20:00"; an end at midnight is "24:00", and an end later the next day says so
function times(startsAt?: string | null, endsAt?: string | null): string {
  if (!startsAt || !endsAt) {
    return 'times not set';
  }
  const start = lisbonTime(startsAt);
  const end = lisbonTime(endsAt);
  if (end.date === addDays(start.date, 1)) {
    return end.time === '00:00' ? `${start.time} – 24:00` : `${start.time} – ${end.time} (next day)`;
  }
  return `${start.time} – ${end.time}`;
}

// "YYYY-MM-DD" as a date at midnight UTC, for the formats above
function utcDate(date: string): Date {
  const [year, month, day] = date.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}
