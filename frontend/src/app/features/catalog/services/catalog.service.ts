import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, distinctUntilChanged, filter, finalize, map, of, Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { ADULT_SIZES, KIDS_SIZES, Product } from '../../../core/models/product';
import { environment } from '../../../../environment/environment';

const PRODUCTS: Product[] = [
  {
    id: 'local-2026',
    name: 'Camiseta local Los FIFAS 2026',
    price: 200,
    accent: '#1e4fa3',
    stripe: '#f4f7fb',
    category: 'local',
    description: 'Rayas azul y blanco. La camiseta de casa para la temporada 2026.',
    sizes: [...ADULT_SIZES],
  },
  {
    id: 'visitante-2026',
    name: 'Camiseta visitante 2026',
    price: 200,
    accent: '#0b1730',
    stripe: '#d4af37',
    category: 'visitante',
    description: 'Navy con detalles dorados para los partidos de visitante.',
    sizes: [...ADULT_SIZES],
  },
  {
    id: 'tercera-roja',
    name: 'Tercera equipación roja',
    price: 190,
    accent: '#8b1e2d',
    stripe: '#f1d5c0',
    category: 'tercera',
    description: 'Tercera equipación en rojo, corte clásico de competición.',
    sizes: [...ADULT_SIZES],
  },
  {
    id: 'portero',
    name: 'Camiseta de portero',
    price: 180,
    accent: '#15803d',
    stripe: '#0b1730',
    category: 'portero',
    description: 'Verde y navy, pensada para el arco.',
    sizes: [...ADULT_SIZES],
  },
  {
    id: 'retro',
    name: 'Edición retro 98',
    price: 220,
    accent: '#163a7a',
    stripe: '#e8c36a',
    category: 'retro',
    description: 'Homenaje a la camiseta del 98, edición limitada.',
    sizes: [...ADULT_SIZES],
  },
  {
    id: 'local-nino',
    name: 'Camiseta local niño',
    price: 140,
    accent: '#1e4fa3',
    stripe: '#f4f7fb',
    category: 'local',
    description: 'Misma local 2026, talla infantil.',
    sizes: [...KIDS_SIZES],
  },
];

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly productsSignal = signal<Product[]>(PRODUCTS);

  private readonly msal = inject(MsalService);
  private readonly broadcast = inject(MsalBroadcastService);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  readonly loading = signal(false);

  readonly loadError = signal<string | null>(null);

  constructor() {
    this.destroyRef.onDestroy(() => this.request?.unsubscribe());
    this.broadcast.inProgress$.pipe(
      filter((status) => status === InteractionStatus.None),
      map(() => {
        const account = this.msal.instance.getActiveAccount() ?? this.msal.instance.getAllAccounts()[0];
        return account ? `${account.homeAccountId}:${account.localAccountId}:${account.tenantId}` : null;
      }),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((account) => {
      this.request?.unsubscribe();
      if (account) {
        this.loadFromBackend();
      } else {
        this.productsSignal.set(PRODUCTS);
        this.loadError.set('Inicia sesión con Microsoft desde Perfil para cargar el catálogo en línea. Mostramos productos de demostración.');
      }
    });
  }

  loadFromBackend(): void {
    this.request?.unsubscribe();
    this.loadError.set(null);
    this.loading.set(true);
    this.request = this.http.get<Product[]>(`${environment.apiBaseUrl}/api/productos`)
      .pipe(
        catchError((error: unknown) => {
          let reason = 'No pudimos cargar el catálogo en línea.';
          if (error instanceof HttpErrorResponse) {
            if (error.statusText === 'Sign in required') {
              reason = 'Inicia sesión con Microsoft desde Perfil para cargar el catálogo en línea.';
            } else if (error.statusText === 'API permission not configured') {
              reason = 'Falta configurar el permiso de acceso al catálogo.';
            } else if (error.status === 0) {
              reason = 'No se pudo conectar con el catálogo en línea. Revisa la conexión y vuelve a intentarlo.';
            } else {
              reason = `No pudimos cargar el catálogo en línea (HTTP ${error.status}).`;
            }
          } else if (typeof error === 'object' && error !== null && 'errorCode' in error) {
            reason = 'No se pudo autorizar el acceso al catálogo. Cierra sesión e inicia sesión nuevamente con Microsoft.';
          }
          if (!environment.production) {
            // Log only diagnostic codes, never tokens, accounts or response bodies.
            const code = typeof error === 'object' && error !== null && 'errorCode' in error
              && typeof error.errorCode === 'string' ? error.errorCode : 'unknown';
            console.warn('[Catálogo]', error instanceof HttpErrorResponse
              ? { status: error.status, reason: error.statusText }
              : { code });
          }
          this.loadError.set(`${reason} Mostramos productos de demostración.`);
          return of(PRODUCTS);
        }),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((data) => {
        this.productsSignal.set(data);
      });
  }

  list(): Product[] {
    return this.productsSignal();
  }

  byId(id: string): Product | undefined {
    return this.productsSignal().find((product) => product.id === id);
  }
}
