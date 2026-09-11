import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, of } from 'rxjs';
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

  readonly loadError = signal<string | null>(null);

  constructor() {
    this.loadFromBackend();
  }

  loadFromBackend(): void {
    this.loadError.set(null);
    this.http.get<Product[]>(`${environment.apiBaseUrl}/api/productos`)
      .pipe(
        catchError(() => {
          this.loadError.set('No pudimos cargar el catálogo en línea. Mostramos productos de demostración.');
          return of(PRODUCTS);
        })
      )
      .subscribe((data) => {
        if (data && data.length > 0) {
          this.productsSignal.set(data);
        }
      });
  }

  list(): Product[] {
    return this.productsSignal();
  }

  byId(id: string): Product | undefined {
    return this.productsSignal().find((product) => product.id === id);
  }
}
