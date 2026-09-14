import { NotificationsService } from '../../notifications/services/notifications.service';
import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CatalogService } from '../../catalog/services/catalog.service';
import { CartService } from '../services/cart.service';
import { TransactionsService } from '../../transactions/services/transactions.service';

@Component({
  selector: 'app-cart-page',
  imports: [RouterLink],
  templateUrl: './cart-page.html',
  styleUrl: './cart-page.css',
})
export class CartPage {
  private readonly notifications = inject(NotificationsService);
  private readonly transactions = inject(TransactionsService);
  private readonly router = inject(Router);
  simulatePurchase(): void {
    if (!this.lines().length) return;
    this.transactions.create(this.lines().map(line => ({ productoId: line.product.id,
      productoNombre: line.product.name, cantidad: line.quantity, precioUnitario: line.product.price,
      talla: line.size, nombreEstampado: line.playerName, numeroEstampado: line.dorsal })), () => {
        this.cart.clear();
        this.notifications.notify('compra', 'Tu solicitud de compra fue registrada correctamente.');
        this.router.navigateByUrl('/transacciones');
      });
  }
  readonly cart = inject(CartService);
  private readonly catalog = inject(CatalogService);

  readonly lines = computed(() =>
    this.cart.items()
      .map((line) => {
        const product = this.catalog.byId(line.productId);
        if (!product) return null;
        return { ...line, product, subtotal: product.price * line.quantity };
      })
      .filter((line) => line !== null),
  );

  readonly total = computed(() => this.lines().reduce((sum, line) => sum + line.subtotal, 0));
  readonly count = this.cart.count;
  readonly catalogLoading = this.catalog.loading;
  readonly missingCount = computed(() => this.cart.items().length - this.lines().length);
  readonly purchaseSaving = this.transactions.saving;
  readonly purchaseError = this.transactions.error;

  priceLabel(price: number): string {
    return `$${price.toFixed(2)}`;
  }

  increase(productId: string, size?: string, playerName?: string, dorsal?: number): void {
    const line = this.cart.items().find(
      (l) =>
        l.productId === productId &&
        l.size === size &&
        l.playerName === playerName &&
        l.dorsal === dorsal,
    );
    if (line) {
      this.cart.updateQuantity(productId, line.quantity + 1, size, playerName, dorsal);
    }
  }

  decrease(productId: string, size?: string, playerName?: string, dorsal?: number): void {
    const line = this.cart.items().find(
      (l) =>
        l.productId === productId &&
        l.size === size &&
        l.playerName === playerName &&
        l.dorsal === dorsal,
    );
    if (line) {
      this.cart.updateQuantity(productId, line.quantity - 1, size, playerName, dorsal);
    }
  }

  remove(productId: string, size?: string, playerName?: string, dorsal?: number): void {
    this.cart.remove(productId, size, playerName, dorsal);
  }

  clear(): void {
    this.cart.clear();
  }
}
