import { CdkTrapFocus } from '@angular/cdk/a11y';
import { CdkConnectedOverlay, CdkOverlayOrigin, ConnectedPosition } from '@angular/cdk/overlay';
import { NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterRenderEffect,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { groupNotifications, notificationAge, notificationCopy, notificationLook } from './notification-text';
import { AppNotification, NotificationService } from './notification.service';

// How many notifications show before the list scrolls
const ROWS_SHOWN = 4;

// The gap between the bell and the panel, and the space left under the panel above the bottom of the screen
const GAP_BELOW_BELL = 8;
const SCREEN_MARGIN = 16;

// Under the bell, its right edge on the bell's right edge; pushed back on screen when there is no room
const BELOW_RIGHT: ConnectedPosition[] = [
  { originX: 'end', originY: 'bottom', overlayX: 'end', overlayY: 'top', offsetY: GAP_BELOW_BELL },
  { originX: 'start', originY: 'bottom', overlayX: 'start', overlayY: 'top', offsetY: GAP_BELOW_BELL },
];

// One notification as the list shows it
interface Row {
  id: string;
  read: boolean;
  title: string;
  detail: string;
  age: string;
  icon: string;
  kind: string;
}

/**
 * The bell in the navbar, with how many notifications are unread, and the dropdown list of them.
 *
 * Opening the list marks nothing read: a member marks one read by opening it, or all with "Mark all as read".
 * Those that were unread when the list opened stay under "New" while it is open.
 */
@Component({
  selector: 'app-notification-bell',
  imports: [CdkConnectedOverlay, CdkOverlayOrigin, CdkTrapFocus, MatButton, MatIconButton, MatIcon, NgTemplateOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './notification-bell.css',
  template: `
    <button
      matIconButton
      type="button"
      class="bell"
      [class.bell--open]="open()"
      cdkOverlayOrigin
      #origin="cdkOverlayOrigin"
      #bellButton
      [attr.aria-label]="label()"
      [attr.aria-expanded]="open()"
      aria-haspopup="dialog"
      (click)="toggle()"
    >
      <mat-icon fontSet="material-symbols-outlined">notifications</mat-icon>
      @if (notifications.unread() > 0) {
        <span class="badge" aria-hidden="true">{{ badge() }}</span>
      }
    </button>

    <ng-template
      cdkConnectedOverlay
      [cdkConnectedOverlayOrigin]="origin"
      [cdkConnectedOverlayOpen]="open()"
      [cdkConnectedOverlayPositions]="positions"
      [cdkConnectedOverlayPush]="true"
      [cdkConnectedOverlayViewportMargin]="8"
      [cdkConnectedOverlayHasBackdrop]="true"
      cdkConnectedOverlayBackdropClass="cdk-overlay-transparent-backdrop"
      (backdropClick)="close()"
      (detach)="close()"
      (overlayKeydown)="onKeydown($event)"
    >
      <section class="panel" #panel role="dialog" aria-label="Notifications" cdkTrapFocus [cdkTrapFocusAutoCapture]="true">
        <header class="panel-header">
          <h2 class="panel-title" tabindex="-1" cdkFocusInitial #title>Notifications</h2>
          @if (notifications.unread() > 0) {
            <button matButton type="button" class="text-action" (click)="markAllRead()">Mark all as read</button>
          }
        </header>

        @switch (view()) {
          @case ('loading') {
            <div class="notification-list" aria-busy="true">
              <span class="sr-only">Loading notifications</span>
              @for (each of skeletons; track each) {
                <div class="row row--skeleton motion-safe:animate-pulse" aria-hidden="true">
                  <span class="row-icon bg-[#ecebe7]"></span>
                  <span class="row-text gap-2! pt-0.5">
                    <span class="block h-[17px] w-3/5 rounded-md bg-[#ecebe7]"></span>
                    <span class="block h-3.5 w-[85%] rounded-md bg-[#ecebe7]"></span>
                    <span class="block h-3.5 w-[30%] rounded-md bg-[#ecebe7]"></span>
                  </span>
                </div>
              }
            </div>
          }
          @case ('error') {
            <div class="failed" role="alert">
              <p>Couldn't load notifications.</p>
              <button matButton type="button" class="text-action" (click)="notifications.retry()">Try again</button>
            </div>
          }
          @case ('empty') {
            <div class="empty">
              <span class="empty-icon" aria-hidden="true">
                <mat-icon fontSet="material-symbols-outlined">notifications</mat-icon>
              </span>
              <p class="empty-title">You're all caught up</p>
              <p class="empty-text">
                We'll tell you here when a week's schedule is published or one of your shifts changes.
              </p>
            </div>
          }
          @default {
            <div class="notification-list" #list>
              @if (groups().fresh.length > 0) {
                <h3 class="group-label" id="notifications-new">New</h3>
                <ul class="rows" aria-labelledby="notifications-new">
                  @for (item of groups().fresh; track item.id) {
                    <ng-container *ngTemplateOutlet="row; context: { $implicit: item }" />
                  }
                </ul>
              }
              @if (groups().earlier.length > 0) {
                <h3 class="group-label" id="notifications-earlier">Earlier</h3>
                <ul class="rows" aria-labelledby="notifications-earlier">
                  @for (item of groups().earlier; track item.id) {
                    <ng-container *ngTemplateOutlet="row; context: { $implicit: item }" />
                  }
                </ul>
              }
            </div>
          }
        }
      </section>
    </ng-template>

    <ng-template #row let-item>
      <li>
        <button type="button" class="row" [class.row--unread]="!item.read" (click)="openItem(item)">
          <span class="row-icon" [attr.data-kind]="item.kind" aria-hidden="true">
            <mat-icon fontSet="material-symbols-outlined">{{ item.icon }}</mat-icon>
          </span>
          <span class="row-text">
            <span class="row-title">{{ item.title }}</span>
            <span class="row-detail">{{ item.detail }}</span>
            <span class="row-time">{{ item.age }}</span>
          </span>
          @if (!item.read) {
            <span class="dot" role="img" aria-label="Unread"></span>
          }
        </button>
      </li>
    </ng-template>
  `,
})
export class NotificationBell {
  protected readonly notifications = inject(NotificationService);
  protected readonly positions = BELOW_RIGHT;
  protected readonly skeletons = [0, 1, 2];
  protected readonly open = signal(false);

  private readonly bellButton = viewChild.required('bellButton', { read: ElementRef<HTMLElement> });
  private readonly title = viewChild('title', { read: ElementRef<HTMLElement> });
  private readonly panel = viewChild<string, ElementRef<HTMLElement>>('panel', { read: ElementRef });
  private readonly list = viewChild<string, ElementRef<HTMLElement>>('list', { read: ElementRef });

  // Those that were unread when the list opened: they stay under "New" while it is open
  private readonly newWhenOpened = signal<ReadonlySet<string>>(new Set());

  protected readonly badge = computed(() => (this.notifications.unread() > 9 ? '9+' : String(this.notifications.unread())));

  protected readonly label = computed(() => {
    const unread = this.notifications.unread();
    return unread === 0 ? 'Notifications' : `Notifications, ${unread} unread`;
  });

  protected readonly view = computed(() => {
    const empty = this.notifications.list().length === 0;
    const status = this.notifications.status();
    if (empty && status === 'loading') {
      return 'loading';
    }
    if (empty && status === 'error') {
      return 'error';
    }
    return empty ? 'empty' : 'list';
  });

  // Worked out again at every check for new notifications (every 30 seconds), so "Just now" does not stay
  protected readonly groups = computed(() => {
    const now = new Date();
    const { fresh, earlier } = groupNotifications(this.notifications.list(), this.newWhenOpened());
    const row = (each: AppNotification): Row => ({
      id: each.id,
      read: each.read,
      ...notificationCopy(each),
      age: notificationAge(each.createdAt, now),
      ...notificationLook(each.type),
    });
    return { fresh: fresh.map(row), earlier: earlier.map(row) };
  });

  // Whenever the list opens or changes: at most 4 notifications show and the rest scroll, and the panel ends above
  // the bottom of the screen wherever it starts (the phone's top bar is taller when it offers to install the app)
  private readonly fitToScreen = afterRenderEffect(() => {
    this.groups();
    const panel = this.panel()?.nativeElement;
    if (!panel) {
      return;
    }
    // Measured from the bell: the overlay may not have placed the panel yet when this runs
    const top = Math.round(this.bellButton().nativeElement.getBoundingClientRect().bottom) + GAP_BELOW_BELL;
    panel.style.maxHeight = `calc(100dvh - ${top}px - ${SCREEN_MARGIN}px)`;

    const list = this.list()?.nativeElement;
    if (!list) {
      return;
    }
    const rows = list.querySelectorAll<HTMLElement>('.rows > li');
    const last = rows[ROWS_SHOWN - 1];
    // No layout (as in unit tests) measures 0: then only the screen limits the list
    list.style.maxHeight =
      rows.length > ROWS_SHOWN && last.offsetHeight > 0
        ? `${last.offsetTop + last.offsetHeight + parseFloat(getComputedStyle(list).paddingBottom)}px`
        : '';
  });

  protected toggle(): void {
    if (this.open()) {
      this.close();
      return;
    }
    this.newWhenOpened.set(new Set(this.notifications.list().filter((each) => !each.read).map((each) => each.id)));
    this.open.set(true);
  }

  protected close(): void {
    if (!this.open()) {
      return;
    }
    this.open.set(false);
    this.bellButton().nativeElement.focus();
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.close();
    }
  }

  // Opening a notification marks it read; it has nowhere to go yet, so the list stays open
  protected openItem(item: Row): void {
    if (!item.read) {
      this.notifications.markRead(item.id);
    }
  }

  // Everything moves to "Earlier"; focus goes to the title, as the button it was on goes away
  protected markAllRead(): void {
    this.title()?.nativeElement.focus();
    this.newWhenOpened.set(new Set());
    this.notifications.markAllRead();
  }
}
