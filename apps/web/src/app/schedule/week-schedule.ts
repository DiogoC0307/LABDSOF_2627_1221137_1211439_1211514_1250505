import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  OnDestroy,
  OnInit,
  afterNextRender,
  computed,
  effect,
  inject,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { CdkTrapFocus } from '@angular/cdk/a11y';
import { NgTemplateOutlet } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { AuthService, LAST_PIQUETES_KEY } from '../auth/auth.service';
import { addDays, isoWeek, longDay, mondayOf, shortDay, todayInLisbon, weekDays } from './iso-week';
import { Piquete } from './piquete';
import {
  DeleteShiftData,
  DeleteShiftDialog,
  PublishData,
  PublishDialog,
  scheduleDialogConfig,
} from './schedule-dialogs';
import { ScheduleDirectory } from './schedule-directory';
import { ScheduleDraftService } from './schedule-draft.service';
import { DraftShift } from './schedule-model';
import {
  Range,
  ShiftState,
  assignedCount,
  daySegments,
  freeGaps,
  fromPiquete,
  missingCount,
  newShiftRange,
  rangeLabel,
  rangeWords,
  shiftState,
  timeLabel,
  totalOf,
} from './schedule-rules';
import { NewShift, NewShiftData, NewShiftDialog } from './new-shift-dialog';
import { ShiftPanel } from './shift-panel';
import { PlacedSegment, placeSegments } from './shift-time';
import { weekRange } from './week-label';
import { WeekSelector } from './week-selector';

type WeekKind = 'past' | 'current' | 'future';

type StatusTone = 'green' | 'amber' | 'grey';

type EditAction = 'edit' | 'stop' | 'discard' | null; // The button left of Publish

// A shift as a card in the grid, or as a row in the phone's list
interface ShiftCard {
  key: string;
  shift: DraftShift;
  state: ShiftState;
  selected: boolean;
  top: number;
  height: number;
  left: number;
  width: number;
  from: string; // "00:00"
  to: string; // "08:00"
  people: string; // "4 of 5 firefighters"
  tag: string; // "Full" or "Needs 2 more"
  change: string | null; // "New" or "Edited" while unpublished
  label: string;
}

// "+ Add shift" in a free time of a day
interface AddSlot {
  key: string;
  date: string;
  from: number;
  to: number;
  top: number;
  height: number;
  range: string;
  label: string;
}

// A row of a day in the phone's list: a shift or a free time
interface DayEntry {
  key: string;
  card?: ShiftCard;
  slot?: AddSlot;
}

// A day of the week: its column in the grid, or its card in the phone's list
interface DayColumn {
  date: string;
  header: string; // "Thu 26"
  title: string; // "Thursday 26 November", in the phone's list
  today: boolean;
  past: boolean;
  cards: ShiftCard[];
  slots: AddSlot[];
  entries: DayEntry[]; // The day's shifts (a night shift only on the day it starts) and free times, in order
  add: (Range & { label: string }) | null; // The day's own Add shift button, with the times its dialog opens on
}

/** The last week loaded, kept so the roster stays readable offline (RNF06); sign-out removes it. */
interface CachedWeek {
  week: string;
  piquetes: Piquete[];
}

/**
 * Schedule page: the week's grid, all 24 hours of the seven days, with the selected shift in a side panel.
 *
 * The supervisor edits a draft of the week and publishes it; members see nothing before that.
 *
 * A past week is read-only, this week is read-only until "Edit schedule" and later weeks are always editable. A shift
 * on a day that has passed never changes.
 */
