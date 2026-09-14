import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { distinctUntilChanged, filter, finalize, forkJoin, map, Observable, Subscription, timeout } from 'rxjs';
import { environment } from '../../../../environment/environment';
import { NotificationsService } from '../../notifications/services/notifications.service';

const STORAGE_KEY = 'fifas.cart';
export interface CartLine { id?: number; productId: string; quantity: number; size?: string; playerName?: string; dorsal?: number; }
interface ApiCartLine { id: number; productoId: string; cantidad: number; talla?: string; nombreJugador?: string; dorsal?: number; }

function matches(a: CartLine, b: CartLine): boolean {
  return a.productId === b.productId && a.size === b.size &&
    a.playerName === b.playerName && a.dorsal === b.dorsal;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly msal = inject(MsalService);
  private readonly broadcast = inject(MsalBroadcastService);
  private readonly destroy = inject(DestroyRef);
  private readonly notifications = inject(NotificationsService);
  private readonly url = `${environment.apiBaseUrl}/api/carritos`;
  private readonly lines = signal<CartLine[]>(this.readStored());
  private owner: string | null = null;
  private request?: Subscription;
  private revision = 0;
  readonly items = this.lines.asReadonly();
  readonly count = computed(() => this.lines().reduce((sum, line) => sum + line.quantity, 0));
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    this.broadcast.inProgress$.pipe(filter(s => s === InteractionStatus.None),
      map(() => this.accountKey()), distinctUntilChanged(), takeUntilDestroyed(this.destroy))
      .subscribe(owner => {
        this.request?.unsubscribe(); this.owner = owner; this.error.set(null);
        if (owner) this.loadAndMerge(); else this.lines.set(this.readStored());
      });
    this.destroy.onDestroy(() => this.request?.unsubscribe());
  }

  add(productId: string, size?: string, playerName?: string, dorsal?: number): void {
    const candidate: CartLine = { productId, quantity: 1, size, playerName, dorsal };
    const existing = this.lines().find(line => matches(line, candidate));
    const updated = existing ? { ...existing, quantity: existing.quantity + 1 } : candidate;
    this.revision++;
    this.lines.update(items => existing ? items.map(line => matches(line, candidate) ? updated : line) : [...items, updated]);
    this.afterChange(updated);
    this.notifications.notify('carrito', playerName ? 'Agregaste una camiseta personalizada al carrito.' : 'Agregaste un producto al carrito.');
  }

  updateQuantity(productId: string, quantity: number, size?: string, playerName?: string, dorsal?: number): void {
    const current = this.lines().find(line => matches(line, { productId, quantity, size, playerName, dorsal }));
    if (!current || current.quantity === quantity) return;
    if (quantity <= 0) { this.remove(productId, size, playerName, dorsal); return; }
    const updated = { ...current, quantity };
    this.revision++;
    this.lines.update(items => items.map(line => matches(line, current) ? updated : line));
    this.afterChange(updated);
    this.notifications.notify('carrito', 'Actualizaste la cantidad de un producto en el carrito.');
  }

  remove(productId: string, size?: string, playerName?: string, dorsal?: number): void {
    const current = this.lines().find(line => matches(line, { productId, quantity: 1, size, playerName, dorsal }));
    if (!current) return;
    this.revision++;
    this.lines.update(items => items.filter(line => !matches(line, current)));
    if (!this.owner) this.persist();
    else if (current.id) this.mutate(this.http.delete<void>(`${this.url}/${current.id}`), undefined);
    else this.refresh();
    this.notifications.notify('carrito', 'Quitaste un producto del carrito.');
  }

  clear(): void {
    if (!this.lines().length) return;
    this.revision++;
    this.lines.set([]);
    if (!this.owner) this.persist(); else this.mutate(this.http.delete<void>(this.url), undefined);
    this.notifications.notify('carrito', 'Vaciaste tu carrito.');
  }

  refresh(): void { if (this.owner) this.loadAndMerge(false); }

  private loadAndMerge(mergeGuest = true): void {
    const guest = mergeGuest ? this.readStored() : [];
    const revisionAtStart = this.revision;
    this.loading.set(true); this.error.set(null);
    this.request = this.http.get<ApiCartLine[]>(this.url).pipe(timeout(15000)).subscribe({
      next: data => {
        const remote = data.map(item => this.fromApi(item));
        // Never let a slower GET erase an item the customer just added.
        if (revisionAtStart !== this.revision) { this.loading.set(false); return; }
        this.lines.set(remote);
        if (!guest.length) { this.loading.set(false); return; }
        const merged = guest.map(item => {
          const found = remote.find(server => matches(server, item));
          return found ? { ...found, quantity: found.quantity + item.quantity } : item;
        });
        const untouched = remote.filter(server => !guest.some(item => matches(server, item)));
        const desired = [...untouched, ...merged];
        forkJoin(desired.map(line => this.http.post<ApiCartLine>(this.url, this.toApi(line)))).subscribe({
          next: saved => { localStorage.removeItem(STORAGE_KEY); this.lines.set(saved.map(item => this.fromApi(item))); this.loading.set(false); },
          error: error => { this.lines.set(desired); this.loading.set(false); this.error.set(this.failure(error)); },
        });
      },
      error: error => { this.loading.set(false); this.error.set(this.failure(error)); },
    });
  }

  private afterChange(line: CartLine): void {
    if (!this.owner) { this.persist(); return; }
    this.mutate(this.http.post<ApiCartLine>(this.url, this.toApi(line)), line);
  }

  private mutate(observable: Observable<unknown>, line?: CartLine): void {
    this.saving.set(true); this.error.set(null);
    observable.pipe(timeout(15000), finalize(() => this.saving.set(false))).subscribe({
      next: value => {
        if (line && value && typeof value === 'object') {
          const saved = this.fromApi(value as ApiCartLine);
          this.lines.update(items => items.map(item => matches(item, line) ? saved : item));
        }
      },
      // Keep the optimistic line visible. A refresh here used to replace it with
      // the older server cart, making an add appear to do nothing.
      error: error => { this.error.set(this.failure(error)); },
    });
  }

  private toApi(line: CartLine) { return { productoId: line.productId, cantidad: line.quantity,
    talla: line.size, nombreJugador: line.playerName, dorsal: line.dorsal }; }
  private fromApi(item: ApiCartLine): CartLine { return { id: item.id, productId: String(item.productoId),
    quantity: item.cantidad, size: item.talla || undefined, playerName: item.nombreJugador || undefined, dorsal: item.dorsal ?? undefined }; }
  private accountKey(): string | null { const a = this.msal.instance.getActiveAccount() ?? this.msal.instance.getAllAccounts()[0]; return a ? `${a.homeAccountId}:${a.localAccountId}:${a.tenantId}` : null; }
  private persist(): void { localStorage.setItem(STORAGE_KEY, JSON.stringify(this.lines())); }
  private readStored(): CartLine[] { try { const raw = localStorage.getItem(STORAGE_KEY); return raw ? JSON.parse(raw) : []; } catch { return []; } }
  private failure(error: unknown): string { if (error instanceof HttpErrorResponse) {
    if (error.status === 401 || error.status === 403) return 'Tu sesión no tiene acceso al carrito. Vuelve a iniciar sesión.';
    if (error.status === 400) return 'El servicio desplegado rechazó el formato del carrito. Actualiza carritoService y vuelve a intentar.';
    if (error.status === 0) return 'No se pudo conectar con el servicio de carrito.';
  } return 'No pudimos sincronizar el carrito. El producto se mantiene visible para que puedas volver a intentar.'; }
}
