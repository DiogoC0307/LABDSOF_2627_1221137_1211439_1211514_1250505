import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AccessRole, AuthService } from './auth.service';

/**
 * Lets a route open only for the given roles, otherwise back to the schedule. Convenience only: the API checks the
 * role of every request and answers 403 anyway (SCRUM-77).
 */
export function roleGuard(...roles: AccessRole[]): CanActivateFn {
  return () => {
    const role = inject(AuthService).session()?.role;
    return role !== undefined && roles.includes(role) ? true : inject(Router).parseUrl('/');
  };
}