@Component({
  selector: 'app-week-schedule',
  imports: [WeekSelector, ShiftPanel, MatButton, NgTemplateOutlet, CdkTrapFocus],
  templateUrl: './week-schedule.html',
  styleUrl: './week-schedule.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'schedule-theme flex flex-col lg:flex-1 lg:min-h-0' }, // Takes the height left under the menu
})
export class WeekSchedule implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly injector = inject(Injector);
  protected readonly auth = inject(AuthService);
  protected readonly drafts = inject(ScheduleDraftService);
  private readonly directory = inject(ScheduleDirectory);
  private request?: Subscription;

  private readonly grid = viewChild<ElementRef<HTMLElement>>('grid');

  protected readonly today = todayInLisbon();
  protected readonly currentMonday = mondayOf(this.today);

  protected readonly monday = signal(this.currentMonday); // The Monday of the week shown
  protected readonly error = signal<string | null>(null);
  protected readonly offline = signal(false);

  /**
   * Desktop (64rem and wider): the week's grid with the side panel beside it.
   *
   * Phone: the days as a list and the side panel as a drawer over the page. Without matchMedia (tests) it's a desktop.
   */
  protected readonly wide = signal(true);
  private readonly media = typeof matchMedia === 'function' ? matchMedia('(min-width: 64rem)') : null;
  private readonly onMediaChange = (event: MediaQueryListEvent) => this.wide.set(event.matches);

  protected readonly editing = signal(false); // This week is read-only until "Edit schedule" is pressed
  protected readonly selectedId = signal<string | null>(null);

  // The members and who can't come in the week, loaded for the supervisor when a shift is opened
  private readonly loadDirectory = effect(() => {
    if (this.selectedId() !== null && this.auth.isSupervisor()) {
      const monday = this.monday();
      untracked(() => this.directory.load(monday));
    }
  });

  // Hour labels down the left side, every 2 hours ("00:00", "02:00" ... "22:00")
  protected readonly hours = Array.from({ length: 12 }, (_, index) => ({
    label: `${String(index * 2).padStart(2, '0')}:00`,
    top: (index / 12) * 100,
  }));

  // Past, current or future week
  protected readonly weekKind = computed<WeekKind>(() => {
    if (addDays(this.monday(), 6) < this.today) {
      return 'past';
    }

    return this.monday() === this.currentMonday ? 'current' : 'future';
  });

  // The supervisor can change the week now (days before today still can't)
  protected readonly editable = computed(
    () =>
      this.auth.isSupervisor() &&
      this.drafts.week(this.monday()) !== undefined &&
      (this.weekKind() === 'future' || (this.weekKind() === 'current' && this.editing())),
  );

  protected readonly shifts = computed(() => this.drafts.shifts(this.monday()));
  protected readonly changes = computed(() => this.drafts.changes(this.monday()));

  // Status label above the Edit and Publish buttons
  protected readonly status = computed<{ text: string; tone: StatusTone }>(() => {
    const changes = this.changes();

    if (this.weekKind() === 'past') {
      return { text: "Past week · can't be changed", tone: 'grey' };
    }

    if (changes > 0) {
      return { text: `${changes} unpublished ${changes === 1 ? 'change' : 'changes'}`, tone: 'amber' };
    }

    if (this.weekKind() === 'current' && this.editing()) {
      return { text: 'Editing · no changes yet', tone: 'grey' };
    }

    if (this.shifts().length === 0) {
      return { text: 'No shifts yet', tone: 'grey' };
    }

    return { text: 'Published', tone: 'green' };
  });

  // Today's day, shown first on a phone in this week only
  protected readonly todayColumn = computed(() =>
    this.weekKind() === 'current' ? (this.days().find((day) => day.today) ?? null) : null,
  );

  // Edit schedule, Stop editing or Discard changes
  protected readonly editAction = computed<EditAction>(() => {
    if (!this.auth.isSupervisor() || this.weekKind() === 'past') {
      return null;
    }

    if (this.changes() > 0) {
      return 'discard';
    }

    if (this.weekKind() === 'current') {
      return this.editing() ? 'stop' : 'edit';
    }

    return null;
  });

  protected readonly canPublish = computed(() => this.weekKind() !== 'past' && this.changes() > 0);

  // The seven days of the week, with their shift cards and Add shift slots and buttons
  protected readonly days = computed<DayColumn[]>(() => {
    const shifts = this.shifts();

    // A night shift is split into its day and the next morning
    const segments = shifts.flatMap((shift) =>
      daySegments(shift).map((segment) => ({ ...segment, label: '', item: shift })),
    );

    return weekDays(this.monday()).map((date) => {
      const ofDay = segments.filter((segment) => segment.date === date);
      const cards = placeSegments(ofDay).map((placed) => this.card(placed.item, placed));

      // Add shift slots in the free times, only on days that can still change
      const canAdd = this.editable() && date >= this.today;
      const gaps = canAdd ? freeGaps(ofDay) : [];
      const slots = gaps.map((gap) => this.slot(date, gap.from, gap.to));

      // The day's own Add shift button opens on the first free time, or 08:00 to 20:00 when the day is all covered
      const suggested = gaps.length > 0 ? newShiftRange(gaps[0]) : { from: 8 * 60, to: 20 * 60 };

      // The phone's list: shifts (a night shift only on the day it starts) and free times, in order
      const entries: (DayEntry & { at: number })[] = [
        ...cards
          .filter((card) => card.shift.dutyDate === date)
          .map((card) => ({ key: card.key, card, at: card.shift.start })),
        ...slots.map((slot) => ({ key: slot.key, slot, at: slot.from })),
      ];

      return {
        date,
        header: shortDay(date),
        title: longDay(date),
        today: date === this.today,
        past: date < this.today,
        cards,
        slots,
        entries: entries.sort((a, b) => a.at - b.at),
        add: canAdd ? { ...suggested, label: `Add shift on ${longDay(date)}` } : null,
      };
    });
  });

  // The shift open in the side panel
  protected readonly selected = computed(() => this.shifts().find((shift) => shift.id === this.selectedId()) ?? null);

  protected readonly selectedEditable = computed(() => {
    const shift = this.selected();

    return shift !== null && this.editable() && shift.dutyDate >= this.today;
  });

  // Why the selected shift can't be changed, shown on top of the side panel
  protected readonly selectedNote = computed(() => {
    const shift = this.selected();

    if (!shift || this.selectedEditable()) {
      return null;
    }

    if (shift.dutyDate < this.today) {
      return "This shift has already happened, so it can't be changed.";
    }

    if (this.weekKind() === 'current' && this.auth.isSupervisor()) {
      return 'This week is published. Press "Edit schedule" to make changes.';
    }

    return null;
  });

  protected readonly selectedDayShifts = computed(() => {
    const shift = this.selected();
    return shift ? this.shifts().filter((each) => each.dutyDate === shift.dutyDate) : [];
  });

  ngOnInit(): void {
    // Desktop or phone, and again whenever the screen width crosses 64rem
    if (this.media) {
      this.wide.set(this.media.matches);
      this.media.addEventListener('change', this.onMediaChange);
    }

    this.load();
  }

  ngOnDestroy(): void {
    this.media?.removeEventListener('change', this.onMediaChange);
    this.request?.unsubscribe();
  }

  // Shows another week, chosen in the week selector
  protected goTo(monday: string): void {
    if (monday !== this.monday()) {
      this.selectedId.set(null);
      this.monday.set(monday);
      this.load();
    }
  }

  // Edit schedule and Stop editing, for this week
  protected startEditing(): void {
    this.editing.set(true);
  }

  protected stopEditing(): void {
    this.editing.set(false);
  }

  // Back to the week as published, with no dialog; editing ends
  protected discard(): void {
    this.drafts.discard(this.monday());
    this.editing.set(false);
  }

  // Opens a shift in the side panel
  protected select(shift: DraftShift): void {
    this.selectedId.set(shift.id);
  }

  // "+ Add shift" in a free time: the dialog opens on that time, at most 12 hours of it
  protected addShift(slot: AddSlot): void {
    this.askNewShift(slot.date, newShiftRange({ from: slot.from, to: slot.to }));
  }

  // The day's own Add shift button
  protected addOnDay(day: DayColumn): void {
    if (day.add) {
      this.askNewShift(day.date, day.add);
    }
  }

  /**
   * Add shift dialog: asks for the new shift's times and roles
   *
   * "Create shift" adds it to the draft, selected so the side panel opens on it.
   */
  private askNewShift(date: string, range: Range): void {
    const data: NewShiftData = { day: longDay(date), start: range.from, end: range.to };

    this.dialog
      .open<NewShiftDialog, NewShiftData, NewShift>(NewShiftDialog, {
        ...scheduleDialogConfig(data, 'dialog'),
        // The title, not the Start box: its list of hours would open over the dialog.
        autoFocus: 'first-heading',
      })
      .afterClosed()
      .subscribe((created) => {
        if (created) {
          const shift = this.drafts.add(this.monday(), date, created.start, created.end, created.crew);
          this.selectedId.set(shift.id);
        }
      });
  }

  // Closes the side panel and gives the focus back to the shift's card
  protected closePanel(): void {
    const id = this.selectedId();
    this.selectedId.set(null);
    this.focusLater(id ? `[data-shift-id="${id}"]` : null);
  }

  // Delete shift dialog, then removes the shift from the draft
  protected confirmDelete(): void {
    const shift = this.selected();

    if (!shift) {
      return;
    }

    const data: DeleteShiftData = {
      day: longDay(shift.dutyDate),
      time: rangeLabel(shift.start, shift.end),
      people: assignedCount(shift),
    };

    this.dialog
      .open(DeleteShiftDialog, scheduleDialogConfig(data, 'alertdialog'))
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.drafts.remove(this.monday(), shift.id);
          this.selectedId.set(null);
          this.focusLater(null);
        }
      });
  }

  // Publish schedule dialog, then publishes the week's draft
  protected confirmPublish(): void {
    const data: PublishData = { range: weekRange(this.monday()), changes: this.changes() };

    this.dialog
      .open(PublishDialog, scheduleDialogConfig(data, 'dialog'))
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.drafts.publish(this.monday());
          this.editing.set(false);
        }
      });
  }

  // A shift's card, placed at its hours in the day's column
  private card(shift: DraftShift, placed: PlacedSegment<DraftShift>): ShiftCard {
    const required = totalOf(shift.crew);
    const assigned = assignedCount(shift);
    const missing = missingCount(shift);

    // A shift from before roles has no roles yet
    const people =
      required === 0 ? 'No roles yet' : `${assigned} of ${required} ${required === 1 ? 'firefighter' : 'firefighters'}`;

    return {
      key: `${shift.id}-${placed.from}`,
      shift,
      state: shiftState(shift, this.today),
      selected: shift.id === this.selectedId(),
      top: placed.topPercent,
      height: placed.heightPercent,
      left: placed.leftPercent,
      width: placed.widthPercent,
      from: timeLabel(shift.start),
      to: timeLabel(shift.end),
      people,
      tag: required === 0 ? 'Choose roles' : missing === 0 ? 'Full' : `Needs ${missing} more`,
      change: shift.change === 'new' ? 'New' : shift.change === 'edited' ? 'Edited' : null,
      label: `${longDay(shift.dutyDate).split(' ')[0]} ${rangeWords(shift.start, shift.end)}, ${people}`,
    };
  }

  // An Add shift slot, placed at its hours in the day's column
  private slot(date: string, from: number, to: number): AddSlot {
    return {
      key: `${date}-${from}`,
      date,
      from,
      to,
      top: (from / 1440) * 100,
      height: ((to - from) / 1440) * 100,
      range: rangeLabel(from, to),
      label: `Add shift on ${longDay(date).split(' ')[0]}, ${rangeWords(from, to)}`,
    };
  }

  // After the next render, focuses the element, or the grid when it is gone
  private focusLater(selector: string | null): void {
    afterNextRender(
      () => {
        const grid = this.grid()?.nativeElement;
        const target = selector ? grid?.querySelector<HTMLElement>(selector) : null;
        (target ?? grid)?.focus();
      },
      { injector: this.injector },
    );
  }

  private load(): void {
    const monday = this.monday();
    const week = isoWeek(monday);
    // A newer week replaces a request still on its way, so a slow answer never fills the wrong week.
    this.request?.unsubscribe();
    this.request = this.http
      .get<Piquete[]>('/api/v1/piquetes', { params: new HttpParams().set('week', week) })
      .subscribe({
        next: (list) => {
          this.error.set(null);
          this.offline.set(false);
          this.drafts.load(monday, toDraft(list));
          writeCache({ week, piquetes: list });
        },
        error: () => {
          if (!this.auth.session()) {
            // 401: the interceptor signed out; the shell shows the sign-in screen, with no roster.
            return;
          }
          const cached = readCache();
          if (cached?.week === week) {
            this.error.set(null);
            this.offline.set(true);
            this.drafts.load(monday, toDraft(cached.piquetes));
            return;
          }
          this.offline.set(false);
          this.error.set("Could not load the week's schedule.");
        },
      });
  }
}

// The week's piquetes from the API as draft shifts, without those that have no times yet
function toDraft(list: Piquete[]): DraftShift[] {
  return list.map(fromPiquete).filter((shift): shift is DraftShift => shift !== null);
}

function writeCache(cached: CachedWeek): void {
  try {
    localStorage.setItem(LAST_PIQUETES_KEY, JSON.stringify(cached));
  } catch {
    // Storage full or blocked: the roster still shows, it just is not kept for offline use.
  }
}

function readCache(): CachedWeek | null {
  try {
    const parsed = JSON.parse(localStorage.getItem(LAST_PIQUETES_KEY) ?? 'null') as Partial<CachedWeek> | null;
    return parsed && typeof parsed.week === 'string' && Array.isArray(parsed.piquetes)
      ? { week: parsed.week, piquetes: parsed.piquetes }
      : null;
  } catch {
    return null;
  }
}
