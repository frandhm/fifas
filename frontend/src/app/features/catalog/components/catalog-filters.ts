import { Component, input, output } from '@angular/core';
import { ProductCategory } from '../../../core/models/product';

@Component({
  selector: 'app-catalog-filters',
  templateUrl: './catalog-filters.html',
  styleUrl: './catalog-filters.css',
})
export class CatalogFilters {
  readonly active = input<ProductCategory | 'all'>('all');
  readonly changeFilter = output<ProductCategory | 'all'>();

  readonly options: { id: ProductCategory | 'all'; label: string }[] = [
    { id: 'all', label: 'Todas' },
    { id: 'local', label: 'Local' },
    { id: 'visitante', label: 'Visitante' },
    { id: 'tercera', label: 'Tercera' },
    { id: 'portero', label: 'Portero' },
    { id: 'retro', label: 'Retro' },
  ];
}
