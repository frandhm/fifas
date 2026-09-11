import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../auth/services/auth.service';
import { TransactionsService } from '../services/transactions.service';

@Component({
  selector: 'app-transactions-page',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './transactions-page.html',
  styleUrl: './transactions-page.css',
})
export class TransactionsPage {
  private readonly authService = inject(AuthService);
  private readonly transactionsService = inject(TransactionsService);
  private readonly router = inject(Router);

  readonly user = this.authService.currentUser;
  readonly transactions = this.transactionsService.userTransactions;

  readonly userInitials = computed(() => {
    const f = (this.user()?.firstName || 'F').trim().charAt(0);
    const l = (this.user()?.lastName || 'G').trim().charAt(0);
    return `${f}${l}`.toUpperCase();
  });

  readonly totalSpent = computed(() => {
    return this.transactions().reduce((acc, curr) => acc + curr.total, 0);
  });

  getStatusClass(status: string): string {
    switch (status) {
      case 'Entregado':
        return 'status-delivered';
      case 'En camino':
        return 'status-shipping';
      case 'Procesando':
        return 'status-processing';
      case 'Cancelado':
        return 'status-canceled';
      default:
        return '';
    }
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
