import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { CartService } from '../../cart/services/cart.service';
import { CatalogService } from '../services/catalog.service';

@Component({
  selector: 'app-product-page',
  imports: [RouterLink],
  templateUrl: './product-page.html',
  styleUrl: './product-page.css',
})
export class ProductPage {
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly route = inject(ActivatedRoute);

  private readonly productId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('id') ?? '')),
    { initialValue: this.route.snapshot.paramMap.get('id') ?? '' },
  );

  readonly product = computed(() => this.catalog.byId(this.productId()));

  priceLabel(price: number): string {
    return `$${price.toFixed(2)}`;
  }

  addToCart(): void {
    const product = this.product();
    if (product) {
      this.cart.add(product.id);
    }
  }
}
