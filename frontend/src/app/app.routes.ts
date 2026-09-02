import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/pages/login-page').then((m) => m.LoginPage),
  },
  {
    path: '',
    loadComponent: () => import('./core/layout/public-layout').then((m) => m.PublicLayout),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/home/pages/home-page').then((m) => m.HomePage),
      },
      {
        path: 'catalogo',
        loadComponent: () =>
          import('./features/catalog/pages/catalog-page').then((m) => m.CatalogPage),
      },
      {
        path: 'catalogo/:id',
        loadComponent: () =>
          import('./features/catalog/pages/product-page').then((m) => m.ProductPage),
      },
      {
        path: 'personalizar',
        loadComponent: () =>
          import('./features/custom-jersey/pages/custom-jersey-page').then(
            (m) => m.CustomJerseyPage,
          ),
      },
      {
        path: 'carrito',
        loadComponent: () => import('./features/cart/pages/cart-page').then((m) => m.CartPage),
      },
      {
        path: 'perfil',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/profile/pages/profile-page').then((m) => m.ProfilePage),
      },
      {
        path: 'transacciones',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/transactions/pages/transactions-page').then((m) => m.TransactionsPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
