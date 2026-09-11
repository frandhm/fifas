import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { filter, map, take } from 'rxjs';

export const authGuard: CanActivateFn = (_route, state) => {
  const msal = inject(MsalService);
  const broadcast = inject(MsalBroadcastService);
  return broadcast.inProgress$.pipe(
    filter((status) => status === InteractionStatus.None),
    take(1),
    map(() => {
      const account = msal.instance.getActiveAccount() ?? msal.instance.getAllAccounts()[0];
      if (account) {
        msal.instance.setActiveAccount(account);
        return true;
      }
      msal.loginRedirect({ scopes: ['openid', 'profile'], redirectStartPage: state.url });
      return false;
    }),
  );
};
