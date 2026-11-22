import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export type AccessRole = 'MEMBER' | 'SUPERVISOR' | 'ADMIN';

export interface MockUser {
  id: string;
  fullName: string;
  role: AccessRole;
}

export interface Session {
  token: string;
  memberId: string;
  role: AccessRole;
  fullName: string;
}

const SESSION_KEY = 'escala.session';
// Kept in sync with App: the last loaded roster is removed on sign-out, so the next user does not see it offline.
export const LAST_PIQUETES_KEY = 'escala.lastPiquetes';

/**
 * The mocked sign-in of ADR 0006 (SCRUM-63). The session is kept in localStorage so the installed PWA opens
 * offline; the token proves nothing by itself, the API checks the role of every request.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly session = signal<Session | null>(readSession());
  readonly isSupervisor = computed(() => this.session()?.role === 'SUPERVISOR');

  users(): Observable<MockUser[]> {
    return this.http.get<MockUser[]>('/api/v1/auth/mock-users');
  }

  signIn(memberId: string): Observable<Session> {
    return this.http.post<Session>('/api/v1/auth/mock-sign-in', { memberId }).pipe(
      tap((session) => {
        writeStorage(SESSION_KEY, JSON.stringify(session));
        this.session.set(session);
      }),
    );
  }

  signOut(): void {
    removeStorage(SESSION_KEY);
    removeStorage(LAST_PIQUETES_KEY);
    this.session.set(null);
  }
}

function readSession(): Session | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as Partial<Session>;
    return typeof parsed.token === 'string' && typeof parsed.role === 'string' ? (parsed as Session) : null;
  } catch {
    return null;
  }
}

function writeStorage(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // Storage can be unavailable (private mode); the session then lasts only while the page is open.
  }
}

function removeStorage(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    // Nothing to remove when storage is unavailable.
  }
}
