import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../../catalog/services/catalog.service';
import { CartService } from '../services/cart.service';

@Component({
  selector: 'app-cart-page',
  imports: [RouterLink],
  templateUrl: './cart-page.html',
  styleUrl: './cart-page.css',
})
export class CartPage {
  private readonly cart = inject(CartService);
  private readonly catalog = inject(CatalogService);

  readonly lines = computed(() =>
    this.cart.items()
      .map((line) => {
        const product = this.catalog.byId(line.productId);
        if (!product) {
          return null;
        }
        return { ...line, product, subtotal: product.price * line.quantity };
      })
      .filter((line) => line !== null),
  );

  readonly total = computed(() => this.lines().reduce((sum, line) => sum + line.subtotal, 0));
  readonly count = this.cart.count;

  priceLabel(price: number): string {
    return `$${price.toFixed(2)}`;
  }

  increase(productId: string, size?: string): void {
    const line = this.cart.items().find(
      (l) => l.productId === productId && l.size === size,
    );
    if (line) {
      this.cart.updateQuantity(productId, line.quantity + 1, size);
    }
  }

  decrease(productId: string, size?: string): void {
    const line = this.cart.items().find(
      (l) => l.productId === productId && l.size === size,
    );
    if (line) {
      this.cart.updateQuantity(productId, line.quantity - 1, size);
    }
  }

  remove(productId: string, size?: string): void {
    this.cart.remove(productId, size);
  }

  clear(): void {
    this.cart.clear();
  }
}
