import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Sends the mocked token with every API call except the sign-in ones, and signs out when the API answers 401
 * (unknown or inactive member, or a token from another role), so the sign-in screen is shown again (SCRUM-63).
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const session = auth.session();
  const isSignInCall = request.url.startsWith('/api/v1/auth/');
  const authorized =
    session && !isSignInCall ? request.clone({ setHeaders: { Authorization: `Bearer ${session.token}` } }) : request;
  return next(authorized).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isSignInCall) {
        auth.signOut();
      }
      return throwError(() => error);
    }),
  );
};
