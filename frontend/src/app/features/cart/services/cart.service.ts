import { NotificationsService } from '../../notifications/services/notifications.service';
import { computed, inject, Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'fifas.cart';

export interface CartLine {
  productId: string;
  quantity: number;
  size?: string;
  playerName?: string;
  dorsal?: number;
}

function lineMatches(
  line: CartLine,
  productId: string,
  size?: string,
  playerName?: string,
  dorsal?: number,
): boolean {
  return (
    line.productId === productId &&
    line.size === size &&
    line.playerName === playerName &&
    line.dorsal === dorsal
  );
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly notifications = inject(NotificationsService);
  private readonly lines = signal<CartLine[]>(this.readStored());

  readonly items = this.lines.asReadonly();
  readonly count = computed(() => this.lines().reduce((sum, line) => sum + line.quantity, 0));

  add(productId: string, size?: string, playerName?: string, dorsal?: number): void {
    this.lines.update((current) => {
      const existing = current.find((l) => lineMatches(l, productId, size, playerName, dorsal));
      if (!existing) {
        return [...current, { productId, quantity: 1, size, playerName, dorsal }];
      }
      return current.map((l) =>
        lineMatches(l, productId, size, playerName, dorsal)
          ? { ...l, quantity: l.quantity + 1 }
          : l,
      );
    });
    this.persist();
    this.notifications.notify('carrito', playerName ? 'Agregaste una camiseta personalizada al carrito.' : 'Agregaste un producto al carrito.');
  }

  remove(productId: string, size?: string, playerName?: string, dorsal?: number): void {
    if (!this.lines().some(l => lineMatches(l, productId, size, playerName, dorsal))) return;
    this.lines.update((current) =>
      current.filter((l) => !lineMatches(l, productId, size, playerName, dorsal)),
    );
    this.persist();
    this.notifications.notify('carrito', 'Quitaste un producto del carrito.');
  }

  updateQuantity(
    productId: string,
    quantity: number,
    size?: string,
    playerName?: string,
    dorsal?: number,
  ): void {
    if (!this.lines().some(l => lineMatches(l, productId, size, playerName, dorsal) && l.quantity !== quantity)) return;
    if (quantity <= 0) {
      this.remove(productId, size, playerName, dorsal);
      return;
    }
    this.lines.update((current) =>
      current.map((l) =>
        lineMatches(l, productId, size, playerName, dorsal) ? { ...l, quantity } : l,
      ),
    );
    this.persist();
    this.notifications.notify('carrito', 'Actualizaste la cantidad de un producto en el carrito.');
  }

  clear(): void {
    if (!this.lines().length) return;
    this.lines.set([]);
    this.persist();
    this.notifications.notify('carrito', 'Vaciaste tu carrito.');
  }

  private persist(): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this.lines()));
  }

  private readStored(): CartLine[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as CartLine[]) : [];
    } catch {
      return [];
    }
  }
}
