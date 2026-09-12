import { ProfileSidebar } from '../../../share/ui/profile-sidebar/profile-sidebar';
import { NotificationsService } from '../../notifications/services/notifications.service';
import { Component, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../auth/services/auth.service';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [ProfileSidebar],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css',
})
export class ProfilePage {
  private readonly notifications = inject(NotificationsService);
  private readonly authService = inject(AuthService);

  // Form signals initialized from AuthService
  readonly user = this.authService.currentUser;

  readonly firstName = signal(this.user()?.firstName || 'Ejemplo');
  readonly lastName = signal(this.user()?.lastName || 'Ejemplo');
  readonly email = signal(this.user()?.email || 'usuario@fifas.com');
  readonly phone = signal(this.user()?.phone || '+56 9 1234 5678');

  readonly savedNotice = signal(false);

  readonly userInitials = computed(() => {
    const f = this.firstName().trim().charAt(0) || 'U';
    const l = this.lastName().trim().charAt(0) || '';
    return `${f}${l}`.toUpperCase();
  });

  onSave(event: Event): void {
    event.preventDefault();
    this.authService.updateProfile({
      firstName: this.firstName(),
      lastName: this.lastName(),
      email: this.email(),
      phone: this.phone(),
    });

    this.notifications.notify('perfil', 'Guardaste los cambios de tu perfil en este dispositivo.');
    this.savedNotice.set(true);
    setTimeout(() => {
      this.savedNotice.set(false);
    }, 3500);
  }

  onLogout(): void {
    this.authService.logout();
  }
}
