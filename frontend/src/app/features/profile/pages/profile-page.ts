import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../auth/services/auth.service';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css',
})
export class ProfilePage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

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

    this.savedNotice.set(true);
    setTimeout(() => {
      this.savedNotice.set(false);
    }, 3500);
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
