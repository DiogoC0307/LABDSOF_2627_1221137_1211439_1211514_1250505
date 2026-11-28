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

/** Whether a request is the Schedule page's: GET /api/v1/piquetes?week= (SCRUM-65). */
export function isWeekRequest(req: { url: string; params: { has(name: string): boolean } }): boolean {
  return req.url === '/api/v1/piquetes' && req.params.has('week');
}

/** The one request the Schedule page makes for its week. */
export function weekRequest() {
  return http().expectOne(isWeekRequest);
}

/**
 * Answers, with an empty week, any request the Schedule page made for its week. Specs that open the Schedule page
 * for something else call it before http().verify(); week-schedule.spec.ts tests the week view itself.
 */
export function flushWeekRequests(): void {
  http()
    .match(isWeekRequest)
    .forEach((request) => request.flush([]));
}

/**
 * Answers, with no notifications, any request the Schedule page made for its notifications
 */
export function flushNotificationRequests(): void {
  http()
    .match('/api/v1/notifications')
    .forEach((request) => request.flush([]));
}

export const pending = {
  id: 'p1',
  dutyDate: '2026-11-05',
  slots: [{ id: 's1', role: 'CREW_MEMBER', status: 'PENDING' }],
};

/** Types an input or select value the way a user does, so ngModel sees it. */
export function setValue(element: HTMLInputElement | HTMLSelectElement, value: string): void {
  element.value = value;
  element.dispatchEvent(new Event(element instanceof HTMLSelectElement ? 'change' : 'input'));
}
