import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { distinctUntilChanged, filter, map, Subscription, timeout } from 'rxjs';
import { environment } from '../../../../environment/environment';

export interface Profile {
  id: number | null;
  nombre: string;
  apellido: string;
  correo: string;
  telefono: string;
  direccion: string;
  roles: string[];
}
export type ProfileInput = Omit<Profile, 'id' | 'roles'>;

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly msal = inject(MsalService);
  private readonly broadcast = inject(MsalBroadcastService);
  private readonly destroy = inject(DestroyRef);
  private readonly url = `${environment.apiBaseUrl}/api/usuarios/me`;
  private owner: string | null = null;
  private generation = 0;
  private request?: Subscription;
  readonly profile = signal<Profile | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly saved = signal(false);

  constructor() {
    this.broadcast.inProgress$.pipe(
      filter(status => status === InteractionStatus.None),
      map(() => this.accountKey()), distinctUntilChanged(), takeUntilDestroyed(this.destroy),
    ).subscribe(owner => {
      this.reset();
      this.owner = owner;
      if (owner) this.refresh();
    });
    this.destroy.onDestroy(() => this.request?.unsubscribe());
  }

  private accountKey(): string | null {
    const account = this.msal.instance.getActiveAccount() ?? this.msal.instance.getAllAccounts()[0];
    return account ? `${account.homeAccountId}:${account.localAccountId}:${account.tenantId}` : null;
  }

  reset(): void {
    this.generation++;
    this.request?.unsubscribe();
    this.owner = null;
    this.profile.set(null);
    this.loading.set(false);
    this.saving.set(false);
    this.error.set(null);
    this.saved.set(false);
  }

  refresh(): void {
    if (!this.owner || this.saving()) return;
    this.request?.unsubscribe();
    const generation = this.generation;
    this.loading.set(true);
    this.error.set(null);
    this.saved.set(false);
    this.request = this.http.get<Profile>(this.url).pipe(timeout(15000)).subscribe({
      next: profile => {
        if (generation !== this.generation) return;
        this.profile.set(profile);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        if (generation !== this.generation) return;
        this.loading.set(false);
        if (error instanceof HttpErrorResponse && error.status === 404 && error.error?.code === 'PROFILE_NOT_FOUND') {
          // Un perfil nuevo usa solo datos reales de la cuenta; se persiste al guardar.
          const account = this.msal.instance.getActiveAccount() ?? this.msal.instance.getAllAccounts()[0];
          const claims = account?.idTokenClaims;
          this.profile.set({ id: null, nombre: String(claims?.['given_name'] ?? ''),
            apellido: String(claims?.['family_name'] ?? ''),
            correo: String(claims?.['email'] ?? account?.username ?? ''), telefono: '', direccion: '',
            roles: this.rolesFrom(error.error, claims) });
        } else {
          this.error.set(this.failure('cargar tu perfil', error));
        }
      },
    });
  }

  save(data: ProfileInput, onSaved: () => void): void {
    if (!this.owner || !this.profile() || this.loading() || this.saving()) return;
    const generation = this.generation;
    this.saving.set(true);
    this.saved.set(false);
    this.error.set(null);
    this.request = this.http.put<Profile>(this.url, data).pipe(timeout(15000)).subscribe({
      next: profile => {
        if (generation !== this.generation) return;
        this.profile.set(profile);
        this.saving.set(false);
        this.saved.set(true);
        onSaved();
      },
      error: (error: unknown) => {
        if (generation !== this.generation) return;
        this.saving.set(false);
        this.error.set(this.failure('guardar tu perfil', error));
      },
    });
  }

  private failure(action: string, error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401 || error.status === 403) return 'Tu sesión no tiene acceso al perfil. Vuelve a iniciar sesión.';
      if (error.status === 400) return 'Revisa los campos del perfil y vuelve a guardar.';
      if (error.status === 409) return 'El perfil cambió durante el guardado. Recarga los datos e intenta nuevamente.';
    }
    return `No pudimos ${action}. Revisa tu conexión y vuelve a intentarlo.`;
  }

  private rolesFrom(response: unknown, claims: Record<string, unknown> | undefined): string[] {
    const apiRoles = response && typeof response === 'object' && Array.isArray((response as { roles?: unknown }).roles)
      ? (response as { roles: unknown[] }).roles : [];
    const claimRoles = Array.isArray(claims?.['roles']) ? claims['roles'] : [];
    return [...apiRoles, ...claimRoles].filter((role): role is string => typeof role === 'string' && role.length > 0)
      .filter((role, index, all) => all.indexOf(role) === index);
  }
}
