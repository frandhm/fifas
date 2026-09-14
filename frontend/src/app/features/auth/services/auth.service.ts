import { Injectable, computed, inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { ProfileService } from '../../profile/services/profile.service';

export interface AuthUser {
  email: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly msal = inject(MsalService);
  private readonly profiles = inject(ProfileService);
  readonly currentUser = computed<AuthUser | null>(() => {
    const profile = this.profiles.profile();
    return profile ? { email: profile.correo, firstName: profile.nombre,
      lastName: profile.apellido, phone: profile.telefono } : null;
  });

  logout(): void {
    this.profiles.reset();
    localStorage.removeItem('fifas.auth.user');
    const account = this.msal.instance.getActiveAccount() ?? this.msal.instance.getAllAccounts()[0];
    void this.msal.logoutRedirect({ account, postLogoutRedirectUri: window.location.origin });
  }
}
