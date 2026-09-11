import { Component, computed, inject, signal } from '@angular/core';
import { ProductCategory } from '../../../core/models/product';
import { CartService } from '../../cart/services/cart.service';
import { ProductCard } from '../../../share/ui/product-card/product-card';
import { CatalogFilters } from '../components/catalog-filters';
import { CatalogService } from '../services/catalog.service';

@Component({
  selector: 'app-catalog-page',
  imports: [CatalogFilters, ProductCard],
  templateUrl: './catalog-page.html',
  styleUrl: './catalog-page.css',
})
export class CatalogPage {
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);

  readonly category = signal<ProductCategory | 'all'>('all');
  readonly products = computed(() => {
    const selected = this.category();
    const all = this.catalog.list();
    return selected === 'all' ? all : all.filter((product) => product.category === selected);
  });

  setCategory(category: ProductCategory | 'all'): void {
    this.category.set(category);
  }

  addToCart(productId: string): void {
    this.cart.add(productId);
  }
}
