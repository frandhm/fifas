import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '../../cart/services/cart.service';
import { CatalogService } from '../../catalog/services/catalog.service';

@Component({
  selector: 'app-home-page',
  imports: [RouterLink],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
})
export class HomePage {
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);

  readonly products = this.catalog.list();
  readonly index = signal(0);

  readonly current = computed(() => this.products[this.index()]);
  readonly prevProduct = computed(
    () => this.products[(this.index() - 1 + this.products.length) % this.products.length],
  );
  readonly nextProduct = computed(
    () => this.products[(this.index() + 1) % this.products.length],
  );

  priceLabel(price: number): string {
    return `$${price.toFixed(2)}`;
  }

  prev(): void {
    this.index.update((value) => (value - 1 + this.products.length) % this.products.length);
  }

  next(): void {
    this.index.update((value) => (value + 1) % this.products.length);
  }

  goTo(i: number): void {
    this.index.set(i);
  }

  addToCart(): void {
    this.cart.add(this.current().id);
  }
}
