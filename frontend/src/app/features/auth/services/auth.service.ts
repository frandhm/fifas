import { Injectable, computed, signal } from '@angular/core';

const STORAGE_KEY = 'fifas.auth.user';

export interface AuthUser {
  email: string;
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

    const next: AuthUser = { email: email.trim() };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    this.user.set(next);
    return true;
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.user.set(null);
  }

  private readStoredUser(): AuthUser | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as AuthUser) : null;
    } catch {
      return null;
    }
  }
}
