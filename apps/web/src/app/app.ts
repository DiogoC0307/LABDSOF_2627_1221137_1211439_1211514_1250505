import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { Router, RouterOutlet } from '@angular/router';
import { AuthService } from './auth/auth.service';
import { Navbar } from './navbar/navbar';
import { SignIn } from './sign-in/sign-in';
import { AppUpdate } from './update/app-update';

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
}

/**
 * The shell: the sign-in screen without a session; with one, the navbar at the top and the router outlet (SCRUM-63,
 * SCRUM-77). The navbar's links and the route guards are convenience only: the API checks the role of every request.
 * When a new version is deployed, it offers to reload, signed in or not (SCRUM-166).
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MatButton, Navbar, SignIn],
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class App implements OnInit, OnDestroy {
  protected readonly auth = inject(AuthService);
  protected readonly update = inject(AppUpdate);
  private readonly router = inject(Router);
  private deferredInstall: BeforeInstallPromptEvent | null = null;
  private readonly onBeforeInstall = (event: Event) => this.captureInstallPrompt(event);

  protected readonly canInstall = signal(false);

  ngOnInit(): void {
    window.addEventListener('beforeinstallprompt', this.onBeforeInstall);
    this.update.start();
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
