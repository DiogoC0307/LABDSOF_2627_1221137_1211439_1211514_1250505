import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AccessRole, AuthService } from '../auth/auth.service';
import { NotificationBell } from '../notifications/notification-bell';

interface NavLink {
  path: string;
  label: string;
  icon: string;
  roles?: readonly AccessRole[];
}

const LINKS: readonly NavLink[] = [
  { path: '/', label: 'Schedule', icon: 'calendar_month' },
  { path: '/unavailability', label: "When I can't", icon: 'event_busy' },
  { path: '/members', label: 'Members', icon: 'group', roles: ['SUPERVISOR', 'ADMIN'] },
];

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, MatButton, MatIcon, NotificationBell],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  changeDetection: ChangeDetectionStrategy.Eager,
  host: { class: 'sticky top-0 z-10 block text-ink' },
})
export class Navbar {
  protected readonly auth = inject(AuthService);

  readonly canInstall = input(false); /** True once the browser offers to install the PWA. */
  readonly install = output<void>();
  readonly signOut = output<void>();

  protected readonly links = computed(() => {
    const role = this.auth.session()?.role;
    return LINKS.filter((link) => !link.roles || (role !== undefined && link.roles.includes(role)));
  });
}
