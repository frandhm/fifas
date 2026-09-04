import { computed, Injectable, signal } from '@angular/core';

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
  }

  remove(productId: string, size?: string, playerName?: string, dorsal?: number): void {
    this.lines.update((current) =>
      current.filter((l) => !lineMatches(l, productId, size, playerName, dorsal)),
    );
    this.persist();
  }

  updateQuantity(
    productId: string,
    quantity: number,
    size?: string,
    playerName?: string,
    dorsal?: number,
  ): void {
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
  }

  clear(): void {
    this.lines.set([]);
    this.persist();
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
