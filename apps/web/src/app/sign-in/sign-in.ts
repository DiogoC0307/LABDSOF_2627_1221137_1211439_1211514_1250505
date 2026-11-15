import { ChangeDetectionStrategy, Component, OnInit, inject, output, signal } from '@angular/core';
import { AuthService, MockUser } from '../auth/auth.service';

/** Pick one of the seeded synthetic users (mocked sign-in, ADR 0006). */
@Component({
  selector: 'app-sign-in',
  templateUrl: './sign-in.html',
  styleUrl: './sign-in.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class SignIn implements OnInit {
  private readonly auth = inject(AuthService);

  readonly signedIn = output<void>();

  protected readonly users = signal<MockUser[]>([]);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.auth.users().subscribe({
      next: (users) => this.users.set(users),
      error: () => this.error.set('Could not load the users.'),
    });
  }

  protected signIn(user: MockUser): void {
    this.error.set(null);
    this.auth.signIn(user.id).subscribe({
      next: () => this.signedIn.emit(),
      error: () => this.error.set('Could not sign in.'),
    });
  }
}
