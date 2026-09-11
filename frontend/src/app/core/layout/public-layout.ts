import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { CartService } from '../../features/cart/services/cart.service';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.css',
})
export class PublicLayout {
  private readonly msal = inject(MsalService);
  private readonly router = inject(Router);
  readonly cartCount = inject(CartService).count;

  goToProfile(): void {
    const isLoggedIn = this.msal.instance.getAllAccounts().length > 0;
    if (isLoggedIn) {
      this.router.navigateByUrl('/perfil');
    } else {
      this.msal.loginRedirect();
    }
  }
}