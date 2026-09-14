import { ProfileSidebar } from '../../../share/ui/profile-sidebar/profile-sidebar';
import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NotificationsService, NotificationType } from '../services/notifications.service';
@Component({
  selector: 'app-notifications-page', imports: [ProfileSidebar, DatePipe, RouterLink],
  templateUrl: './notifications-page.html', styleUrl: './notifications-page.css',
})
export class NotificationsPage {
  readonly notifications = inject(NotificationsService);
  readonly filter = signal<'all' | 'unread'>('all');
  readonly visible = computed(() => this.notifications.items().filter(n => this.filter() === 'all' || !n.leido));
  readonly labels: Record<NotificationType, string> = { sesion: 'Tu cuenta', carrito: 'Tu carrito', perfil: 'Tu perfil', compra: 'Compra de demostración' };
  constructor() { this.notifications.refresh(); }
}
