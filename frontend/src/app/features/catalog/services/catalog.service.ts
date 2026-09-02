import { Injectable } from '@angular/core';
import { Product } from '../../../core/models/product';

const PRODUCTS: Product[] = [
  {
    id: 'local-2026',
    name: 'Camiseta local Los FIFAS 2026',
    price: 200,
    accent: '#1e4fa3',
    stripe: '#f4f7fb',
    category: 'local',
    description: 'Rayas azul y blanco. La camiseta de casa para la temporada 2026.',
  },
  {
    id: 'visitante-2026',
    name: 'Camiseta visitante 2026',
    price: 200,
    accent: '#0b1730',
    stripe: '#d4af37',
    category: 'visitante',
    description: 'Navy con detalles dorados para los partidos de visitante.',
  },
  {
    id: 'tercera-roja',
    name: 'Tercera equipación roja',
    price: 190,
    accent: '#8b1e2d',
    stripe: '#f1d5c0',
    category: 'tercera',
    description: 'Tercera equipación en rojo, corte clásico de competición.',
  },
  {
    id: 'portero',
    name: 'Camiseta de portero',
    price: 180,
    accent: '#15803d',
    stripe: '#0b1730',
    category: 'portero',
    description: 'Verde y navy, pensada para el arco.',
  },
  {
    id: 'retro',
    name: 'Edición retro 98',
    price: 220,
    accent: '#163a7a',
    stripe: '#e8c36a',
    category: 'retro',
    description: 'Homenaje a la camiseta del 98, edición limitada.',
  },
  {
    id: 'local-nino',
    name: 'Camiseta local niño',
    price: 140,
    accent: '#1e4fa3',
    stripe: '#f4f7fb',
    category: 'local',
    description: 'Misma local 2026, talla infantil.',
  },
];

@Injectable({ providedIn: 'root' })
export class CatalogService {
  list(): Product[] {
    return PRODUCTS;
  }

  byId(id: string): Product | undefined {
    return PRODUCTS.find((product) => product.id === id);
  }
}
