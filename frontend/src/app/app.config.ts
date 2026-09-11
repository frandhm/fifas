import { firstValueFrom } from 'rxjs';
import { APP_INITIALIZER, ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import {
  MSAL_GUARD_CONFIG,
  MSAL_INSTANCE,
  MSAL_INTERCEPTOR_CONFIG,
  MsalGuard,
  MsalInterceptor,
  MsalService,
  MsalBroadcastService,
} from '@azure/msal-angular';
import { InteractionType } from '@azure/msal-browser';

import { routes } from './app.routes';
import { msalInstanceFactory } from './core/auth/msal-config';
import { msalInterceptorConfigFactory } from './core/auth/msal-interceptor-config';

function initializeMsal(msalService: MsalService): () => Promise<void> {
  return () => firstValueFrom(msalService.initialize());
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),

    { provide: MSAL_INSTANCE, useFactory: msalInstanceFactory },
    { provide: MSAL_INTERCEPTOR_CONFIG, useFactory: msalInterceptorConfigFactory },
    { provide: MSAL_GUARD_CONFIG, useValue: { interactionType: InteractionType.Redirect } },
    { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },
    { provide: APP_INITIALIZER, useFactory: initializeMsal, deps: [MsalService], multi: true },

    MsalGuard,
    MsalService,
    MsalBroadcastService,
  ]
};