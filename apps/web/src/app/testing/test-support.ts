import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { routes } from '../app.routes';
import { authInterceptor } from '../auth/auth.interceptor';

/** Test helpers shared by the specs (synthetic data only). */

export const SESSION_KEY = 'escala.session';

export const users = [
  { id: '00000000-0000-4000-8000-000000000001', fullName: 'Synthetic Member', role: 'MEMBER' },
  { id: '00000000-0000-4000-8000-000000000002', fullName: 'Synthetic Supervisor', role: 'SUPERVISOR' },
  { id: '00000000-0000-4000-8000-000000000003', fullName: 'Synthetic Admin', role: 'ADMIN' },
];

export function signInAs(role: string): void {
  const user = users.find((candidate) => candidate.role === role)!;
  localStorage.setItem(
    SESSION_KEY,
    JSON.stringify({ token: `mock.${role}`, memberId: user.id, role, fullName: user.fullName }),
  );
}

export const testProviders = [
  provideHttpClient(withInterceptors([authInterceptor])),
  provideHttpClientTesting(),
  provideRouter(routes),
];

export function http(): HttpTestingController {
  return TestBed.inject(HttpTestingController);
}

/** No cached RCM yet. Flushed after a supervisor's roster load (SCRUM-76). */
export const emptyRisk = {
  dico: '1312',
  rcm: null,
  fetchedAt: null,
  dataAgeHours: null,
  source: 'IPMA',
  assumedHigher: false,
};

/** The schedule asks for today's risk only after the roster loads, and only for a supervisor. */
export function flushRosterRisk(): void {
  const raw = localStorage.getItem(SESSION_KEY);
  if (raw?.includes('"role":"SUPERVISOR"')) {
    http().expectOne('/api/v1/risk/today').flush(emptyRisk);
  }
}

export function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

export function todaysPiquete(status = 'ELIGIBLE') {
  return {
    id: 'today',
    dutyDate: todayIso(),
    slots: [{ id: 's-today', role: 'CREW_MEMBER', status }],
  };
}

export const pending = {
  id: 'p1',
  dutyDate: '2026-11-05',
  slots: [{ id: 's1', role: 'CREW_MEMBER', status: 'PENDING' }],
};

export const eligible = {
  ...pending,
  slots: [{ id: 's1', role: 'CREW_MEMBER', status: 'ELIGIBLE' }],
};

/** Types an input or select value the way a user does, so ngModel sees it. */
export function setValue(element: HTMLInputElement | HTMLSelectElement, value: string): void {
  element.value = value;
  element.dispatchEvent(new Event(element instanceof HTMLSelectElement ? 'change' : 'input'));
}
