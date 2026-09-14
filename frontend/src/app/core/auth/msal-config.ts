import { IPublicClientApplication, LogLevel, PublicClientApplication } from '@azure/msal-browser';
import { environment } from '../../../environment/environment';

export function msalInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.azure.clientId,
      authority: environment.azure.authority,
      redirectUri: environment.azure.redirectUri,
    },
    cache: {
      cacheLocation: 'localStorage',
    },
    system: {
      loggerOptions: {
        loggerCallback: () => {},
        logLevel: LogLevel.Error,
        piiLoggingEnabled: false,
      },
    },
  });
}