import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { distinctUntilChanged, filter, map, Subscription, timeout } from 'rxjs';
import { environment } from '../../../../environment/environment';

export type NotificationType = 'sesion' | 'carrito' | 'perfil' | 'compra';
export interface NotificationItem {
  id: number; mensaje: string; tipo: NotificationType; leido: boolean; fecha: string;
}
@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly http = inject(HttpClient);
  private readonly msal = inject(MsalService);
  private readonly broadcast = inject(MsalBroadcastService);
  private readonly destroy = inject(DestroyRef);
  private readonly url = `${environment.apiBaseUrl}/api/notificaciones`;
  private owner: string | null = null;
  private generation = 0;
  private listRequest?: Subscription;
  private timer?: ReturnType<typeof setTimeout>;
  private readonly data = signal<NotificationItem[]>([]);
  readonly items = this.data.asReadonly();
  readonly unread = computed(() => this.data().filter(n => !n.leido).length);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly toast = signal<string | null>(null);
  readonly pending = signal<number[]>([]);

  constructor() {
    this.broadcast.inProgress$.pipe(
      filter(s => s === InteractionStatus.None),
      map(() => this.accountKey()), distinctUntilChanged(), takeUntilDestroyed(this.destroy),
    ).subscribe(key => {
      this.owner = key; this.generation++;
      this.listRequest?.unsubscribe(); this.data.set([]); this.pending.set([]);
      this.loading.set(false); this.error.set(null); this.dismiss();
      if (key) this.refresh();
    });
    this.destroy.onDestroy(() => { this.listRequest?.unsubscribe(); clearTimeout(this.timer); });
  }
  private accountKey(): string | null {
    const a = this.msal.instance.getActiveAccount() ?? this.msal.instance.getAllAccounts()[0];
    return a ? `${a.homeAccountId}:${a.localAccountId}:${a.tenantId}` : null;
  }
  dismiss(): void { clearTimeout(this.timer); this.toast.set(null); }
  private show(message: string): void {
    this.dismiss(); this.toast.set(message);
    this.timer = setTimeout(() => this.toast.set(null), 6500);
  }
  private failure(action: string, error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const status = error.status;
      if (!environment.production) console.warn('[Notificaciones]', { action, status });
      if (status === 0) return `No pudimos ${action}: no hay conexión con la API o el navegador bloqueó la respuesta.`;
      if (status === 401 || status === 403) return `No pudimos ${action}: la API rechazó el acceso (HTTP ${status}).`;
      if (status === 404) return `No pudimos ${action}: la ruta de notificaciones no está disponible (HTTP 404).`;
      return `No pudimos ${action} (HTTP ${status}). Vuelve a intentarlo.`;
    }
    return `No pudimos ${action}. La sesión o el servicio no respondió correctamente; vuelve a intentarlo.`;
  }
  refresh(): void {
    if (!this.owner) return;
    this.listRequest?.unsubscribe();
    const generation = this.generation;
    this.loading.set(true); this.error.set(null);
    this.listRequest = this.http.get<NotificationItem[]>(this.url).pipe(timeout(15000)).subscribe({
      next: items => {
        if (generation !== this.generation) return;
        this.loading.set(false);
        if (!Array.isArray(items) || items.some(item => !item || typeof item.id !== 'number' || typeof item.mensaje !== 'string' || typeof item.fecha !== 'string' || typeof item.tipo !== 'string')) {
          this.error.set('La API devolvió un historial con un formato incompatible. Verifica que el microservicio de notificaciones esté actualizado.');
          return;
        }
        this.data.set(items);
      },
      error: (error: unknown) => {
        if (generation !== this.generation) return;
        this.loading.set(false); this.error.set(this.failure('cargar tu historial', error));
      },
    });
  }
  notify(tipo: NotificationType, mensaje: string): void {
    this.show(mensaje);
    // Guest cart actions show a toast but never persist under another account.
    const key = this.accountKey();
    if (!key) return;
    this.http.post<NotificationItem>(this.url, { tipo, mensaje }).pipe(timeout(15000), takeUntilDestroyed(this.destroy)).subscribe({
      next: () => { if (key === this.accountKey()) this.refresh(); },
      error: (error: unknown) => {
        if (key !== this.accountKey()) return;
        this.error.set(this.failure('guardar la notificación', error));
        this.show(`${mensaje} No se pudo guardar el aviso en tu historial.`);
      },
    });
  }
  setRead(item: NotificationItem): void {
    if (item.leido || this.pending().includes(item.id)) return;
    const generation = this.generation;
    this.error.set(null);
    this.pending.update(ids => [...ids, item.id]);
    this.http.put<NotificationItem>(this.url, { id: item.id, leido: true })
      .pipe(timeout(15000), takeUntilDestroyed(this.destroy)).subscribe({
        next: updated => {
          if (generation !== this.generation) return;
          this.data.update(items => items.map(n => n.id === updated.id ? updated : n));
          this.pending.update(ids => ids.filter(id => id !== item.id));
        },
        error: (error: unknown) => {
          if (generation !== this.generation) return;
          this.pending.update(ids => ids.filter(id => id !== item.id));
          this.error.set(this.failure('marcar el aviso como leído', error));
        },
      });
  }
}
