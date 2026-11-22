import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { Member, MemberService } from './member.service';

/** The members with their functions, licences and certifications; registering and editing for the admin only. */
@Component({
  selector: 'app-members-list',
  imports: [RouterLink],
  templateUrl: './members-list.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class MembersList implements OnInit {
  private readonly members = inject(MemberService);
  private readonly auth = inject(AuthService);

  protected readonly list = signal<Member[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly isAdmin = computed(() => this.auth.session()?.role === 'ADMIN');

  ngOnInit(): void {
    this.members.list().subscribe({
      next: (list) => this.list.set(list),
      error: () => this.error.set('Could not load the members.'),
    });
  }
}
