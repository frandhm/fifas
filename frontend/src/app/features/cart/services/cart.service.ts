import { computed, Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'fifas.cart';

export interface CartLine {
  productId: string;
  quantity: number;
  size?: string;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly lines = signal<CartLine[]>(this.readStored());

  readonly items = this.lines.asReadonly();
  readonly count = computed(() => this.lines().reduce((sum, line) => sum + line.quantity, 0));

  add(productId: string, size?: string): void {
    this.lines.update((current) => {
      const existing = current.find(
        (line) => line.productId === productId && line.size === size,
      );
      if (!existing) {
        return [...current, { productId, quantity: 1, size }];
      }
      return current.map((line) =>
        line.productId === productId && line.size === size
          ? { ...line, quantity: line.quantity + 1 }
          : line,
      );
    });
    this.persist();
  }

  remove(productId: string, size?: string): void {
    this.lines.update((current) =>
      current.filter((line) => !(line.productId === productId && line.size === size)),
    );
    this.persist();
  }

  updateQuantity(productId: string, quantity: number, size?: string): void {
    if (quantity <= 0) {
      this.remove(productId, size);
      return;
    }
    this.lines.update((current) =>
      current.map((line) =>
        line.productId === productId && line.size === size ? { ...line, quantity } : line,
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
