import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { filter, switchMap, take, throwError } from 'rxjs';
import { environment } from '../../../environment/environment';

// Background API requests must never initiate an interactive login.
export const apiAuthInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(`${environment.apiBaseUrl}/`)) return next(request);

  const msal = inject(MsalService);
  const broadcast = inject(MsalBroadcastService);
  const scopes = environment.azure.protectedResourceScopes;
  if (!scopes.length || scopes.some((scope) => scope.includes('PON_AQUI'))) {
    return throwError(() => new HttpErrorResponse({
      status: 503, statusText: 'API permission not configured', url: request.url,
    }));
  }

  return broadcast.inProgress$.pipe(
    filter((status) => status === InteractionStatus.None),
    take(1),
    switchMap(() => {
      const account = msal.instance.getActiveAccount() ?? msal.instance.getAllAccounts()[0];
      if (!account) {
        return throwError(() => new HttpErrorResponse({
          status: 401, statusText: 'Sign in required', url: request.url,
        }));
      }
      return msal.acquireTokenSilent({ scopes, account }).pipe(
        switchMap((result) => next(request.clone({
          setHeaders: { Authorization: `Bearer ${result.accessToken}` },
        }))),
      );
    }),
  );
};
