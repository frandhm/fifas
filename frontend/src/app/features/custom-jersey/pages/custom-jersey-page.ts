import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { CartService } from '../../cart/services/cart.service';
import { CatalogService } from '../../catalog/services/catalog.service';

@Component({
  selector: 'app-custom-jersey-page',
  imports: [RouterLink],
  templateUrl: './custom-jersey-page.html',
  styleUrl: './custom-jersey-page.css',
})
export class CustomJerseyPage implements OnInit {
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly productId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('id') ?? '')),
    { initialValue: this.route.snapshot.paramMap.get('id') ?? '' },
  );

  readonly product = computed(() => {
    const id = this.productId();
    if (id) {
      return this.catalog.byId(id);
    }
    // Si no hay ID en la URL (ej. navegar directamente a /personalizar), toma la primera camiseta por defecto
    const all = this.catalog.list();
    return all.length > 0 ? all[0] : undefined;
  });

  readonly playerName = signal('');
  readonly dorsal = signal(10);

  readonly playerNameDisplay = computed(() =>
    this.playerName().toUpperCase() || 'TU NOMBRE',
  );
  readonly nameLength = computed(() => this.playerName().length);

  /** Posiciones X de las rayas verticales del SVG (ancho total 200, paso 26px, raya 14px) */
  readonly stripeOffsets = Array.from({ length: 9 }, (_, i) => i * 26 - 4);

  ngOnInit(): void {}

  onNameInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.playerName.set(val.slice(0, 12));
    // Sincroniza el valor del input por si Angular no lo actualiza solo
    (event.target as HTMLInputElement).value = val.slice(0, 12);
  }

  onDorsalInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    const num = parseInt(val, 10);
    if (!isNaN(num)) {
      this.dorsal.set(Math.max(0, Math.min(99, num)));
    }
  }

  increaseDorsal(): void {
    const v = this.dorsal();
    if (v < 99) this.dorsal.set(v + 1);
  }

  decreaseDorsal(): void {
    const v = this.dorsal();
    if (v > 0) this.dorsal.set(v - 1);
  }

  priceLabel(price: number): string {
    return `$${price.toFixed(2)}`;
  }

  addToCart(): void {
    const product = this.product();
    if (!product) return;
    this.cart.add(
      product.id,
      undefined,
      this.playerName() || undefined,
      this.playerName() ? this.dorsal() : undefined,
    );
    this.router.navigate(['/carrito']);
  }
}
