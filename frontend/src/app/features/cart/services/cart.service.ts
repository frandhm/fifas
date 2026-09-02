import { computed, Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'fifas.cart';

export interface CartLine {
  productId: string;
  quantity: number;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly lines = signal<CartLine[]>(this.readStored());

  readonly items = this.lines.asReadonly();
  readonly count = computed(() => this.lines().reduce((sum, line) => sum + line.quantity, 0));

  add(productId: string): void {
    this.lines.update((current) => {
      const existing = current.find((line) => line.productId === productId);
      if (!existing) {
        return [...current, { productId, quantity: 1 }];
      }
      return current.map((line) =>
        line.productId === productId ? { ...line, quantity: line.quantity + 1 } : line,
      );
    });
    this.persist();
  }

  remove(productId: string): void {
    this.lines.update((current) => current.filter((line) => line.productId !== productId));
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
