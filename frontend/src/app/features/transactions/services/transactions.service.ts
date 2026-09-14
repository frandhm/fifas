import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { distinctUntilChanged, filter, finalize, map, Subscription, timeout } from 'rxjs';
import { environment } from '../../../../environment/environment';

export interface TransactionItem { name: string; quantity: number; size: string; customName?: string; customDorsal?: number; price: number; }
export interface Transaction { id: string; date: string; items: TransactionItem[]; total: number; status: 'Entregado'|'En camino'|'Procesando'|'Cancelado'; paymentMethod: string; }
export interface NewRequest { productoId: string; productoNombre: string; cantidad: number; precioUnitario: number; talla?: string; nombreEstampado?: string; numeroEstampado?: number; }
interface RequestDto extends NewRequest { id: number; estado: string; fechaCreacion: string; }

@Injectable({ providedIn: 'root' })
export class TransactionsService {
  private readonly http = inject(HttpClient);
  private readonly msal = inject(MsalService);
  private readonly broadcast = inject(MsalBroadcastService);
  private readonly destroy = inject(DestroyRef);
  private readonly url = `${environment.apiBaseUrl}/api/solicitudes`;
  private owner: string | null = null;
  readonly authenticated = signal(false);
  private request?: Subscription;
  private readonly transactions = signal<Transaction[]>([]);
  readonly userTransactions = this.transactions.asReadonly();
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    this.broadcast.inProgress$.pipe(filter(s => s === InteractionStatus.None),
      map(() => this.accountKey()), distinctUntilChanged(), takeUntilDestroyed(this.destroy))
      .subscribe(owner => { this.request?.unsubscribe(); this.owner = owner; this.authenticated.set(!!owner); this.transactions.set([]); if (owner) this.refresh(); });
    this.destroy.onDestroy(() => this.request?.unsubscribe());
  }

  refresh(): void {
    if (!this.owner || this.loading()) return;
    this.loading.set(true); this.error.set(null);
    this.request = this.http.get<RequestDto[]>(this.url).pipe(timeout(15000), finalize(() => this.loading.set(false))).subscribe({
      next: data => this.transactions.set(data.map(item => this.toTransaction(item))),
      error: error => this.error.set(this.failure('cargar las solicitudes', error)),
    });
  }

  create(items: NewRequest[], onSuccess: () => void): void {
    if (!this.owner) { this.error.set('Inicia sesión para registrar la solicitud.'); return; }
    if (!items.length || this.saving()) return;
    this.saving.set(true); this.error.set(null);
    this.http.post<RequestDto[]>(this.url, { items }).pipe(timeout(15000))
      .pipe(finalize(() => this.saving.set(false))).subscribe({
        next: created => { this.transactions.update(current => [...created.map(item => this.toTransaction(item)), ...current]); onSuccess(); },
        error: error => this.error.set(this.failure('crear la solicitud', error)),
      });
  }

  private toTransaction(item: RequestDto): Transaction {
    const status: Transaction['status'] = item.estado === 'LISTO' ? 'Entregado' :
      item.estado === 'EN_PROCESO' ? 'En camino' : item.estado === 'CANCELADO' ? 'Cancelado' : 'Procesando';
    return { id: `FIF-${item.id}`, date: new Date(item.fechaCreacion).toLocaleDateString('es-CL'),
      items: [{ name: item.productoNombre, quantity: item.cantidad, size: item.talla || '—',
        customName: item.nombreEstampado || undefined, customDorsal: item.numeroEstampado,
        price: Number(item.precioUnitario) }], total: Number(item.precioUnitario) * item.cantidad,
      status, paymentMethod: 'Solicitud en línea' };
  }
  private accountKey(): string | null { const a = this.msal.instance.getActiveAccount() ?? this.msal.instance.getAllAccounts()[0]; return a ? `${a.homeAccountId}:${a.localAccountId}:${a.tenantId}` : null; }
  private failure(action: string, error: unknown): string { if (error instanceof HttpErrorResponse) {
    if (error.status === 400) return 'La solicitud contiene datos inválidos. Revisa el carrito.';
    if (error.status === 401 || error.status === 403) return 'Tu sesión no tiene acceso a las solicitudes.';
    if (error.status === 0) return 'No hay conexión con el servicio de solicitudes.';
  } return `No pudimos ${action}. Vuelve a intentarlo.`; }
}
