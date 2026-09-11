import { firstValueFrom } from 'rxjs';
import { APP_INITIALIZER, ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  MSAL_GUARD_CONFIG,
  MSAL_INSTANCE,
  MsalGuard,
  MsalService,
  MsalBroadcastService,
} from '@azure/msal-angular';
import { InteractionType } from '@azure/msal-browser';

import { routes } from './app.routes';
import { msalInstanceFactory } from './core/auth/msal-config';
import { apiAuthInterceptor } from './core/auth/api-auth.interceptor';

function initializeMsal(msalService: MsalService): () => Promise<void> {
  return () => firstValueFrom(msalService.initialize());
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([apiAuthInterceptor])),

    { provide: MSAL_INSTANCE, useFactory: msalInstanceFactory },
    { provide: MSAL_GUARD_CONFIG, useValue: { interactionType: InteractionType.Redirect } },
    { provide: APP_INITIALIZER, useFactory: initializeMsal, deps: [MsalService], multi: true },

    MsalGuard,
    MsalService,
    MsalBroadcastService,
  ]
};