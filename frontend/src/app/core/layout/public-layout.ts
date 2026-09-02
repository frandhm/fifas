import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink],
  template: `
    <div class="min-h-dvh bg-zinc-50 text-zinc-900">
      <header class="flex items-center justify-between border-b bg-white px-6 py-3">
        <a routerLink="/" class="flex items-center gap-2">
          <img src="/images/logo.png" alt="Los FIFAS" class="h-10 w-10 object-contain" />
        </a>
        <nav class="flex gap-4 text-sm font-semibold">
          <a routerLink="/">Home</a>
          <a routerLink="/catalogo">Catálogo</a>
          <a routerLink="/carrito">Carrito</a>
          <a routerLink="/login">Login</a>
        </nav>
      </header>
      <router-outlet />
    </div>
  `,
})
export class PublicLayout {}
