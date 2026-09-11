import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

export const authGuard: CanActivateFn = () => {
  const msal = inject(MsalService);
  const isLoggedIn = msal.instance.getAllAccounts().length > 0;

  if (!isLoggedIn) {
    msal.loginRedirect({ scopes: ['User.Read'], prompt: 'select_account' });
    return false;
  }

  return true;
};