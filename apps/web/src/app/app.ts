import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './auth/auth.service';
import { SignIn } from './sign-in/sign-in';

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
}

/**
 * The shell: the sign-in screen without a session; with one, the menu by role and the router outlet (SCRUM-63,
 * SCRUM-77). The menu and the route guards are convenience only: the API checks the role of every request.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, SignIn],
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class App implements OnInit, OnDestroy {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private deferredInstall: BeforeInstallPromptEvent | null = null;
  private readonly onBeforeInstall = (event: Event) => this.captureInstallPrompt(event);

  protected readonly canInstall = signal(false);
  protected readonly seesMembers = computed(() => {
    const role = this.auth.session()?.role;
    return role === 'SUPERVISOR' || role === 'ADMIN';
  });

  ngOnInit(): void {
    window.addEventListener('beforeinstallprompt', this.onBeforeInstall);
  }

  ngOnDestroy(): void {
    window.removeEventListener('beforeinstallprompt', this.onBeforeInstall);
  }

  protected onSignedIn(): void {
    void this.router.navigateByUrl('/');
  }

  protected signOut(): void {
    this.auth.signOut();
    void this.router.navigateByUrl('/');
  }

  protected async install(): Promise<void> {
    if (!this.deferredInstall) {
      return;
    }
    await this.deferredInstall.prompt();
    this.deferredInstall = null;
    this.canInstall.set(false);
  }

  private captureInstallPrompt(event: Event): void {
    event.preventDefault();
    this.deferredInstall = event as BeforeInstallPromptEvent;
    this.canInstall.set(true);
  }
}
