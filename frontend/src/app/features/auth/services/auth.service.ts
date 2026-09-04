import { Injectable, computed, signal } from '@angular/core';

const STORAGE_KEY = 'fifas.auth.user';

export interface AuthUser {
  email: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly user = signal<AuthUser | null>(this.readStoredUser());

  readonly currentUser = this.user.asReadonly();
  readonly isLoggedIn = computed(() => this.user() !== null);

  login(email: string, password: string): boolean {
    if (!email.trim() || password.length < 4) {
      return false;
    }

    const stored = this.readStoredUser();
    const next: AuthUser = {
      email: email.trim(),
      firstName: stored?.firstName || 'Francisco',
      lastName: stored?.lastName || 'García',
      phone: stored?.phone || '+56 9 1234 5678',
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    this.user.set(next);
    return true;
  }

  updateProfile(data: { firstName: string; lastName: string; email: string; phone: string }): void {
    const next: AuthUser = {
      email: data.email.trim(),
      firstName: data.firstName.trim(),
      lastName: data.lastName.trim(),
      phone: data.phone.trim(),
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    this.user.set(next);
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.user.set(null);
  }

  private readStoredUser(): AuthUser | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const parsed = JSON.parse(raw) as AuthUser;
      return {
        email: parsed.email || 'usuario@fifas.com',
        firstName: parsed.firstName || 'Francisco',
        lastName: parsed.lastName || 'García',
        phone: parsed.phone || '+56 9 1234 5678',
      };
    } catch {
      return null;
    }
  }
}
