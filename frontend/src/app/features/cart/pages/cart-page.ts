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

  priceLabel(price: number): string {
    return `$${price.toFixed(2)}`;
  }

  remove(productId: string): void {
    this.cart.remove(productId);
  }
}
